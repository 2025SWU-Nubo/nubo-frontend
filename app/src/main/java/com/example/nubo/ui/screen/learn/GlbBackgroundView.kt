package com.example.nubo.ui.screen.learn


import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberScene
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberMainLightNode
import kotlin.math.PI
import kotlin.math.sin

/**
 *  백그라운드 스레드의 모든 작업 결과를 담을 데이터 클래스
 */
data class LoadedNodes(
    val rootNode: ModelNode,
    val raindrops: List<Node>,
    val leaves: List<Node>,
    val clouds: List<Node>,
    val flowers: List<Node>,
    val positions: Map<Node, Position>
)

/**
 * URL로부터 .glb 파일을 로드하여 화면 배경에 3D 모델을 렌더링하는 Composable
 * (io.github.sceneview 라이브러리 사용)
 *
 * @param modifier Modifier
 * @param glbUrl 렌더링할 .glb 모델의 URL
 */
@Composable
fun GlbBackgroundView(
    modifier: Modifier = Modifier,
    glbUrl: String,
    onModelLoaded: () -> Unit
) {
    // --- 애니메이션 대상 노드와 상태 저장 ---
    // 1. 애니메이션을 적용할 노드 리스트
    val raindropNodes = remember { mutableStateListOf<Node>() }
    val leafNodes = remember { mutableStateListOf<Node>() }

    // 2. 물방울의 시작 위치를 저장할 맵
    val startPositions = remember { mutableStateMapOf<Node, Position>() }

    // 구름 위치 저장
    val cloudFloatNodes = remember { mutableStateListOf<Node>() }

    // 꽃 저장
    val flowerSwayNodes = remember { mutableStateListOf<Node>() }


    // 로더 생성
    val context = LocalContext.current
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    // 1. Scene '객체'를 Composable 외부에서 먼저 만듦
    val scene = rememberScene(engine)

    // 3. Composable이 관찰할 노드 리스트 (파라미터 이름: childNodes)
    val childNodes = remember { mutableStateListOf<Node>() }
    var isLoading by remember { mutableStateOf(true) } // 로딩 상태

    // 3D 뷰를 위한 카메라 노드를 생성
    val cameraNode = rememberCameraNode(engine)

    // 3D 뷰를 밝혀줄 기본 조명 생성
    val mainLightNode = rememberMainLightNode(engine)

    // 모델을 로드
    LaunchedEffect(glbUrl) {
        isLoading = true
        try {
            //1. IO 스레드에서 모든 무거운 작업을 처리하고, 'LoadedNodes' 결과만 반환
            val loadedData: LoadedNodes = withContext(Dispatchers.IO) {
                Log.d("GlbBackgroundView", "모델 로딩 및 노드 설정 시작 (IO 스레드)")

                // --- 1. 모델 다운로드 및 로딩 ---
                val localFile = downloadGlbToCache(context, glbUrl)
                val fileUri = localFile.toUri().toString()
                val modelInstance = modelLoader.loadModelInstance(fileUri)
                    ?: throw IOException("ModelInstance 로딩 실패")

                // --- 2. ModelNode 생성 및 기본 설정 ---
                val node = ModelNode(modelInstance = modelInstance).apply {
                    var z = -1f
                    val e = extents
                    val maxHalf = maxOf(e.x, e.y, e.z)
                    val full = maxHalf * 1f
                    val target = 6f
                    val factor = if (full > 0f) target / full else 1f
                    scale = Scale(factor)
                    position = Position(0f, -0.45f, z)
                }

                // [수정] 3. Compose 상태가 아닌 '임시' 리스트에 노드를 담음
                val tempRaindrops = mutableListOf<Node>()
                val tempLeaves = mutableListOf<Node>()
                val tempClouds = mutableListOf<Node>()
                val tempFlowers = mutableListOf<Node>()
                val tempPositions = mutableMapOf<Node, Position>()

                // --- 노드 검색, 크기 조절, 리스트에 추가 ---

                /*// --- 1. 라이트 노드 검색 및 배치 ---
                val lightX = -4f // 좌측
                val lightY = 6.0f  // 상단
                val lightZ = -3f  // 장면 앞쪽

                //주광
                node.nodes["Area Light_Main"]?.let { light ->
                    light.position = Position(x = lightX, y = lightY, z = lightZ)
                }
                //보조광
                node.nodes["Point Light_Sub"]?.let { light ->
                    // Area Light와 살짝 다른 위치
                    light.position = Position(x = lightX + 0.2f, y = lightY - 0.7f, z = lightZ)
                }

                // 물방울 하이라이트용 조명
                node.nodes["Raindrop Point_01"]?.let { light ->
                    light.position = Position(x = lightX + 1.0f, y = lightY - 3.0f, z = lightZ)
                }

                // 4. [추가] 언덕 강조용 조명
                node.nodes["Hill Point"]?.let { light ->
                    light.position = Position(x = lightX + 1.5f, y = lightY - 3.0f, z = lightZ)
                }*/

                // 2. 이름으로 노드 찾기 (크기 조절)
                val cloudNode = node.nodes["Cloud_Main"]
                val cloudNodeEye = node.nodes["Face_Eye"]
                val cloudNodeM = node.nodes["Face_Mouth"]

                val cloudYOffset = -2.7f
                val cloudXOffset = 0.05f

                // 1. 크기 및 위치 조절 (구름)
                cloudNode?.let {
                    it.scale = Scale(1.3f)
                    it.position = it.position + Position(y = cloudYOffset)
                    tempClouds.add(it) // [수정] 임시 리스트에 추가
                    tempPositions[it] = it.position
                }
                cloudNodeEye?.let {
                    it.scale = Scale(0.8f)
                    it.position = it.position + Position(y = cloudYOffset)
                    tempClouds.add(it) // [수정]
                    tempPositions[it] = it.position
                }
                cloudNodeM?.let {
                    it.scale = Scale(0.8f)
                    it.position = it.position + Position(y = cloudYOffset, x = cloudXOffset)
                    tempClouds.add(it) // [수정]
                    tempPositions[it] = it.position
                }

                val rainStartYOffset = 0.8f
                val rainNodeNames = listOf(
                    "Raindrop_01", "Raindrop_02", "Raindrop_03", "Raindrop_04", "Raindrop_05"
                )
                rainNodeNames.forEach { name ->
                    node.nodes[name]?.let { drop ->
                        drop.scale = Scale(0.7f)
                        val newStartPosition =
                            drop.position + Position(y = rainStartYOffset, z = -1f)
                        tempRaindrops.add(drop) // [수정]
                        tempPositions[drop] = newStartPosition
                    }
                }

                val leafNodeNames = listOf("Flower_Leaf_01", "Flower_Leaf_02")
                leafNodeNames.forEach { name ->
                    node.nodes[name]?.let { leaf ->
                        tempLeaves.add(leaf) // [수정]
                        tempPositions[leaf] = leaf.position
                    }
                }

                node.nodes["Flower"]?.let { bell ->
                    tempFlowers.add(bell) // [수정]
                    tempPositions[bell] = bell.position
                }

                // [수정] 4. withContext의 결과로 LoadedNodes 객체를 반환
                LoadedNodes(
                    rootNode = node,
                    raindrops = tempRaindrops,
                    leaves = tempLeaves,
                    clouds = tempClouds,
                    flowers = tempFlowers,
                    positions = tempPositions
                )
            } // [ IO 스레드 종료 ]

            // [수정] 2. 메인 스레드로 돌아온 후, Compose 상태 리스트들을 업데이트
            Log.d("GlbBackgroundView", "백그라운드 작업 완료. Compose 상태 업데이트")
            raindropNodes.clear()
            raindropNodes.addAll(loadedData.raindrops)

            leafNodes.clear()
            leafNodes.addAll(loadedData.leaves)

            cloudFloatNodes.clear()
            cloudFloatNodes.addAll(loadedData.clouds)

            flowerSwayNodes.clear()
            flowerSwayNodes.addAll(loadedData.flowers)

            startPositions.clear()
            startPositions.putAll(loadedData.positions)

            childNodes.clear()
            childNodes.add(loadedData.rootNode)

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GlbBackgroundView", "모델 로딩 실패", e)
            childNodes.clear()
        } finally {
            isLoading = false
            onModelLoaded() // 로딩 완료 콜백 호출
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        //  3D 뷰
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,

            // 생성한 카메라와 조명을 장면에 전달
            cameraNode = cameraNode,
            mainLightNode = mainLightNode,

            // 우리가 만든 'scene' 객체와 'childNodes' 리스트를 전달
            scene = scene,
            childNodes = childNodes,

            cameraManipulator = null,

            // --- onFrame 애니메이션 콜백 수정 ---
            onFrame = { frameTimeNanos ->
                val timeSeconds = frameTimeNanos / 1_000_000_000.0

                // 1. 물방울 애니메이션 (동시 낙하)
                val fallDistance = -9f
                val fallDuration = 3.0
                val waitDuration = 7.0
                val totalCycleDuration = fallDuration + waitDuration
                // val rainStaggerSeconds = 1.2 // <--- [삭제] 시간차 제거

                // 'forEachIndexed' -> 'forEach'
                raindropNodes.forEach { drop ->
                    val startPos = startPositions[drop] ?: return@forEach

                    // 'staggeredTime' -> 'timeSeconds' (시간차 로직 삭제)
                    val timeInCycle = timeSeconds % totalCycleDuration

                    var progress = 0.0
                    if (timeInCycle >= waitDuration) { // 7.0초 ~ 10.0초 사이
                        val timeIntoFall = timeInCycle - waitDuration
                        progress = timeIntoFall / fallDuration
                    }

                    drop.position = startPos + Position(y = progress.toFloat() * fallDistance)
                }

                // 2. 풀잎 - 각도 고정 + 살짝 손 흔들기

                // --- 애니메이션 값 ---
                val leafAngle = 5f // [!!] 흔드는 각도 (아주 살짝)
                val leafCycleDuration = 4.0 // 4초에 1번 왕복 (천천히)
                val leafSpeed = (2 * PI) / leafCycleDuration
                val leafTimeOffset = 2.0 // 잎사귀끼리 시간차
                // ---

                leafNodes.forEachIndexed { index, leaf ->
                    val startPos = startPositions[leaf]

                    // 1. 위치를 원본 위치로 고정 (필수!)
                    if (startPos != null) leaf.position = startPos

                    // 2. sin() 값으로 좌우 흔들림(Z축) 각도 계산
                    val sway =
                        sin(timeSeconds * leafSpeed + (index * leafTimeOffset)).toFloat() * leafAngle

                    // 4. X축(0), Y축(고정), Z축(애니메이션) 적용
                    leaf.rotation = Rotation(x = 50f, y = 0f, z = sway)
                }

                // --- 3. 구름 둥실 애니메이션 ---

                // "아주 조금" (위아래로 움직일 최대 거리)
                val floatAmplitude = 0.07f
                // "5초에 한번" 왕복
                val floatCycleDurationSeconds = 6.0

                val floatSpeed = (2 * PI) / floatCycleDurationSeconds
                val floatY = sin(timeSeconds * floatSpeed).toFloat() * floatAmplitude

                cloudFloatNodes.forEach { node ->
                    val startPos = startPositions[node] ?: return@forEach
                    // Y축으로만 둥실거리도록
                    node.position = startPos + Position(y = floatY)
                }

                // ---  4. 꽃(종) 스윙 애니메이션 ---
                val bellAngle = 5f // 좌우로 흔들릴 각도
                val bellCycleDuration = 8.0 // 6초에 1번 왕복 (살랑살랑)

                val bellSpeed = (2 * PI) / bellCycleDuration
                val bellSway = sin(timeSeconds * bellSpeed).toFloat() * bellAngle

                flowerSwayNodes.forEach { bell ->
                    val startPos = startPositions[bell] ?: return@forEach
                    // Z축을 기준으로 회전 (좌우 스윙)
                    bell.rotation = Rotation(z = bellSway)
                    bell.position = startPos // 위치는 고정
                }

            },
        )
        // 플레이스홀더가 사라진 후, 모델 파싱이 끝나기 전까지
        // 잠깐 노출될 수 있는 내부 로딩 인디케이터
        if (isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}

// ===== 캐시에 GLB를 내려받아 File 로 반환하는 유틸 =====
private suspend fun downloadGlbToCache(
    context: Context,
    url: String,
    fileName: String = "dashboard_bg.glb"
): File = withContext(Dispatchers.IO) {
    val outFile = File(context.cacheDir, fileName)
    if (outFile.exists() && outFile.length() > 0L) return@withContext outFile

    val client = OkHttpClient()
    val request = Request.Builder().url(url).get().build()
    client.newCall(request).execute().use { resp ->
        if (!resp.isSuccessful) throw IOException("GLB 다운로드 실패: HTTP ${resp.code}")
        val body = resp.body ?: throw IOException("GLB 다운로드 실패: body null")
        outFile.outputStream().use { os -> body.byteStream().copyTo(os) }
    }
    return@withContext outFile
}
