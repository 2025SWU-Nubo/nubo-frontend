@file:Suppress("FunctionName", "unused")

package com.example.components.toast

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.nubo.R
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Grey250
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple300
import kotlinx.coroutines.launch

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
enum class AppToastType { NORMAL, POSITIVE, NEGATIVE, FAVORITE, AI_RESULT,UPLOAD, ALARM_ALLOWED,ALARM_DENIED }

// ──────────────────────────────────────────────────────────────

// 데이터 모델
//   - preDelayMillis: 표시 지연(시트 닫힌 뒤 약간 기다렸다 띄우기 등)
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
    val durationMillis: Int = 2000,
    val preDelayMillis: Int = 120
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
// ──────────────────────────────────────────────────────────────
@Composable
fun defaultToastStyleProvider(): (AppToastType) -> AppToastStyle = { t ->
    when (t) {
        AppToastType.AI_RESULT -> AppToastStyle(
            bg = Color.White,                 // 이미지 로딩 실패 시 폴백
            titleColor = Color.Black,
            textColor = Color.White.copy(alpha = 0.55f),
            backgroundRes = R.drawable.toast_bg,
            scrim = Color.Transparent,
            shape = RoundedCornerShape(14.dp)
        )
        AppToastType.NORMAL,
        AppToastType.POSITIVE,
        AppToastType.NEGATIVE,
        AppToastType.FAVORITE,
        AppToastType.UPLOAD,
        AppToastType.ALARM_ALLOWED,
            AppToastType.ALARM_DENIED
        -> AppToastStyle(
            bg = Grey250,
            titleColor = Color.White,
            textColor = Color.White,
            backgroundRes = null,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 기본 아이콘(강제 노출용)
// ──────────────────────────────────────────────────────────────
private val DEFAULT_FAVORITE_ICON_RES = R.drawable.favorite
private val DEFAULT_ERROR_ICON_RES = R.drawable.error_toast
private val DEFAULT_POSITIVE_ICON_RES = R.drawable.check
private val DEFAULT_UPLOAD_ICON_RES = R.drawable.upload
private val DEFAULT_ALARM_ALLOW_ICON_RES = R.drawable.alarm_on
private val DEFAULT_ALARM_DENY_ICON_RES = R.drawable.alarm_off

// ──────────────────────────────────────────────────────────────
// 호스트 상태 (순차 처리 + 표시 지연 + exit 버퍼)
// ──────────────────────────────────────────────────────────────
@Stable
class AppToastHostState
{
    private val mutex = Mutex()
    var current by mutableStateOf<AppToastData?>(null)
        private set

    suspend fun show(data: AppToastData) {
        mutex.withLock {
            // 1) 표시 지연 (시트 닫힘 이후 살짝 기다렸다가 띄우고 싶을 때 유용)
            if (data.preDelayMillis > 0) delay(data.preDelayMillis.toLong())

            // 2) 현재 토스트 진입
            current = data
            try {
                // 표시 시간 유지 (최소 800ms 보장)
                delay(data.durationMillis.coerceAtLeast(800).toLong())
            } finally {
                // 3) 자신이라면 null로 만들어 exit 시작
                if (current?.id == data.id) current = null
            }

            // 4) 퇴장 애니메이션 길이에 맞게 추가 버퍼(겹침 방지)
            delay(460)
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
        durationMillis: Int = 2000,
        preDelayMillis: Int = 120
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
            durationMillis = durationMillis,
            preDelayMillis = preDelayMillis
        )
    )

    fun dismiss() { current = null }
}



@Composable
fun rememberAppToastHostState(): AppToastHostState = remember { AppToastHostState() }

// 전역 토스트 호스트를 제공하기 위한 CompositionLocal
val LocalAppToastHostState = staticCompositionLocalOf<AppToastHostState> {
    error("AppToastHostState is not provided")
}

// ──────────────────────────────────────────────────────────────
// 토스트 호스트 UI
//   - 마지막 non-null 데이터 래치: exit 동안에도 내용이 바뀌지 않게
//   - 퇴장: slideOut + fadeOut + scaleOut(살짝 확대)로 “안드 내장 토스트” 느낌
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

    // 마지막 non-null 토스트를 기억해 두었다가 exit 중에도 유지
    var rendered by remember { mutableStateOf<AppToastData?>(null) }
    if (data != null && rendered?.id != data.id) {
        rendered = data
    }

    val rootModifier = if (matchParentSize) modifier.fillMaxSize() else modifier

    Box(
        modifier = rootModifier.semantics(mergeDescendants = true) {},
        contentAlignment = contentAlignment
    ) {
        AnimatedVisibility(
            visible = data != null,
            // 아래서 살짝 올라오며 페이드인
            enter = scaleIn(
                initialScale = 0.97f,
                animationSpec = tween(
                    durationMillis = 140,
                    easing = LinearOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 120,
                    easing = LinearOutSlowInEasing
                )
            ),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = 140,
                    easing = FastOutLinearInEasing
                )
            )
        ) {
            // exit 중에도 contentData가 유지됨 (data를 직접 쓰면 null로 사라지며 끊길 수 있음)
            val t = data ?: rendered ?: return@AnimatedVisibility

            val toastStyle = styleProvider(t.type)

            // 타입별 아이콘 강제 규칙
            val isFavorite = t.type == AppToastType.FAVORITE
            val isNegative = t.type == AppToastType.NEGATIVE
            val isPositive = t.type == AppToastType.POSITIVE
            val isUpload = t.type == AppToastType.UPLOAD
            val isAlarmAllow = t.type == AppToastType.ALARM_ALLOWED
            val isAlarmDeny = t.type == AppToastType.ALARM_DENIED

            // 아이콘 리소스
            val effectiveIconRes: Int? = when {
                t.iconRes != null -> t.iconRes
                isFavorite -> DEFAULT_FAVORITE_ICON_RES
                isNegative -> DEFAULT_ERROR_ICON_RES
                isPositive -> DEFAULT_POSITIVE_ICON_RES
                isUpload -> DEFAULT_UPLOAD_ICON_RES
                isAlarmAllow -> DEFAULT_ALARM_ALLOW_ICON_RES
                isAlarmDeny-> DEFAULT_ALARM_DENY_ICON_RES
                else -> null
            }

            // FAVORITE만 컴팩트
            val isCompact = isFavorite
            val contentPadding =
                if (isCompact) PaddingValues(horizontal = 24.dp, vertical = 18.dp)
                else PaddingValues(horizontal = 30.dp, vertical = 18.dp)

            val useImageBackground = toastStyle.backgroundRes != null
            // 토스트 배경 컬러
            val surfaceColor = if (useImageBackground) Color.Transparent else toastStyle.bg

            val fixedWidth = Modifier.fillMaxWidth(1f).widthIn(max = 460.dp)

            // 확대/축소의 기준점을 중앙으로 고정(
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .graphicsLayer { transformOrigin = TransformOrigin(0.5f, 0.5f) }
            ) {
                Surface(
                    onClick = { hostState.dismiss() },         // 탭하여 즉시 닫기
                    color = surfaceColor,
                    shape = toastStyle.shape,
                    tonalElevation = 0.dp,
                    modifier = fixedWidth.shadow(elevation = 5.dp, shape = toastStyle.shape)
                ) {
                    if (useImageBackground) {
                        // 이미지 배경(풀블리드) + 스크림 + 중앙 정렬
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
                                horizontalArrangement = Arrangement.Start
                            ) {
                                if (effectiveIconRes != null) {
                                    Icon(
                                        painter = painterResource(effectiveIconRes),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                                    )
                                    Spacer(Modifier.width(if (isCompact) 8.dp else 10.dp))
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
                                        textAlign = TextAlign.Start
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
                                                textAlign = TextAlign.Start
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
                                                textAlign = TextAlign.Start
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // 단색 배경 + 중앙 정렬
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(contentPadding),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            if (effectiveIconRes != null) {
                                Icon(
                                    painter = painterResource(effectiveIconRes),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                                )
                                Spacer(Modifier.width(if (isCompact) 10.dp else 12.dp))
                            }

                            val columnWidth =
                                if (isCompact) Modifier.wrapContentWidth() else Modifier.fillMaxWidth()

                            Column(
                                modifier = columnWidth,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = t.title,
                                    color = toastStyle.titleColor,
                                    style = AppTextStyles.b2_semibold_16,
                                    maxLines = if (t.layout == AppToastLayout.TitleOnly) 2 else 3,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Start
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
                                            textAlign = TextAlign.Start
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
                                            textAlign = TextAlign.Start
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
// 데모 화면
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
// 오버레이(Popup)
//   - current == null이어도 항상 그려서 exit 애니메이션이 끊기지 않도록 함
//   - 네비 바 인셋만큼 위로 띄워 위치 안정
// ──────────────────────────────────────────────────────────────
@Composable
fun AppToastOverlay(
    hostState: AppToastHostState,
    extraBottomOffset: Dp = 52.dp,
) {
    // 네비게이션 바 인셋 계산 (한 번만)
    val bottomInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    // 토스트 표시 여부를 오버레이 레벨에서 관리
    var showOverlay by remember { mutableStateOf(false) }
    // exit 동안만 잠깐 더 유지
    LaunchedEffect(hostState.current) {
        if (hostState.current != null) {
            showOverlay = true
        } else {
            // AppToastHost.exit 애니메이션 140ms + 버퍼
            kotlinx.coroutines.delay(200)
            showOverlay = false
        }
    }

    if (!showOverlay) return  // 완전히 사라지면 Popup 자체를 제거해 터치 통과

    Popup(
        alignment = Alignment.BottomCenter,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            excludeFromSystemGesture = true,
            usePlatformDefaultWidth = false
        )
    ) {
        // 하단 바 클릭영역과 겹치지 않도록 충분히 올려둠
        Column(
            modifier = Modifier
                .padding(bottom = bottomInset + extraBottomOffset)
                .fillMaxWidth(),   // 폭 안정 (크래시 방지)
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppToastHost(
                hostState = hostState,
                matchParentSize = false,                 // Overlay가 전체 영역이라 false로 충분
                contentAlignment = Alignment.BottomCenter
            )
        }
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
