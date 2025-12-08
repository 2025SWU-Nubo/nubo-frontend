package com.example.nubo.ui.screen.recommendCard

import android.R.attr.fontWeight
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.nubo.R
import com.example.nubo.model.card.CardDetailItem
import com.example.nubo.model.card.RecommendCardDetailItem
import com.example.nubo.ui.component.InfoBubble
import com.example.nubo.ui.component.KeywordChip
import com.example.nubo.ui.screen.card.CardDetailViewModel
import com.example.nubo.ui.screen.card.InfoUiState
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey30
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.NuboAppTheme
import com.example.nubo.ui.theme.Purple700
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.utils.standardizeMarkdown
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import kotlin.math.max

@Composable
fun RecommendCardDetailScreen(
    item: RecommendCardDetailItem,
    onBack: () -> Unit,
    onSaveClick: () -> Unit
) {
    // 뒤로가기 처리
    BackHandler { onBack() }

    var showInfoBubble by remember { mutableStateOf(false) }

    // 실제 UI는 Content 쪽에서만 처리
    RecommendCardDetailContent(
        item = item,
        onBack = onBack,
        onSaveClick = onSaveClick,
        showInfoBubble = showInfoBubble,
        onInfoClick = { showInfoBubble = true },
        onDismissInfo = { showInfoBubble = false }
    )
}

@Composable
private fun RecommendCardDetailContent(
    item: RecommendCardDetailItem,
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
    showInfoBubble: Boolean,
    onInfoClick: () -> Unit,
    onDismissInfo: () -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val bottomSafe = rememberImeOrNavBottomPadding(extra = 24.dp) // 하단 여유

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CustomTopBar(
                title = item.title,
                onBack = onBack,
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding() // 시스템 인셋 먼저 처리
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier.fillMaxWidth(),   // Box 안에서 가로 꽉 차게
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleMain500,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "저장하기",
                        style = AppTextStyles.b1_semibold_18
                    )
                }
            }
        }
    ) { inner ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(6.dp))

                ImageWithButton(
                    item = item,
                    onInfoClick = onInfoClick,
                    onPlayClick = {
                        item.videoUrl.takeIf { it.isNotBlank() }?.let { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                        }
                    },
                    showInfoBubble = showInfoBubble,
                    onDismissInfo = onDismissInfo
                )


                DetailBodyMarkdown(description = item.summary)
                CardKeyword(item.tags)

                Spacer(Modifier.height(24.dp))
            }
        }

    }
}

/**
 * 상단 바(뒤로 가기, 제목)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomTopBar(
    title: String,
    onBack: () -> Unit,
) {
    // 카드 제목 글자 수 제한
    val maxLength = 16
    val displayTitle = remember(title) {
        if (title.length > maxLength) {
            title.take(maxLength).trimEnd() + "…"
        } else {
            title
        }
    }

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
                text = displayTitle,
                style = AppTextStyles.b1_semibold_18,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {},
        modifier = Modifier.drawBehind {
            val y = size.height
            drawLine(
                color = Grey50,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
    )
}

/**
 * 원본 영상 이미지(플레이 + 정보 버튼)
 */
@Composable
private fun ImageWithButton(
    item: RecommendCardDetailItem,
    onInfoClick: () -> Unit,
    onPlayClick: () -> Unit,
    showInfoBubble: Boolean = false,
    onDismissInfo: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        // 썸네일 이미지
        Image(
            painter = rememberAsyncImagePainter(item.videoThumbnailUrl),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(7.dp)),
            contentScale = ContentScale.Crop,
        )

        // Info 버튼
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 7.dp, end = 7.dp)
                .size(36.dp)
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
                    .size(20.dp)
            )
        }

        // InfoBubble (CardDetail과 동일한 위치)
        if (showInfoBubble) {

            // 뒤 배경 클릭 → 닫힘
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
                    .zIndex(10f)
            ) {
                InfoBubble(
                    title = "#${item.aiCategoryName}",
                    subtitleLeft = "AI 카테고리",
                    centerValue = item.createdAt,
                    subtitleCenter = "저장한 날짜",
                    subtitleRight = "저장 플랫폼",
                    savedPlatformResId = when (item.videoPlatform.uppercase()) {
                        "YOUTUBE" -> R.drawable.youtube_logo
                        "INSTAGRAM" -> R.drawable.instagram_logo
                        else -> R.drawable.ai_prompt_logo
                    },
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .wrapContentHeight(),
                    tailOnRight = true
                )
            }
        }

        // 재생 버튼
        IconButton(
            onClick = onPlayClick,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = "Start",
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(60.dp)
            )
        }

        // ⭐ 관심사 일치도 배지 (너가 만든 기존 코드 그대로 통합)
        val match = item.matchPercent
        val annotated = buildAnnotatedString {
            append("${item.username}님의 관심사와 ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = PurpleMain500)) {
                append("$match%")
            }
            append(" 일치해요")
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Image(
                painter = painterResource(R.drawable.interestrate_bg),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )

            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.sparkle),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = annotated,
                    style = AppTextStyles.b3_regular_14,
                    color = Purple700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}



