package com.example.nubo.ui.screen.learn


import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import io.github.sceneview.Scene
import io.github.sceneview.model.Model
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
    val context = LocalContext.current
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    // 1. Scene '객체'를 Composable 외부에서 먼저 만듦
    val scene = rememberScene(engine)

    // 3. Composable이 관찰할 노드 리스트 (파라미터 이름: childNodes)
    val childNodes = remember { mutableStateListOf<Node>() }
    var isLoading by remember { mutableStateOf(true) }

    // [추가] 3D 뷰를 위한 카메라 노드를 만듭니다.
    val cameraNode = rememberCameraNode(engine)

    // [추가] 3D 뷰를 밝혀줄 기본 조명을 만듭니다.
    val mainLightNode = rememberMainLightNode(engine)

    // 모델을 로드
    LaunchedEffect(glbUrl) {
        isLoading = true
        try {
            val localFile = downloadGlbToCache(context, glbUrl)

            // [수정] localFile.path 대신, 'file://' URI 문자열을 생성합니다.
            val fileUri = localFile.toUri().toString()

            // [수정] modelLoader에 fileUri를 전달합니다.
            val modelInstance = modelLoader.loadModelInstance(fileUri)

            modelInstance?.let { instance -> // 'it'의 이름도 instance로 명확하게 변경
                // 이제 'instance'는 ModelInstance 타입
                val node = ModelNode(modelInstance = instance).apply {
                    // ... (크기/위치 조절 코드) ...
                    var z = -3f
                    val e = extents
                    val maxHalf = maxOf(e.x, e.y, e.z)
                    val full = maxHalf * 2f
                    val target = 4f
                    val factor = if (full > 0f) target / full else 1f
                    scale = io.github.sceneview.math.Scale(factor)
                    val downBy = e.y * factor * 0.27f
                    position = io.github.sceneview.math.Position(0f, -downBy, z)
                }
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

            isOpaque = false,

            // 우리가 만든 'scene' 객체와 'childNodes' 리스트를 전달
            scene = scene,
            childNodes = childNodes,

            cameraManipulator = null
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
