

package com.example.nubo.ui.screen.card

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBarDefaults.windowInsets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.components.toast.AppToastLayout
import com.example.components.toast.AppToastType
import com.example.components.toast.LocalAppToastHostState
import com.example.nubo.model.card.CardDetailItem
import com.example.nubo.ui.component.InfoBubble
import com.example.nubo.ui.theme.Grey30
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.GreyMain100
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple50
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.utils.standardizeMarkdown
import com.google.firebase.annotations.concurrent.Background
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import com.example.nubo.ui.component.KeywordChip


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    item: CardDetailItem,
    onBack: () -> Unit,
    onInfoClick: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onToggleFavorite: () -> Unit,
    toastMessage: String?,
    onConsumeToast: () -> Unit,
    toastDelayMillis : Int = 0,
    navController: NavController,
    // 레벨업 토스트 파라미터
    toastMessage2: Pair<AnnotatedString, String>?,
    onConsumeToast2: () -> Unit,
    viewModel: CardDetailViewModel = hiltViewModel()
) {
    // 뒤로가기 처리
    BackHandler { onBack() }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 전역 토스트 호스트 가져오기
    val toastHost = LocalAppToastHostState.current

    val bottomSafe = rememberImeOrNavBottomPadding(extra = 24.dp) // 토스트 + 여유
    val scope = rememberCoroutineScope()

    val infoState by viewModel.infoState.collectAsState()

    //  토스트 메시지가 들어오면 지연 후 노출
    LaunchedEffect(toastMessage, toastDelayMillis) {
        val msg = toastMessage
        if (!msg.isNullOrEmpty()) {
            // 지연 시간이 설정된 경우 지정 ms 만큼 대기
            if (toastDelayMillis > 0) {
                delay(toastDelayMillis.toLong())
            }
            toastHost.show(
                title = AnnotatedString(msg),
                layout = AppToastLayout.TitleOnly,
                type = AppToastType.POSITIVE,
                durationMillis = 2200
            )
            // 한 번만 보이도록 즉시 소거
            onConsumeToast()
        }
    }

    // 레벨업/열매 토스트 표시
    LaunchedEffect(toastMessage2) {
        val msgPair = toastMessage2 ?: return@LaunchedEffect

        val (title, summary) = msgPair


        // Show action toast
        toastHost.show(
            title = title,
            layout = AppToastLayout.TitleWithSummaryAndAction,
            type = AppToastType.NORMAL,
            summary = summary,
            actionLabel = "바로가기",
            onAction = {
                navController.navigate("learn")
            },
            durationMillis = 2800
        )

        // Consume toast so it will not show again
        onConsumeToast2()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {

            CustomTopBar(
                title = item.title,
                onBack = onBack,
                onEdit = onEdit,
                isFavorite = item.isFavorite,
                onToggleFavorite = {
                    val willBeFavorite = !item.isFavorite

                    // 1) 먼저 즐겨찾기 상태 토글 (ViewModel.toggleFavorite)
                    onToggleFavorite()

                    // 2) 토스트는 별도 코루틴에서 실행 (UI 즉시 반영)
                    scope.launch {
                        toastHost.show(
                            title = AnnotatedString(
                                if (willBeFavorite) "즐겨찾기에 추가했어요!" else "즐겨찾기를 해제했어요!"
                            ),
                            layout = AppToastLayout.TitleOnly,
                            type = AppToastType.FAVORITE,
                            durationMillis = 1800
                        )
                    }
                }
            )
        }
    ) { inner ->

        // 아래 오버레이 패딩 계산
        val density = LocalDensity.current
        val imeBottomPx = WindowInsets.ime.getBottom(density)
        val navBottomPx = WindowInsets.navigationBars.getBottom(density)// 키보드 높이
        // Int끼리 먼저 max
        val bottomInsetPx = max(imeBottomPx, navBottomPx)
        // Dp로 변환
        val bottomInsetDp = with(density) { bottomInsetPx.toDp() }
        // Scaffold의 패딩(Dp) + 계산된 Dp + 여백
        val finalBottomPadding = inner.calculateBottomPadding() + bottomInsetDp


        Box( // 오버레이 컨테이너
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            // 본문
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(6.dp))
                ImageWithButton(
                    item = item,
                    onInfoClick = {
                        viewModel.showInfoBubble()
                        onInfoClick?.let { it() }
                    },
                    onPlayClick = {
                        item.videoUrl.takeIf { it.isNotBlank() }?.let { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                        }
                    },
                    showInfoBubble = infoState is InfoUiState.Visible,
                    onDismissInfo = { viewModel.hideInfoBubble() }
                )
//                Spacer(Modifier.height(8.dp))
                DetailBodyMarkdown(description = item.summary)
                CardKeyword(item.tags)

                Spacer(Modifier.height(bottomSafe))
            }
        }
    }
}