/**
 * 요약 노트 마크다운 영역
 */
@Composable
private fun DetailBodyMarkdown(
    description: String,
    visibleLines: Int = 3,
    modifier: Modifier = Modifier
) {
    // 1. 전체 마크다운 정규화
    val normalizedMd = remember(description) {
        standardizeMarkdown(description)
    }

    // 2. 에디터 상태
    val richTextState = rememberRichTextState()

    LaunchedEffect(normalizedMd) {
        if (richTextState.toMarkdown() != normalizedMd) {
            richTextState.setMarkdown(normalizedMd)
        }
    }

    // 3. 줄 수가 maxCollapseLines 보다 많은지 체크
    val hasMore = remember(normalizedMd) {
        normalizedMd.length > 40
    }

    // 4. 3줄 정도 높이만 보여 주도록 Box 높이 계산
    val lineHeightSp = 24.sp
    val visibleHeight = with(LocalDensity.current) {
        (lineHeightSp.toPx() * visibleLines).toDp()
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

            Box(modifier = Modifier
                .fillMaxWidth()
                .height(visibleHeight)
                .clipToBounds()
            ) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = lineHeightSp,
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

                // 5. 아래쪽 흰색 그라데이션 오버레이
                if (hasMore) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(70.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.05f),
                                        Color.White.copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.55f),
                                        Color.White.copy(alpha = 0.9f),
                                    )
                                )
                            )
                    )
                }
            }

            if (hasMore) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "저장하면 요약노트를 모두 볼 수 있어요",
                    style = AppTextStyles.b2_semibold_16,
                    color = PurpleMain500,
                    modifier = Modifier.fillMaxWidth(),   // 중앙 정렬 위해 폭 채우기
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 키워드 영역
 */
@Composable
private fun CardKeyword(
    keywords: List<String>
) {
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
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
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
                        KeywordChip(text = keyword)
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

@Preview(showBackground = true)
@Composable
private fun PreviewRecommendCardDetailScreen() {
    val dummyItem = RecommendCardDetailItem(
        recommendationCardId = 0,
        title = "프리뷰용 추천 카드 제목입니다 프리뷰용 추천 카드 제목입니다",
        summary ="""
            - 이 카드는 프리뷰에서 보여주는 더미 요약 노트입니다
            - 실제 데이터가 들어오면 서버에서 받은 마크다운을 렌더링합니다

            ### 주요 내용
            1. 숏폼을 저장하고
            2. 요약 노트를 확인하고
            3. 추천 카드에서 내 카드로 저장할 수 있어요

             ### 주요 내용
            1. 숏폼을 저장하고
            2. 요약 노트를 확인하고
            3. 추천 카드에서 내 카드로 저장할 수 있어요

             ### 주요 내용
            1. 숏폼을 저장하고
            2. 요약 노트를 확인하고
            3. 추천 카드에서 내 카드로 저장할 수 있어요
        """.trimIndent(),
        tags = listOf("Productivity", "Study hack", "Nubo"),
        videoThumbnailUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        videoPlatform = "YOUTUBE",
        aiCategoryName = "개발 · 프로덕트",
        createdAt = "2025.11.27",
        updatedAt = "2025.11.27",
        username = "누보",
        matchPercent = 83
    )

    NuboAppTheme {
        RecommendCardDetailContent(
            item = dummyItem,
            onBack = {},
            onSaveClick = {},
            showInfoBubble = false,
            onInfoClick = {},
            onDismissInfo = {}
        )
    }
}
