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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.example.nubo.ui.theme.RedError

// ──────────────────────────────────────────────────────────────
// 레이아웃 유형 (제목만 / 요약 포함 / 본문 포함)
// ──────────────────────────────────────────────────────────────
enum class AppToastLayout { TitleOnly, TitleWithSummary, TitleWithBody }

// ──────────────────────────────────────────────────────────────
// 토스트 타입
//  - AI_RESULT: 배경 일러스트 사용 (이미지)
//  - FAVORITE : 흰 배경 + 컴팩트 폭 + 좌측 아이콘 강제 노출
//  - NEGATIVE : 흰 배경 + 좌측 에러 아이콘 강제 노출
//  - NORMAL/POSITIVE: 흰 배경
// ──────────────────────────────────────────────────────────────
enum class AppToastType { NORMAL, POSITIVE, NEGATIVE, FAVORITE, AI_RESULT }

// ──────────────────────────────────────────────────────────────
// 데이터 모델
// ──────────────────────────────────────────────────────────────
data class AppToastData(
    val id: Long,
    val type: AppToastType,
    val layout: AppToastLayout,
    val title: AnnotatedString,
    val summary: String? = null,
    val body: String? = null,
    @DrawableRes val iconRes: Int? = null,  // 호출부에서 직접 아이콘 지정 가능(없어도 됨)
    val iconTint: Color? = null,
    val durationMillis: Int = 2000
)

// ──────────────────────────────────────────────────────────────
// 스타일 모델
//  - backgroundRes: 배경 이미지 (AI_RESULT에서만 사용)
//  - scrim: 배경 이미지 위 가독성 향상용 반투명 오버레이
// ──────────────────────────────────────────────────────────────
data class AppToastStyle(
    val bg: Color,
    val titleColor: Color,
    val textColor: Color,
    val shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    @DrawableRes val backgroundRes: Int? = null,
    val scrim: Color = Color.Transparent
)

