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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey250
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.PurpleMain500
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// ──────────────────────────────────────────────────────────────
// 토스트 레이아웃 종류
//  - TitleOnly              : 제목만 있는 기본형
//  - TitleWithSummary       : 제목 + 한 줄 설명
//  - TitleWithBody          : 제목 + 긴 본문
//  - TitleWithAction        : 제목 + 우측 액션 버튼
//  - TitleWithSummaryAndAction : 제목 + 요약 + 우측 액션 버튼
// ──────────────────────────────────────────────────────────────
enum class AppToastLayout {
    TitleOnly,
    TitleWithSummary,
    TitleWithBody,
    TitleWithAction,
    TitleWithSummaryAndAction
}

// 액션 버튼 슬롯을 갖는 레이아웃인지 여부
private fun AppToastLayout.hasActionSlot(): Boolean =
    this == AppToastLayout.TitleWithAction ||
        this == AppToastLayout.TitleWithSummaryAndAction

// ──────────────────────────────────────────────────────────────
// 토스트 타입
//  - NORMAL       : 기본 회색 배경
//  - POSITIVE     : 성공 아이콘이 붙는 토스트
//  - NEGATIVE     : 에러 아이콘이 붙는 토스트
//  - FAVORITE     : 즐겨찾기용 컴팩트 토스트
//  - AI_RESULT    : 배경 일러스트를 쓰는 특수 토스트
//  - UPLOAD       : 업로드 진행 결과
//  - ALARM_*      : 알림 허용/거부 안내
// ──────────────────────────────────────────────────────────────
enum class AppToastType {
    NORMAL,
    POSITIVE,
    NEGATIVE,
    FAVORITE,
    AI_RESULT,
    UPLOAD,
    ALARM_ALLOWED,
    ALARM_DENIED
}

// ──────────────────────────────────────────────────────────────
// 토스트 데이터 모델
//  - layout        : 레이아웃 타입
//  - title         : 제목(굵게)
//  - summary       : 짧은 설명
//  - body          : 긴 설명
//  - iconRes       : 좌측 아이콘
//  - durationMillis: 화면에 머무는 시간
//  - preDelayMillis: 표시 지연
//  - actionLabel   : 우측 액션 버튼 텍스트
//  - onAction      : 액션 버튼 클릭 콜백
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
    val preDelayMillis: Int = 180,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

