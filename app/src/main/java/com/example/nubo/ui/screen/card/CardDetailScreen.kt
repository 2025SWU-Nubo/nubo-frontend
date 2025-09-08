// app/src/main/java/com/example/nubo/ui/screen/card/CardDetailScreen.kt
package com.example.nubo.ui.screen.card

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey30
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import androidx.core.net.toUri
import com.example.nubo.model.card.CardDetailItem
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.GreyMain300

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    item: CardDetailItem,
    onBack: () -> Unit,
    onInfoClick: (() -> Unit)? = null
) {
    // 시스템 뒤로가기 키 처리
    BackHandler { onBack() }

    val context = LocalContext.current
    val scrollState = rememberScrollState() // 상위 하나만 스크롤 유지

    Scaffold(
        topBar = {
            TopAppBar(
                // 상단 앱바: 제목은 비워두고, 본문에서 중앙 큰 타이틀을 사용
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // ImageVector가 아닌 리소스 아이콘 사용 시 painterResource
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back),
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onInfoClick?.invoke() }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "정보"
                        )
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scrollState), // 상위 한 곳에만 스크롤
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== 제목(중앙 정렬, 크게) =====
            Text(
                text = item.title.ifBlank { "제목 없음" },
                style = AppTextStyles.title_semibold_24,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            // ===== '영상 보러가기' Outlined 작은 버튼 =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = {
                        item.videoUrl.takeIf { it.isNotBlank() }?.let { url ->
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(text = "영상 보러가기", style = AppTextStyles.label_SemiBold_12, color = Color.Black)
                }
            }

            Spacer(Modifier.height(8.dp))

            // ===== 본문 섹션 =====
            DetailBodyMarkdown(
                title = "요약 노트",
                description = item.description
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * 카드 상세(제목 + 구분선 + Markdown)
 */
@Composable
private fun DetailBodyMarkdown(
    title: String = "요약 노트",
    description: String
) {
    // 섹션 타이틀
    Text(
        text = title,
        style = AppTextStyles.title_semibold_22,
        color = GreyMain300
    )
    HorizontalDivider(color = Grey30, thickness = 1.dp)

    // 마크다운 렌더링이 없으므로 일반 텍스트로 표시
//    Text(
//        text = description,
//        style = AppTextStyles.b2_regular_16,
//        lineHeight = 24.sp
//    )

    RichText {
        // 기본 타이포를 더 굵게/크게 쓰고 싶다면 아래 주석 해제
//        ProvideTextStyle() { Markdown(description) }
        Markdown(description)
    }
}

//프리뷰 카드 상세 아이템
data class CardPreviewDetailItem(
    val id: String,
    val title: String,
    val description: String,
    val videoUrl: String,
    val createdAt: String,
    val updatedAt: String
)

// 프리뷰용 더미 데이터
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CardDetailScreenPreview() {
    MaterialTheme {
        CardDetailScreen(
            item = CardDetailItem(
                id = 1,
                title = "Jetpack Compose 완벽 가이드",
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
