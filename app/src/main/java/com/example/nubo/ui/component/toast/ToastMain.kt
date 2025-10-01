@file:Suppress("FunctionName", "unused")

package com.example.components.toast

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey700
import com.example.nubo.ui.theme.PurpleMain500
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.example.nubo.R

// ──────────────────────────────────────────────────────────────
// 3가지 레이아웃 유형
//  - TitleOnly: 제목만 표시
//  - TitleWithSummary: 제목 + 한 줄 요약
//  - TitleWithBody: 제목 + 여러 줄 본문
// ──────────────────────────────────────────────────────────────
enum class AppToastLayout { TitleOnly, TitleWithSummary, TitleWithBody }

// ──────────────────────────────────────────────────────────────
// 토스트 타입  색상 테마 구분용
// ──────────────────────────────────────────────────────────────
enum class AppToastType { NORMAL, POSITIVE, NEGATIVE }

// ──────────────────────────────────────────────────────────────
// 토스트 데이터 모델
//  - title: AnnotatedString으로 받아 부분 색상 강조 가능
//  - summary/body는 선택
//  - iconRes: 좌측 아이콘 옵션  SVG 벡터 드로어블로 추가 가능
//  - iconTint: null이면 원본색 유지  값이 있으면 단색 틴트
// ──────────────────────────────────────────────────────────────
data class AppToastData(
    val id: Long,
    val type: AppToastType,
    val layout: AppToastLayout,
    val title: AnnotatedString,
    val summary: String? = null,
    val body: String? = null,
    @DrawableRes val iconRes: Int? = null,
    val iconTint: Color? = null,
    val durationMillis: Int = 2000
)

// ──────────────────────────────────────────────────────────────
// 스타일 모델  색상·모서리·그림자 등
// ──────────────────────────────────────────────────────────────
data class AppToastStyle(
    val bg: Color,
    val titleColor: Color,
    val textColor: Color,
    val shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    @DrawableRes val backgroundRes: Int? = null,     // ← 추가
    val scrim: Color = Color.Transparent // 텍스트 가독성용 (0 = 미적용)
)