// ──────────────────────────────────────────────────────────────
// 스타일 정의
//  - bg           : 배경 색
//  - titleColor   : 제목 색
//  - textColor    : 본문 색
//  - shape        : 카드 모양
//  - backgroundRes: 배경 이미지 리소스
//  - scrim        : 이미지 위에 까는 반투명 오버레이 색
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
// 타입별 기본 스타일 제공자
// ──────────────────────────────────────────────────────────────
@Composable
fun defaultToastStyleProvider(): (AppToastType) -> AppToastStyle = { t ->
    when (t) {
        // AI 결과 토스트는 이미지 배경 사용
        AppToastType.AI_RESULT -> AppToastStyle(
            bg = Color.White,                 // 이미지 실패 시 폴백 배경
            titleColor = Color.Black,
            textColor = Color.White.copy(alpha = 0.55f),
            backgroundRes = R.drawable.toast_bg,
            scrim = Color.Transparent,
            shape = RoundedCornerShape(14.dp)
        )

        // 나머지는 공통 회색 배경
        AppToastType.NORMAL,
        AppToastType.POSITIVE,
        AppToastType.NEGATIVE,
        AppToastType.FAVORITE,
        AppToastType.UPLOAD,
        AppToastType.ALARM_ALLOWED,
        AppToastType.ALARM_DENIED -> AppToastStyle(
            bg = Grey250,
            titleColor = Color.White,
            textColor = Color.White,
            backgroundRes = null,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 기본 아이콘 리소스
// ──────────────────────────────────────────────────────────────
private val DEFAULT_FAVORITE_ICON_RES = R.drawable.favorite
private val DEFAULT_ERROR_ICON_RES = R.drawable.error_toast
private val DEFAULT_POSITIVE_ICON_RES = R.drawable.check
private val DEFAULT_UPLOAD_ICON_RES = R.drawable.upload
private val DEFAULT_ALARM_ALLOW_ICON_RES = R.drawable.alarm_on
private val DEFAULT_ALARM_DENY_ICON_RES = R.drawable.alarm_off

// ──────────────────────────────────────────────────────────────
// 토스트 호스트 상태
//  - current 에 현재 표시 중인 토스트를 보관
//  - Mutex 로 순차 표시를 보장
// ──────────────────────────────────────────────────────────────
//@Stable
//class AppToastHostState {
//
//    private val mutex = Mutex()
//
//    var current by mutableStateOf<AppToastData?>(null)
//        private set
//
//    // AppToastData 를 그대로 넘기는 버전
//    suspend fun show(data: AppToastData) {
//        mutex.withLock {
//            // 표시 지연
//            if (data.preDelayMillis > 0) {
//                delay(data.preDelayMillis.toLong())
//            }
//
//            // 토스트 진입
//            current = data
//            try {
//                // 최소 800ms 이상 유지
//                delay(data.durationMillis.coerceAtLeast(800).toLong())
//            } finally {
//                // 자신일 때만 null 로 만들어 퇴장 시작
//                if (current?.id == data.id) {
//                    current = null
//                }
//            }
//
//            // 퇴장 애니메이션 버퍼
//            delay(460)
//        }
//    }
//
//    // 편의용 오버로드  title 만 필수 인자
//    suspend fun show(
//        title: AnnotatedString,
//        layout: AppToastLayout = AppToastLayout.TitleOnly,
//        type: AppToastType = AppToastType.NORMAL,
//        summary: String? = null,
//        body: String? = null,
//        @DrawableRes iconRes: Int? = null,
//        iconTint: Color? = null,
//        durationMillis: Int = 2000,
//        preDelayMillis: Int = 180,
//        actionLabel: String? = null,
//        onAction: (() -> Unit)? = null,
//    ) = show(
//        AppToastData(
//            id = System.currentTimeMillis(),
//            type = type,
//            layout = layout,
//            title = title,
//            summary = summary,
//            body = body,
//            iconRes = iconRes,
//            iconTint = iconTint,
//            durationMillis = durationMillis,
//            preDelayMillis = preDelayMillis,
//            actionLabel = actionLabel,
//            onAction = onAction
//        )
//    )
//
//    // 즉시 닫기
//    fun dismiss() {
//        current = null
//    }
//}

@Stable
class AppToastHostState {

    private val mutex = Mutex()

    var current by mutableStateOf<AppToastData?>(null)
        private set

    // Overlay가 컴포지션에 붙어 있어야 하는지 여부
    //  - true: Popup를 띄워둠 (preDelay + enter + visible + exit 동안)
    //  - false: Popup 자체를 제거 → 터치 완전 자유
    var overlayVisible by mutableStateOf(false)
        private set

    // AppToastData 를 그대로 넘기는 버전
    suspend fun show(data: AppToastData) {
        mutex.withLock {
            // 1) 먼저 오버레이를 붙여둔다 (이 시점에는 current == null -> AnimatedVisibility는 false)
            overlayVisible = true

            // 2) preDelay 동안은 빈 상태로 기다림 (여기서 Popup는 이미 attach되어 있음)
            if (data.preDelayMillis > 0) {
                delay(data.preDelayMillis.toLong())
            }

            // 3) 이제 토스트 진입 → AnimatedVisibility: false -> true (enter 애니메이션 재생)
            current = data
            try {
                // 최소 800ms 이상 유지
                delay(data.durationMillis.coerceAtLeast(800).toLong())
            } finally {
                // 자신일 때만 null 로 만들어 퇴장 시작
                if (current?.id == data.id) {
                    current = null
                }
            }

            // 4) 퇴장 애니메이션 시간만큼 더 기다렸다가
            delay(460)

            // 5) overlayVisible=false 로 만들어 Popup 자체를 제거
            overlayVisible = false
        }
    }

    // 편의용 오버로드  title 만 필수 인자
    suspend fun show(
        title: AnnotatedString,
        layout: AppToastLayout = AppToastLayout.TitleOnly,
        type: AppToastType = AppToastType.NORMAL,
        summary: String? = null,
        body: String? = null,
        @DrawableRes iconRes: Int? = null,
        iconTint: Color? = null,
        durationMillis: Int = 2000,
        preDelayMillis: Int = 180,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
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
            preDelayMillis = preDelayMillis,
            actionLabel = actionLabel,
            onAction = onAction
        )
    )

    // 즉시 닫기
    fun dismiss() {
        current = null
    }
}


// remember 용 헬퍼
@Composable
fun rememberAppToastHostState(): AppToastHostState = remember { AppToastHostState() }

// CompositionLocal  전역 토스트 액세스용
val LocalAppToastHostState = staticCompositionLocalOf<AppToastHostState> {
    error("AppToastHostState is not provided")
}

// ──────────────────────────────────────────────────────────────
// 토스트 호스트 UI
//  - Box 내부에서 AnimatedVisibility 로 등장/퇴장
//  - 마지막 렌더링 데이터를 별도로 기억해 exit 중에 내용이 사라지지 않게 처리
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

    // 마지막으로 표시된 토스트를 보관
    var rendered by remember { mutableStateOf<AppToastData?>(null) }
    if (data != null && rendered?.id != data.id) {
        rendered = data
    }

    val rootModifier =
        if (matchParentSize) modifier.fillMaxSize() else modifier

    Box(
        modifier = rootModifier.semantics(mergeDescendants = true) {},
        contentAlignment = contentAlignment
    ) {
        AnimatedVisibility(
            visible = data != null,
            enter =
                fadeIn(
                    animationSpec = tween(
                        durationMillis = 180,
                        easing = LinearOutSlowInEasing
                    )
                ) +
                    scaleIn(
                        initialScale = 0.97f,
                        animationSpec = tween(
                            durationMillis = 180,
                            easing = LinearOutSlowInEasing
                        )
                    ) +
                    slideInVertically(
                        initialOffsetY = { 20 }, // 화면 전체 기준 X, 40px 아래에서 시작
                        animationSpec = tween(
                            durationMillis = 200,
                            easing = LinearOutSlowInEasing
                        )
                    ),
            exit =
                fadeOut(
                    animationSpec = tween(
                        durationMillis = 140,
                        easing = FastOutLinearInEasing
                    )
                ) +
                    slideOutVertically(
                        targetOffsetY = { 30 }, // 살짝 아래로 떨어지며 종료
                        animationSpec = tween(
                            durationMillis = 160,
                            easing = FastOutLinearInEasing
                        )
                    )
        ) {
            val t = data ?: rendered ?: return@AnimatedVisibility
            val toastStyle = styleProvider(t.type)

            // 타입에 따른 기본 아이콘
            val isFavorite = t.type == AppToastType.FAVORITE
            val isNegative = t.type == AppToastType.NEGATIVE
            val isPositive = t.type == AppToastType.POSITIVE
            val isUpload = t.type == AppToastType.UPLOAD
            val isAlarmAllow = t.type == AppToastType.ALARM_ALLOWED
            val isAlarmDeny = t.type == AppToastType.ALARM_DENIED

            val effectiveIconRes: Int? = when {
                t.iconRes != null -> t.iconRes
                isFavorite -> DEFAULT_FAVORITE_ICON_RES
                isNegative -> DEFAULT_ERROR_ICON_RES
                isPositive -> DEFAULT_POSITIVE_ICON_RES
                isUpload -> DEFAULT_UPLOAD_ICON_RES
                isAlarmAllow -> DEFAULT_ALARM_ALLOW_ICON_RES
                isAlarmDeny -> DEFAULT_ALARM_DENY_ICON_RES
                else -> null
            }

            // 즐겨찾기만 컴팩트 패딩 사용
            val isCompact = isFavorite

            val useImageBackground = toastStyle.backgroundRes != null
            val surfaceColor =
                if (useImageBackground) Color.Transparent else toastStyle.bg

            val fixedWidth =
                Modifier.fillMaxWidth(1f).widthIn(max = 460.dp)

            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
            ) {
                Surface(
                    onClick = { hostState.dismiss() }, // 토스트 탭 시 바로 닫기
                    color = surfaceColor,
                    shape = toastStyle.shape,
                    tonalElevation = 0.dp,
                    modifier = fixedWidth.shadow(
                        elevation = 5.dp,
                        shape = toastStyle.shape
                    )
                ) {
                    // 이미지 배경 토스트와 일반 토스트를 분리 렌더링
                    if (useImageBackground) {
                        // AI_RESULT 전용 레이아웃  배경 이미지만 사용
                        val contentPadding =
                            if (isCompact) PaddingValues(horizontal = 24.dp, vertical = 18.dp)
                            else PaddingValues(horizontal = 30.dp, vertical = 18.dp)

                        Box(Modifier.clip(toastStyle.shape)) {
                            Image(
                                painter = painterResource(toastStyle.backgroundRes!!),
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
                                    androidx.compose.material3.Icon(
                                        painter = painterResource(effectiveIconRes),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                                    )
                                    Spacer(
                                        Modifier.width(
                                            if (isCompact) 8.dp else 10.dp
                                        )
                                    )
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = t.title,
                                        color = toastStyle.titleColor,
                                        style = AppTextStyles.b2_medium_16,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Start
                                    )
                                    if (t.layout == AppToastLayout.TitleWithSummary) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = t.summary.orEmpty(),
                                            color = toastStyle.textColor,
                                            style = AppTextStyles.label_medium_14,
                                            lineHeight = 20.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Start
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // 대부분의 회색 배경 토스트는 공통 레이아웃 사용
                        ToastRowContent(
                            data = t,
                            toastStyle = toastStyle,
                            effectiveIconRes = effectiveIconRes,
                            isCompact = isCompact
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// 공통 토스트 Row 레이아웃
//  - 좌측 아이콘
//  - 가운데 텍스트
//  - 우측 액션 버튼(선택)
//  액션 레이아웃일 때는 아이콘을 숨기고 버튼만 보여줌
// ──────────────────────────────────────────────────────────────
@Composable
private fun ToastRowContent(
    data: AppToastData,
    toastStyle: AppToastStyle,
    effectiveIconRes: Int?,
    isCompact: Boolean
) {
    val hasAction = data.layout.hasActionSlot() &&
        !data.actionLabel.isNullOrBlank() &&
        data.onAction != null

    // 액션 버튼이 있는 토스트에서는 아이콘을 숨김
    val showIcon = effectiveIconRes != null && !hasAction

    val hasSummary = data.layout == AppToastLayout.TitleWithSummary ||
        data.layout == AppToastLayout.TitleWithSummaryAndAction
    val hasBody = data.layout == AppToastLayout.TitleWithBody

    val contentPadding =
        if (isCompact) PaddingValues(horizontal = 20.dp, vertical = 14.dp)
        else PaddingValues(horizontal = 24.dp, vertical = 14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // 좌측 아이콘
        if (showIcon) {
            androidx.compose.material3.Icon(
                painter = painterResource(effectiveIconRes!!),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
            )
            Spacer(Modifier.width(if (isCompact) 10.dp else 12.dp))
        }

        // 가운데 텍스트 영역
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = data.title,
                color = toastStyle.titleColor,
                style = AppTextStyles.b2_semibold_16,
                maxLines =
                    if (data.layout == AppToastLayout.TitleOnly ||
                        data.layout == AppToastLayout.TitleWithAction
                    ) 2 else 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )

            when {
                hasSummary -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = data.summary.orEmpty(),
                        color = toastStyle.textColor,
                        style = AppTextStyles.b3_regular_14,
                        lineHeight = 20.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }

                hasBody -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = data.body.orEmpty(),
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

        // 우측 액션 버튼  회색 pill 버튼
        if (hasAction) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .wrapContentWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(GreyMain300)
                    .clickable { data.onAction?.invoke() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = data.actionLabel.orEmpty(),
                    style = AppTextStyles.b3_semibold_14,
                    color = Color.White
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// 강조 텍스트 빌더
//  - full 문자열 안에서 highlight 부분만 색상을 바꿔서 AnnotatedString 생성
// ──────────────────────────────────────────────────────────────
fun buildHighlightedTitle(
    full: String,
    highlight: String,
    highlightColor: Color = PurpleMain500
): AnnotatedString = buildAnnotatedString {
    val idx = full.indexOf(highlight)
    if (idx < 0) {
        append(full)
    } else {
        append(full.substring(0, idx))
        pushStyle(SpanStyle(color = highlightColor))
        append(highlight)
        pop()
        append(full.substring(idx + highlight.length))
    }
}

// ──────────────────────────────────────────────────────────────
// 데모용 화면  여러 타입의 토스트를 테스트할 수 있음
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
                            title = buildHighlightedTitle(
                                "토스트의 제목이 여기에 작성됩니다",
                                "제목"
                            ),
                            layout = AppToastLayout.TitleOnly,
                            type = AppToastType.POSITIVE
                        )
                    }
                }) { Text("POSITIVE") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    scope.launch {
                        host.show(
                            title = buildHighlightedTitle(
                                "토스트의 제목이 여기에 작성됩니다",
                                "제목"
                            ),
                            layout = AppToastLayout.TitleOnly,
                            type = AppToastType.NORMAL
                        )
                    }
                }) { Text("NORMAL") }

                Spacer(Modifier.height(8.dp))

                Button(onClick = {
                    scope.launch {
                        host.show(
                            title = buildHighlightedTitle("AI 요약이 완료되었습니다", "완료"),
                            layout = AppToastLayout.TitleWithSummary,
                            type = AppToastType.AI_RESULT,
                            summary = "핵심 포인트 3가지를 정리했어요"
                        )
                    }
                }) { Text("AI_RESULT") }

                Spacer(Modifier.height(8.dp))

                Button(onClick = {
                    scope.launch {
                        host.show(
                            title = AnnotatedString("즐겨찾기에 추가됨"),
                            layout = AppToastLayout.TitleOnly,
                            type = AppToastType.FAVORITE
                        )
                    }
                }) { Text("FAVORITE") }

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
                }) { Text("NEGATIVE") }

                Spacer(Modifier.height(8.dp))

                Button(onClick = {
                    scope.launch {
                        host.show(
                            title = AnnotatedString("보드 생성이 완료되었습니다"),
                            layout = AppToastLayout.TitleWithAction,
                            type = AppToastType.NORMAL,
                            actionLabel = "바로가기",
                            onAction = { /* 보드 화면으로 이동 */ }
                        )
                    }
                }) { Text("ACTION") }
            }

            AppToastHost(hostState = host, styleProvider = styleProvider)
        }
    }
}

