package com.example.nubo.ui.screen.card

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import com.example.nubo.model.card.CardDetailItem
import com.example.nubo.ui.theme.GreyMain100
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.PurpleMain500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    item: CardDetailItem,
    onBack: () -> Unit,
    onInfoClick: (() -> Unit)? = null,
    onEdit: (()-> Unit)? = null,
) {
    // 시스템 뒤로가기 키 처리
    BackHandler { onBack() }

    val context = LocalContext.current
    val scrollState = rememberScrollState() // 상위 하나만 스크롤 유지

    Scaffold(
        // 상단 바
        topBar= {CustomTopBar(item.title,onBack,onEdit)},
//        contentWindowInsets = WindowInsets(0)

    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState), // 상위 한 곳에만 스크롤
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ===== 원본 영상(바로가기 버튼, 상세 정보 아이콘) =====
            ImageWithButton(
                item,
                onInfoClick={ onInfoClick?.invoke() },
                onPlayClick = {
                    item.videoUrl.takeIf { it.isNotBlank() }?.let { url ->
                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
                }
            )

            Spacer(Modifier.height(8.dp))

            // ===== 본문 섹션 =====
            DetailBodyMarkdown(
                description = item.description,
            )
            Spacer(Modifier.height(12.dp))
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
    onEdit: (() -> Unit)?= null
){
    CenterAlignedTopAppBar(
        windowInsets = WindowInsets(0),
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
                style = AppTextStyles.subtitle_semibold_20,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            IconButton(onClick = {onEdit?.invoke()}) {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = "수정하기"
                )
            }
        }
    )
}

/**
 * 원본 영상 이미지(원본 영상으로 이동 버튼 + 상세 정보 버튼)
 */
@Composable
private fun ImageWithButton(
    item: CardDetailItem,
    onInfoClick: () -> Unit,
    onPlayClick: () -> Unit
){

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(215.dp)
    ) {
        Image(
            painter = rememberAsyncImagePainter(item.imageUrl),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(7.dp)),
            contentScale = ContentScale.Crop,

        )

        IconButton(
            onClick = onInfoClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(18.dp)
                .size(20.dp) // 전체 버튼 크기
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                painter = painterResource(R.drawable.info),
                contentDescription = "Info",
                tint = Color.White,
                modifier = Modifier.size(20.dp)

            )
        }


        IconButton(
            onClick = onPlayClick,
            modifier = Modifier
                .align(Alignment.Center)
            ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = "Start",
                tint = Color.White,
                modifier = Modifier.size(56.dp)
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
    maxCollapseLines: Int = 9,    // 줄 수 기준으로 접힘 계산
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    // 1) Normalize markdown so that a heading isn't immediately followed by a list
    val normalizedMd = remember(description) {
        description
            // Insert a blank line between heading and list
            .replace(Regex("(?m)^(#{1,6}\\s+.+?)\\n(?=\\s*[-*•]\\s+)"), "$1\n\n")
            // Insert a blank line when a bold line is used like a heading
            .replace(Regex("(?m)^\\*\\*.+?\\*\\*\\s*\\n(?=\\s*[-*•]\\s+)"), "$0\n")
            .trim()
    }

    // 2) Build a safe collapsed markdown by cutting lines (no height/clip needed)
    val collapsedMd = remember(normalizedMd, maxCollapseLines) {
        val lines = normalizedMd.lines()
        lines.take(minOf(lines.size, maxCollapseLines)).joinToString("\n")
    }

    // 3) Decide which markdown to render
    val mdToShow = if (isExpanded) normalizedMd else collapsedMd

    // 4) Determine if collapsing is necessary
    val canCollapse = remember(normalizedMd, maxCollapseLines) {
        normalizedMd.lineSequence().count() > maxCollapseLines
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(Color.White),
        border = BorderStroke(1.5.dp, GreyMain100),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "요약 노트",
                style = AppTextStyles.label_semibold_14,
                color = GreyMain300
            )
            Spacer(Modifier.height(8.dp))

            // 5) Render markdown with safe line height (no heightIn/clip/animateContentSize)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // Apply safe text metrics
                ProvideTextStyle(
                    value = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp, // safe line height
                        platformStyle = PlatformTextStyle(includeFontPadding = true),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Proportional,
                            trim = LineHeightStyle.Trim.None
                        )
                    )
                ) {
                    RichText { Markdown(mdToShow) }
                }

                // 6) Visual fade overlay only when collapsed (purely decorative)
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

            // 7) Toggle button
            if (canCollapse) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 8.dp),
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


// 프리뷰용 더미 데이터
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CardDetailScreenPreview() {
    MaterialTheme {
        CardDetailScreen(
            item = CardDetailItem(
                id = 1,
                title = "Jetpack Compose 완벽 가이드(아주 길어지면 어떡하지?)",
                description = """
## Jetpack Compose 소개

**Jetpack Compose**는 Android의 최신 UI 툴킷입니다.

### 주요 특징

### 1. 선언형 UI
- 상태에 따라 UI가 자동으로 업데이트됩니다
- `@Composable` 함수를 사용합니다

### 2. 완전히 Kotlin으로 작성
```kotlin
@Composable
fun Greeting(name: String) {
    Text(text = "Hello ${'$'}name!")
}
```

### 3. 기존 View 시스템과 상호 운용성
- 기존 앱에 점진적으로 도입 가능
- `ComposeView`와 `AndroidView` 사용

## 장점
- **빠른 개발**: 적은 코드로 더 많은 작업
- **직관적**: UI가 어떻게 보일지 바로 알 수 있음
- **강력함**: 애니메이션, 테마, 접근성 기본 제공

> "Compose makes it fun to build Android UIs"
> - Android Team

더 자세한 내용은 [공식 문서](https://developer.android.com/jetpack/compose)를 참고하세요.
                """.trimIndent(),
                videoUrl = "https://www.youtube.com/watch?v=example",
                date = "2024-01-15T09:00:00Z",
                imageUrl = "https://picsum.photos/seed/compose/800/450",  // or ""
                category = "Android",
                boardSource = "Nubo",
                videoPlatform = "YOUTUBE"
            ),
            onBack = { /* 미리보기에서는 동작하지 않음 */ },
            onInfoClick = { /* 정보 버튼 클릭 */ }
        )
    }
}