@Composable
fun defaultToastStyleProvider(): (AppToastType) -> AppToastStyle = { t ->
    when (t) {
        AppToastType.NORMAL -> AppToastStyle(
            bg = Color.White,
            titleColor = Color.Black,
            textColor = Grey700,
            backgroundRes = R.drawable.toast_bg
        )
        AppToastType.POSITIVE -> AppToastStyle(
            bg = Color.White,
            titleColor = Color.Black,
            textColor = Grey700,
            backgroundRes = R.drawable.toast_bg
        )
        AppToastType.NEGATIVE -> AppToastStyle(
            bg = Color.White,
            titleColor = Color.Black,
            textColor = Grey700,
            backgroundRes = R.drawable.toast_bg
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 호스트 상태  show 호출을 큐처럼 순차 처리
// ──────────────────────────────────────────────────────────────
@Stable
class AppToastHostState {
    private val mutex = Mutex()
    var current by mutableStateOf<AppToastData?>(null)
        private set

    /** 토스트를 순차 처리 + 자동 사라짐 */
    suspend fun show(data: AppToastData) {
        mutex.withLock {
            current = data
            try {
                // 표시 유지
                delay(data.durationMillis.coerceAtLeast(800).toLong())
            } finally {
                // 아직 내가 올린 토스트면 닫기
                if (current?.id == data.id) current = null
            }
            // exit 애니메이션 시간만큼 대기 후 다음 토스트 허용
            delay(220)
        }
    }

    // 편의 오버로드  제목과 레이아웃만으로 호출 가능
    suspend fun show(
        title: AnnotatedString,
        layout: AppToastLayout = AppToastLayout.TitleOnly,
        type: AppToastType = AppToastType.NORMAL,
        summary: String? = null,
        body: String? = null,
        @DrawableRes iconRes: Int? = null,
        iconTint: Color? = null,
        durationMillis: Int = 2000
    ) = show(
        AppToastData(
            id = System.currentTimeMillis(),
            type = type,
            layout = layout,
            title = title,
            summary = summary,
            body = body,
            iconRes = iconRes,
            iconTint = iconTint,
            durationMillis = durationMillis
        )
    )

    fun dismiss() { current = null }
}

@Composable
fun rememberAppToastHostState(): AppToastHostState = remember { AppToastHostState() }

// ──────────────────────────────────────────────────────────────
// 호스트 UI  3가지 레이아웃을 조건에 맞게 렌더링
//  내용 텍스트는 요청대로 프리텐다드 레귤러 14pt 기준 14sp + lineHeight 20sp
//  제목은 Bold 16~18sp 권장  기본값 16sp
// ──────────────────────────────────────────────────────────────
@Composable
fun AppToastHost(
    hostState: AppToastHostState,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.BottomCenter,
    styleProvider: (AppToastType) -> AppToastStyle = defaultToastStyleProvider(),
    matchParentSize: Boolean = true,
) {
    val data = hostState.current
    val rootModifier =
        if (matchParentSize) modifier.fillMaxSize() else modifier


    Box(
        modifier = rootModifier.semantics(mergeDescendants = true) {},
        contentAlignment = contentAlignment
    ) {
        AnimatedVisibility(
            visible = data != null,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(durationMillis = 240, easing = FastOutLinearInEasing)
            ) + fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            data?.let { t ->
                val toastStyle = styleProvider(t.type)

                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Surface(
                        onClick = { hostState.dismiss() },
                        color = Color.Transparent,                 // ← 투명
                        shape = toastStyle.shape,
                        tonalElevation = 0.dp,
                        modifier = Modifier.shadow(elevation = 8.dp, shape = toastStyle.shape)
                    ) {
                        // 1) 배경: PNG 있으면 이미지, 없으면 단색
                        if (toastStyle.backgroundRes != null) {
                            Image(
                                painter = painterResource(toastStyle.backgroundRes),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Crop
                            )
                            // 2) 스크림(가독성): alpha 0으로 두면 사실상 미적용
                            if (toastStyle.scrim.alpha > 0f) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(toastStyle.scrim)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(toastStyle.bg)
                            )
                        }


                        Row(
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                                .widthIn(min = 200.dp, max = 360.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // 아이콘 옵션  svg 벡터 드로어블 사용 가능
                            if (t.iconRes != null) {
                                Icon(
                                    painter = painterResource(t.iconRes),
                                    contentDescription = null,
                                    tint = t.iconTint ?: Color.Unspecified, // null이면 원본색 유지
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                            }

                            Column(modifier = Modifier.fillMaxWidth(),               // ← 텍스트 폭 확보
                                horizontalAlignment = Alignment.CenterHorizontally // ← 세로 스택 중앙
                                     ) {
                                Text(
                                    text = t.title,
                                    color = toastStyle.titleColor,
                                    style = AppTextStyles.b2_semibold_16,
                                    maxLines = if (t.layout == AppToastLayout.TitleOnly) 2 else 3,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )

                                when (t.layout) {
                                    AppToastLayout.TitleOnly -> Unit

                                    AppToastLayout.TitleWithSummary -> {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = t.summary.orEmpty(),
                                            color = toastStyle.textColor,
                                            style = AppTextStyles.b3_regular_14,
                                            lineHeight = 20.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    AppToastLayout.TitleWithBody -> {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = t.body.orEmpty(),
                                            color = toastStyle.textColor,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// 도우미  제목의 특정 단어를 색상 강조하는 AnnotatedString 빌더
//  예) 강조 색상은 파란색 계열 기본값
// ──────────────────────────────────────────────────────────────
fun buildHighlightedTitle(
    full: String,
    highlight: String,
    highlightColor: Color = PurpleMain500
): AnnotatedString = buildAnnotatedString {
    val idx = full.indexOf(highlight)
    if (idx < 0) append(full) else {
        append(full.substring(0, idx))
        pushStyle(SpanStyle(color = highlightColor))
        append(highlight)
        pop()
        append(full.substring(idx + highlight.length))
    }
}

// ──────────────────────────────────────────────────────────────
// 예시 화면  세 가지 유형을 각각 호출
// ──────────────────────────────────────────────────────────────
@Composable
fun ToastDemoScreen(
    styleProvider: (AppToastType) -> AppToastStyle = defaultToastStyleProvider()
) {
    val host = rememberAppToastHostState()
    val scope = rememberCoroutineScope()

    Scaffold { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            Column(Modifier.padding(20.dp)) {
                Button(onClick = {
                    scope.launch {
                        host.show(
                            title = buildHighlightedTitle("토스트의 제목이 여기에 작성됩니다.", "작성"),
                            layout = AppToastLayout.TitleOnly,
                            type = AppToastType.NORMAL,
                            // 예시 아이콘 없으면 null
                            iconRes = null
                        )
                    }
                }) { Text("제목만") }

                Spacer(Modifier.height(8.dp))

                Button(onClick = {
                    scope.launch {
                        host.show(
                            title = buildHighlightedTitle("토스트의 제목이 여기에 작성됩니다.", "작성"),
                            layout = AppToastLayout.TitleWithSummary,
                            type = AppToastType.NORMAL,
                            summary = "내용이 프리텐다드 레귤러 14pt 글줄 높이 20",
                            iconRes = null
                        )
                    }
                }) { Text("제목 + 한 줄") }

                Spacer(Modifier.height(8.dp))

                Button(onClick = {
                    scope.launch {
                        host.show(
                            title = buildHighlightedTitle("토스트의 제목이 여기에 작성됩니다.", "작성"),
                            layout = AppToastLayout.TitleWithBody,
                            type = AppToastType.NORMAL,
                            body = "내용이 프리텐다드 레귤러 14pt 글줄 높이 20으로 이곳에 작성됩니다. 기본적으로 토스트는 하단 네비 기준 24 떠 있습니다. 토스트에 대한 내용이 이곳에 작성됩니다.",
                            iconRes = null
                        )
                    }
                }) { Text("제목 + 본문") }
            }
            AppToastHost(hostState = host, styleProvider = styleProvider)
        }
    }
}


@Composable
fun AppToastOverlay(
    hostState: AppToastHostState,
) {
    Popup(
        alignment = Alignment.BottomCenter,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            excludeFromSystemGesture = false
        )
    ) {
        AppToastHost(
            hostState = hostState,
            matchParentSize = false,
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 12.dp)
                .zIndex(1000f)
        )
    }
}




// ──────────────────────────────────────────────────────────────
// 프리뷰  기본 아이콘으로 동작 확인  아이콘이 꼭 필요하면 iconRes에 벡터 리소스 전달
// ──────────────────────────────────────────────────────────────
@Preview(showBackground = true, name = "Toast Demo")
@Composable
private fun PreviewToastDemo() {
    MaterialTheme { ToastDemoScreen() }
}

// ──────────────────────────────────────────────────────────────
// 프리뷰  SVG 아이콘 사용 예시
//  - 프로젝트 drawable에 ic_toast_info.xml 추가 후 import com.example.nubo.R
// ──────────────────────────────────────────────────────────────
@Preview(showBackground = true, name = "Toast Demo with SVG Icon")
@Composable
private fun PreviewToastDemoWithSvgIcon() {
    // import com.example.nubo.R  필요
    MaterialTheme {
        val host = rememberAppToastHostState()
        LaunchedEffect(Unit) {
            host.show(
                title = buildHighlightedTitle("토스트의 제목이 여기에 작성됩니다.", "작성"),
                layout = AppToastLayout.TitleWithSummary,
                summary = "내용이 프리텐다드 레귤러 14pt 글줄 높이 20",
                // 예시로 앱 리소스 아이콘 사용  원본색 유지 위해 iconTint = null
                iconRes = /* R.drawable.ic_toast_info */ null,
                iconTint = null,
                durationMillis = 2200
            )
        }
        Box(Modifier.fillMaxSize()) { AppToastHost(host) }
    }
}