// ──────────────────────────────────────────────────────────────
// 전체 앱 위에 떠 있는 오버레이 Popup
//  - 네비게이션 바 인셋만큼 위로 올림
//  - hostState.current 가 null 이어도 exit 애니메이션 동안 유지
// ──────────────────────────────────────────────────────────────
@Composable
fun AppToastOverlay(
    hostState: AppToastHostState,
    extraBottomOffset: Dp = 4.dp,
) {
    if (!hostState.overlayVisible) return

    val bottomInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    Popup(
        alignment = Alignment.BottomCenter,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            excludeFromSystemGesture = false,   // ← 이것도 터치 막힘 방지
            usePlatformDefaultWidth = false
        )
    ) {
        // **터치를 소비하지 않는 Box**
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(bottom = bottomInset + extraBottomOffset)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppToastHost(
                    hostState = hostState,
                    matchParentSize = false,
                    contentAlignment = Alignment.BottomCenter
                )
            }
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
                title = buildHighlightedTitle("AI 요약이 완료되었습니다", "완료"),
                layout = AppToastLayout.TitleWithSummary,
                type = AppToastType.AI_RESULT,
                summary = "3가지 핵심 포인트를 제공해요",
                durationMillis = 2200
            )
        }
        Box(Modifier.fillMaxSize()) { AppToastHost(host) }
    }
}
