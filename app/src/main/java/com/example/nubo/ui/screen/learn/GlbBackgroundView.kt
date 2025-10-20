package com.example.nubo.ui.screen.learn


import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.net.toUri
import com.example.nubo.R
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
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
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import kotlin.math.PI
import kotlin.math.sin


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
    glbUrl: String
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
            val localFile = downloadGlbToCache(context, glbUrl)

            // localFile.path 대신, 'file://' URI 문자열을 생성
            val fileUri = localFile.toUri().toString()

            // modelLoader에 fileUri를 전달
            val modelInstance = modelLoader.loadModelInstance(fileUri)

            modelInstance?.let { instance -> // 'it'의 이름도 instance로 명확하게 변경
                // 이제 'instance'는 ModelInstance 타입
                val node = ModelNode(modelInstance = instance).apply {
                    // ... (크기/위치 조절 코드) ...
                    var z = -3f
                    val e = extents
                    val maxHalf = maxOf(e.x, e.y, e.z)
                    val full = maxHalf * 1f
                    val target = 2.5f // 전체적인 크기
                    val factor = if (full > 0f) target / full else 1f
                    scale = io.github.sceneview.math.Scale(factor)
                    position = io.github.sceneview.math.Position(0f, -0.5f, z)
                }

                // --- 노드 검색, 크기 조절, 리스트에 추가 ---
                // 1. 기존 애니메이션 노드 리스트 초기화
                raindropNodes.clear()
                leafNodes.clear()
                startPositions.clear()
                cloudFloatNodes.clear()

                // --- 1. 라이트 노드 검색 및 배치 ---
                val lightX = -4.0f // 좌측
                val lightY = 5.0f  // 상단
                val lightZ = -1f  // 장면 앞쪽

                node.nodes["Area Light_Main"]?.let { light ->
                    light.position = Position(x = lightX, y = lightY, z = lightZ)
                }
                node.nodes["Point Light_Sub"]?.let { light ->
                    // Area Light와 살짝 다른 위치
                    light.position = Position(x = lightX + 0.5f, y = lightY - 0.5f, z = lightZ)
                }

                // 2. 이름으로 노드 찾기 (크기 조절)
                val cloudNode = node.nodes["Cloud_Main"]
                val cloudNodeEye = node.nodes["Face_Eye"]
                val cloudNodeM = node.nodes["Face_Mouth"]

                val cloudYOffset = -2.7f
                val cloudXOffset = 0.03f

                // 1. 크기 및 위치 조절 (구름)
                cloudNode?.let {
                    it.scale = Scale(1.1f)
                    it.position = it.position + Position(y = cloudYOffset)
                    cloudFloatNodes.add(it) // <--- [추가] 둥실 리스트에 추가
                    startPositions[it] = it.position // <--- [추가] 둥실 애니메이션 시작 위치 저장
                }
                // (눈)
                cloudNodeEye?.let {
                    it.scale = Scale(1f)
                    it.position = it.position + Position(y = cloudYOffset)
                    cloudFloatNodes.add(it) // <--- [추가]
                    startPositions[it] = it.position // <--- [추가]
                }
                // (입)
                cloudNodeM?.let {
                    it.scale = Scale(1f)
                    it.position = it.position + Position(y = cloudYOffset, x = cloudXOffset)
                    cloudFloatNodes.add(it) // <--- [추가]
                    startPositions[it] = it.position // <--- [추가]
                }

                val rainStartYOffset = 0.8f

                // 3. 물방울 찾기, 크기 조절, 리스트에 저장
                val rainNodeNames = listOf(
                    "Raindrop_01", "Raindrop_02", "Raindrop_03", "Raindrop_04", "Raindrop_05"
                )
                rainNodeNames.forEach { name ->
                    node.nodes[name]?.let { drop ->
                        drop.scale = Scale(0.7f)

                        // Y 오프셋을 더한 위치를 시작점으로 저장
                        val newStartPosition = drop.position + Position(y = rainStartYOffset)

                        raindropNodes.add(drop)
                        startPositions[drop] = newStartPosition // 수정된 위치를 저장
                    }
                }

                // 4. 풀잎 찾기, 리스트에 저장
                val leafNodeNames = listOf("Flower_Leaf_01", "Flower_Leaf_02")
                leafNodeNames.forEach { name ->
                    node.nodes[name]?.let { leaf ->
                        leafNodes.add(leaf)
                        startPositions[leaf] = leaf.position // <--- 풀잎 시작 위치 저장
                    }
                }

                // 5. 꽃 찾기
                node.nodes["Flower"]?.let { bell ->
                    flowerSwayNodes.add(bell)
                    startPositions[bell] = bell.position // 시작 위치 저장
                }
                // --- 노드 검색, 크기 조절, 리스트에 추가 ---

                childNodes.clear()
                childNodes.add(node)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            //오류 발생 시 로그를 남겨서 원인을 확인
            Log.e("GlbBackgroundView", "모델 로딩 실패", e)
            childNodes.clear()
        } finally {
            isLoading = false
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
                val fallDistance = -6f
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

                // 2. 풀잎 애니메이션 (5초 주기, 큰 각도, 다른 타이밍)
                val leafAngle = 20f
                val leafCycleDuration = 5.0
                val leafSpeed = (2 * PI) / leafCycleDuration
                val leafTimeOffset = 2.5

                leafNodes.forEachIndexed { index, leaf ->
                    val startPos = startPositions[leaf]
                    val sway = sin(timeSeconds * leafSpeed + (index * leafTimeOffset)).toFloat() * leafAngle
                    leaf.rotation = Rotation(x = sway)
                    if(startPos != null) leaf.position = startPos
                }

                // --- 3. 구름 둥실 애니메이션 ---

                // "아주 조금" (위아래로 움직일 최대 거리)
                val floatAmplitude = 0.05f
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
                val bellAngle = 6f // 좌우로 흔들릴 각도 (15도)
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

        // 로딩 인디케이터 (맨 앞)
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