/**
 * 상단 바(뒤로 가기, 제목, 수정 하기)
 * **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomTopBar(
    title: String,
    onBack: () -> Unit,
    onEdit: (() -> Unit)?= null,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
){
    CenterAlignedTopAppBar(
        windowInsets = WindowInsets.statusBars,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = "뒤로가기"
                )
            }
        },
        title = {
            Text(
                text = title,
                style = AppTextStyles.b1_semibold_18,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    painter = painterResource(
                        if (isFavorite) R.drawable.selected_star else R.drawable.unselected_star
                    ),
                    tint = Color.Unspecified,
                    contentDescription = if (isFavorite) "즐겨찾기 해제" else "즐겨찾기 설정"
                )
            }
            IconButton(onClick = { onEdit?.invoke() }) {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = "수정하기"
                )
            }
        },
        modifier = Modifier.drawBehind {
            val y = size.height
            drawLine(
                color = Grey50,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end   = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
    )
}

/**
 * 원본 영상 이미지(원본 영상으로 이동 버튼 + 상세 정보 버튼)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageWithButton(
    item: CardDetailItem,
    onInfoClick: () -> Unit,
    onPlayClick: () -> Unit,
    showInfoBubble: Boolean = false,
    onDismissInfo: () -> Unit = {}
){

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Image(
            painter = rememberAsyncImagePainter(item.videoThumbnailUrl),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(7.dp)),
            contentScale = ContentScale.Crop,
        )

        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 7.dp, end = 7.dp)
                .size(36.dp) // background size
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable { onInfoClick() }
        ) {
            Icon(
                painter = painterResource(R.drawable.info),
                contentDescription = "Info",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(20.dp) // icon size
            )
        }


        if (showInfoBubble) {
            Box(
                Modifier
                    .matchParentSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDismissInfo() }
            )

            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 48.dp)
            ) {
                InfoBubble(
                    title = "#${item.boardName}",
                    subtitleLeft = "AI 카테고리",
                    centerValue = item.createdAt,
                    subtitleCenter = "저장한 날짜",
                    subtitleRight = "저장 플랫폼",
                    savedPlatformResId = when (item.videoPlatform.uppercase()) {
                        "YOUTUBE" -> R.drawable.youtube_logo
                        "INSTAGRAM" -> R.drawable.instagram_logo
                        else -> R.drawable.basic_profile_image
                    },
                    // Make it compact
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .wrapContentHeight(),
                    tailOnRight = true
                )
            }
        }




        IconButton(
            onClick = onPlayClick,
            modifier = Modifier
                .align(Alignment.Center)
        ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = "Start",
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(60.dp)
            )
        }
    }
}


/**
 * 카드 상세(제목 + 구분선 + Markdown)
 */
