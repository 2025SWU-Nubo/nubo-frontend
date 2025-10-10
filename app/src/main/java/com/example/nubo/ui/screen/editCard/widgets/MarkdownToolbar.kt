package com.example.nubo.ui.screen.editCard.widgets

import androidx.compose.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import com.mohamedrejeb.richeditor.model.RichTextState
import com.example.nubo.utils.toggleHeadingMarkdown
import com.example.nubo.ui.theme.Grey5
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import com.example.nubo.ui.theme.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.nubo.R

@Composable
fun MarkdownToolbar(
    rtState: RichTextState,
    editorFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {

    // 현재 선택된 헤딩 레벨 추적
    var selectedHeading by remember { mutableStateOf<Int?>(null) }

    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier,
        color = Grey5
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 포커스 이동 후 액션 실행 헬퍼 함수
            fun focusThen(action: () -> Unit) {
                editorFocusRequester.requestFocus()
                action()
            }

            // 제목 버튼 (H1 또는 대제목)
            HeadingButton(
                text = "제목",
                isSelected = selectedHeading == 2,
                onClick = {
                    focusThen {
                        toggleHeadingMarkdown(rtState, 2)
                        selectedHeading = if (selectedHeading == 2) null else 1
                    }
                },
                textSize = AppTextStyles.title_semibold_24
            )

            // 부제목 버튼 (H2)
            HeadingButton(
                text = "부제목",
                isSelected = selectedHeading == 3,
                onClick = {
                    focusThen {
                        toggleHeadingMarkdown(rtState, 3)
                        selectedHeading = if (selectedHeading == 3) null else 2
                    }
                },
                textSize = AppTextStyles.subtitle_semibold_20
            )

            // 본문 버튼 (일반 텍스트)
            HeadingButton(
                text = "본문",
                isSelected = selectedHeading == null,
                onClick = {
                    focusThen {
                        // 모든 헤딩 해제
                        selectedHeading = null
                    }
                },
                textSize = AppTextStyles.b2_medium_16
            )

            // 굵게 버튼 (Bold)
            FilterChip(
                selected = false,
                onClick = {
                    focusThen {
                        rtState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    }
                },
                label = {
                    Text(
                        "B",
                        style = AppTextStyles.title_semibold_24
                    )
                }
            )

            // 순서 없는 리스트 버튼
            IconButton(
                onClick = { focusThen { rtState.toggleUnorderedList() } },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.list_toggles), // ← ← 아이콘 파일 필요
                    tint = GreyMain300,
                    contentDescription = "순서 없는 리스트",
                )
            }

            // 순서 있는 리스트 버튼
            IconButton(
                onClick = { focusThen { rtState.toggleOrderedList() } },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.list_numbers), // ← ← 아이콘 파일 필요
                    tint = GreyMain300,
                    contentDescription = "순서 있는 리스트",
                )
            }
//
//            AssistChip(onClick = { focusThen { toggleHeadingMarkdown(rtState, 2) } }, label = { Text("H2", style = MaterialTheme.typography.titleMedium) })
//            AssistChip(onClick = { focusThen { toggleHeadingMarkdown(rtState, 3) } }, label = { Text("H3", style = MaterialTheme.typography.titleSmall) })
//            FilterChip(selected = false, onClick = { focusThen { rtState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) } }, label = { Text("B", fontWeight = FontWeight.Bold) })
//            AssistChip(onClick = { focusThen { rtState.toggleUnorderedList() } }, label = { Text("• 리스트", style = MaterialTheme.typography.labelMedium) })
//            AssistChip(onClick = { focusThen { rtState.toggleOrderedList() } }, label = { Text("1 리스트", style = MaterialTheme.typography.labelMedium) })
        }
    }
}

/**
 * 헤딩 버튼 컴포저블
 * 제목, 부제목, 본문 버튼에 사용
 */
@Composable
private fun HeadingButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textSize: TextStyle
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) PurpleMain500 // 보라색
                else Color.White
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Black,
            style = textSize
        )
    }
}