// ──────────────────────────────────────────────────────────────
// 타입별 기본 스타일
//  - AI_RESULT만 배경 이미지 사용
//  - 나머지는 흰 배경
// ──────────────────────────────────────────────────────────────
@Composable
fun defaultToastStyleProvider(): (AppToastType) -> AppToastStyle = { t ->
    when (t) {
        AppToastType.AI_RESULT -> AppToastStyle(
            bg = Color.White,                 // 이미지 로딩 실패 시 폴백
            titleColor = Color.Black,
            textColor = Color.White.copy(alpha = 0.55f),
            backgroundRes = R.drawable.toast_bg,
            scrim = Color.Transparent
//            scrim = Color.Black.copy(alpha = 0.25f)
        )
        AppToastType.NORMAL,
        AppToastType.POSITIVE,
        AppToastType.NEGATIVE,
        AppToastType.FAVORITE -> AppToastStyle(
            bg = Color.White,
            titleColor = Color.Black,
            textColor = Grey700,
            backgroundRes = null
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 기본 아이콘(강제 노출용)
//  - FAVORITE: 즐겨찾기 아이콘
//  - NEGATIVE: 에러 아이콘
//  * 프로젝트 리소스에 맞게 교체 가능
// ──────────────────────────────────────────────────────────────
private val DEFAULT_FAVORITE_ICON_RES = R.drawable.ic_board_fillstar
private val DEFAULT_ERROR_ICON_RES = R.drawable.error_toast
private val DEFAULT_FAVORITE_ICON_TINT = PurpleMain500
private val DEFAULT_ERROR_ICON_TINT = RedError// Material Red 계열

// ──────────────────────────────────────────────────────────────
// 호스트 상태 (순차 처리)
// ──────────────────────────────────────────────────────────────
@Stable
class AppToastHostState {
    private val mutex = Mutex()
    var current by mutableStateOf<AppToastData?>(null)
        private set

    suspend fun show(data: AppToastData) {
        mutex.withLock {
            current = data
            try {
                delay(data.durationMillis.coerceAtLeast(800).toLong())
            } finally {
                if (current?.id == data.id) current = null
            }
            // 퇴장 페이드아웃 시간만큼 대기 후 다음 토스트 허용
            delay(220)
        }
    }

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
// 토스트 호스트 UI
//  - AI_RESULT: 배경 이미지(풀블리드) + 스크림
//  - 그 외: Surface 색상(bg) 사용 → 콘텐츠 크기 기준(wrap)으로 렌더
//  - FAVORITE: 컴팩트 폭/패딩 + 좌측 아이콘 강제
//  - NEGATIVE: 좌측 에러 아이콘 강제
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
    val rootModifier = if (matchParentSize) modifier.fillMaxSize() else modifier

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
            exit = slideOutVertically(
                targetOffsetY = { it / 4 },
                animationSpec = tween(
                    durationMillis = 220,
                    easing = LinearOutSlowInEasing
                )
            ) + fadeOut(animationSpec = tween(200))
        ) {
            data?.let { t ->
                val toastStyle = styleProvider(t.type)

                // 타입별 아이콘 강제 주입 로직
                val isFavorite = t.type == AppToastType.FAVORITE
                val isNegative = t.type == AppToastType.NEGATIVE
                val effectiveIconRes: Int? = when {
                    t.iconRes != null -> t.iconRes
                    isFavorite -> DEFAULT_FAVORITE_ICON_RES
                    isNegative -> DEFAULT_ERROR_ICON_RES
                    else -> null
                }
                val effectiveIconTint: Color? = t.iconTint ?: when {
                    isFavorite -> DEFAULT_FAVORITE_ICON_TINT
                    isNegative -> DEFAULT_ERROR_ICON_TINT
                    else -> null
                }

                // FAVORITE만 컴팩트 사이징
                val isCompact = isFavorite
                val contentPadding = if (isCompact)
                    PaddingValues(horizontal = 30.dp, vertical = 16.dp)
                else
                    PaddingValues(horizontal = 30.dp, vertical = 16.dp)
                val widthMod = if (isCompact)
                    Modifier.widthIn(max = 320.dp)       // 텍스트 짧을 때 과도한 가로폭 방지
                else
                    Modifier.widthIn(min = 200.dp, max = 360.dp)

                Box(
                    modifier = Modifier
                        .windowInsetsPadding(
                            WindowInsets.navigationBars.only(WindowInsetsSides.Bottom) // ✅ Bottom만
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
//                    modifier = Modifier.fillMaxWidth(),
//                    contentAlignment = Alignment.Center
                ) {
                    // 배경 이미지가 있는 경우(AI_RESULT)만 풀블리드 이미지 사용
                    val useImageBackground = toastStyle.backgroundRes != null

                    // 이미지 없으면 Surface color에 bg 적용 → 콘텐츠 크기만큼 wrap
                    val surfaceColor = if (useImageBackground) Color.Transparent else toastStyle.bg

                    val fixedWidth = if (t.type == AppToastType.FAVORITE)
                        Modifier.fillMaxWidth(0.90f).widthIn(max = 320.dp)   // FAVORITE만 컴팩트
                    else
                        Modifier.fillMaxWidth(0.92f).widthIn(max = 360.dp)   // 기본


                    Surface(
                        onClick = { hostState.dismiss() },
                        color = surfaceColor,
                        shape = toastStyle.shape,
                        tonalElevation = 0.dp,
                        modifier = fixedWidth.shadow(elevation = 8.dp, shape = toastStyle.shape)
                    ) {
                        if (useImageBackground) {
                            // 이미지 배경: 풀블리드 + 스크림 + 콘텐츠
                            Box(Modifier.clip(toastStyle.shape)) {
                                Image(
                                    painter = painterResource(toastStyle.backgroundRes),
                                    contentDescription = null,
                                    modifier = Modifier.matchParentSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (toastStyle.scrim.alpha > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(toastStyle.scrim)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(contentPadding),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (effectiveIconRes != null) {
                                        Icon(
                                            painter = painterResource(effectiveIconRes),
                                            contentDescription = null,
                                            tint = effectiveIconTint ?: Color.Unspecified,
                                            modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                                        )
                                        Spacer(Modifier.width(if (isCompact) 10.dp else 12.dp))
                                    }

                                    val columnWidth =
                                        if (isCompact) Modifier.wrapContentWidth() else Modifier.fillMaxWidth()

                                    Column(
                                        modifier = columnWidth,
                                        horizontalAlignment = Alignment.CenterHorizontally
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
                        } else {
                            // 이미지 없는 경우: 콘텐츠 크기 기준(wrap) → 단색 배경
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()                  // ⬅ 내부는 항상 fillMaxWidth
                                    .padding(contentPadding),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (effectiveIconRes != null) {
                                    Icon(
                                        painter = painterResource(effectiveIconRes),
                                        contentDescription = null,
                                        tint = effectiveIconTint ?: Color.Unspecified,
                                        modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                                    )
                                    Spacer(Modifier.width(if (isCompact) 10.dp else 12.dp))
                                }

                                val columnWidth =
                                    if (isCompact) Modifier.wrapContentWidth() else Modifier.fillMaxWidth()

                                Column(
                                    modifier = columnWidth,
                                    horizontalAlignment = Alignment.CenterHorizontally
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
}

// ──────────────────────────────────────────────────────────────
// 강조 텍스트 빌더 (특정 단어만 색상 변경)
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
// 데모 화면 (버튼으로 토스트 호출)
//  - NORMAL은 흰 배경
//  - AI_RESULT는 배경 이미지
//  - FAVORITE는 컴팩트 + 좌측 즐겨찾기 아이콘
//  - NEGATIVE는 좌측 에러 아이콘
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
                            type = AppToastType.NORMAL
                        )
                    }
                }) { Text("NORMAL (흰 배경)") }

                Spacer(Modifier.height(8.dp))

                Button(onClick = {
                    scope.launch {
                        host.show(
                            title = buildHighlightedTitle("AI 요약이 완료되었습니다.", "완료"),
                            layout = AppToastLayout.TitleWithSummary,
                            type = AppToastType.AI_RESULT,
                            summary = "핵심 포인트 3가지를 정리했어요"
                        )
                    }
                }) { Text("AI_RESULT (배경 이미지)") }

                Spacer(Modifier.height(8.dp))

                Button(onClick = {
                    scope.launch {
                        host.show(
                            title = AnnotatedString("즐겨찾기에 추가됨"),
                            layout = AppToastLayout.TitleOnly,
                            type = AppToastType.FAVORITE
                        )
                    }
                }) { Text("FAVORITE (컴팩트 + 좌측 아이콘)") }

                Spacer(Modifier.height(8.dp))

                Button(onClick = {
                    scope.launch {
                        host.show(
                            title = AnnotatedString("요청 처리에 실패했어요"),
                            layout = AppToastLayout.TitleWithSummary,
                            type = AppToastType.NEGATIVE,
                            summary = "잠시 후 다시 시도해 주세요"
                        )
                    }
                }) { Text("NEGATIVE (좌측 에러 아이콘)") }
            }
            AppToastHost(hostState = host, styleProvider = styleProvider)
        }
    }
}

// ──────────────────────────────────────────────────────────────
// 오버레이(Popup)로 띄우는 호스트 (필요 시 사용)
// ──────────────────────────────────────────────────────────────
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
//                .statusBarsPadding()
//                .padding(top = 12.dp)
                .windowInsetsPadding(
                    WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                )
                .padding(bottom = 12.dp)
                .zIndex(1000f)
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 프리뷰
// ──────────────────────────────────────────────────────────────
@Preview(showBackground = true, name = "Toast Demo")
@Composable
private fun PreviewToastDemo() {
    MaterialTheme { ToastDemoScreen() }
}

@Preview(showBackground = true, name = "Toast Demo AI")
@Composable
private fun PreviewToastDemoAI() {
    MaterialTheme {
        val host = rememberAppToastHostState()
        LaunchedEffect(Unit) {
            host.show(
                title = buildHighlightedTitle("AI 요약이 완료되었습니다.", "완료"),
                layout = AppToastLayout.TitleWithSummary,
                type = AppToastType.AI_RESULT,
                summary = "3가지 핵심 포인트를 제공해요",
                durationMillis = 2200
            )
        }
        Box(Modifier.fillMaxSize()) { AppToastHost(host) }
    }
}