@Composable
private fun DetailBodyMarkdown(
    description: String,
    maxCollapseLines: Int = 9,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    // 1) 서버에서 온 마크다운을 에디터와 동일하게 정규화
    val normalizedMd = remember(description) {
        standardizeMarkdown(description)
    }

    // 2) 접힘 상태일 때 사용할 "잘린 마크다운"
    val collapsedMd = remember(normalizedMd, maxCollapseLines) {
        val lines = normalizedMd.lines()
        lines.take(minOf(lines.size, maxCollapseLines)).joinToString("\n")
    }

    val mdToShow = if (isExpanded) normalizedMd else collapsedMd


    // 3) compose-rich-editor 의 RichTextState 사용 (읽기 전용 용도)
    val richTextState = rememberRichTextState()

    // 4) mdToShow 가 바뀔 때만 setMarkdown 호출 (무한 루프 방지)
    LaunchedEffect(mdToShow) {
        if (richTextState.toMarkdown() != mdToShow) {
            richTextState.setMarkdown(mdToShow)
        }
    }

    // 5) 접기/펼치기 가능 여부
    val canCollapse = remember(normalizedMd, maxCollapseLines) {
        normalizedMd.lineSequence().count() > maxCollapseLines
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(180)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(Color.White),
        border = BorderStroke(1.dp, Grey30),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            Text(
                text = "요약 노트",
                style = AppTextStyles.b2_semibold_16,
                color = Grey500
            )
            Spacer(Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = true),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Proportional,
                            trim = LineHeightStyle.Trim.None
                        )
                    )
                ) {
                    RichText(
                        state = richTextState,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!isExpanded && canCollapse) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.7f),
                                        Color.White
                                    )
                                )
                            )
                    )
                }
            }

            if (canCollapse) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val label = if (isExpanded) "접기" else "펼치기"
                    Text(text = label, style = AppTextStyles.b1_semibold_18, color = PurpleMain500)
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        painter = painterResource(
                            if (isExpanded) R.drawable.keyboard_arrow_up else R.drawable.ic_arrow_down
                        ),
                        contentDescription = label,
                        tint = PurpleMain500,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CardKeyword(
    keywords: List<String>
){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 500,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(Color.White),
        border = BorderStroke(1.dp, Grey30),
    ) {
        Column (
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
        ){
            Text(text = "포함된 키워드", style = AppTextStyles.b2_semibold_16, color = Grey500)
            Spacer(Modifier.height(12.dp))


            if (keywords.isEmpty()) {
                Text(
                    text = "키워드가 없어요",
                    style = AppTextStyles.b2_regular_16,
                    color = GreyMain300
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    keywords.forEach { keyword ->
                        KeywordChip(
                            text = keyword,
                        )
                    }
                }
            }
        }
    }
}

// 하단 인셋 계산 헬퍼
@Composable
private fun rememberImeOrNavBottomPadding(extra: Dp = 0.dp): Dp {
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val navBottom = WindowInsets.navigationBars.getBottom(density)
    val bottomPx = max(imeBottom, navBottom)
    return with(density) { bottomPx.toDp() } + extra
}



// 프리뷰용 더미 데이터
//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun CardDetailScreenPreview() {
//    MaterialTheme {
//        CardDetailScreen(
//            item = CardDetailItem(
//                id = 1,
//                title = "Jetpack Compose 완벽 가이드(아주 길어지면 어떡하지?)",
//                description = """
//## Jetpack Compose 소개
//
//**Jetpack Compose**는 Android의 최신 UI 툴킷입니다.
//
//### 주요 특징
//
//### 1. 선언형 UI
//- 상태에 따라 UI가 자동으로 업데이트됩니다
//- `@Composable` 함수를 사용합니다
//
//### 2. 완전히 Kotlin으로 작성
//```kotlin
//@Composable
//fun Greeting(name: String) {
//    Text(text = "Hello ${'$'}name!")
//}
//```
//
//### 3. 기존 View 시스템과 상호 운용성
//- 기존 앱에 점진적으로 도입 가능
//- `ComposeView`와 `AndroidView` 사용
//
//## 장점
//- **빠른 개발**: 적은 코드로 더 많은 작업
//- **직관적**: UI가 어떻게 보일지 바로 알 수 있음
//- **강력함**: 애니메이션, 테마, 접근성 기본 제공
//
//> "Compose makes it fun to build Android UIs"
//> - Android Team
//
//더 자세한 내용은 [공식 문서](https://developer.android.com/jetpack/compose)를 참고하세요.
//                """.trimIndent(),
//                videoUrl = "https://www.youtube.com/watch?v=example",
//                date = "2024-01-15T09:00:00Z",
//                imageUrl = "https://picsum.photos/seed/compose/800/450",  // or ""
//                category = "Android",
//                boardSource = "Nubo",
//                videoPlatform = "YOUTUBE",
//                tags = ["# frontend","# android"]
//            ),
//            onBack = { /* 미리보기에서는 동작하지 않음 */ },
//            onInfoClick = { /* 정보 버튼 클릭 */ }
//        )
//    }
//}
