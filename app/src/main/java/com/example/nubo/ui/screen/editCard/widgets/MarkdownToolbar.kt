package com.example.nubo.ui.screen.editCard.widgets

import androidx.compose.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import com.mohamedrejeb.richeditor.model.RichTextState
import com.example.nubo.utils.toggleHeadingMarkdown
import com.example.nubo.utils.clearHeadingMarkdown
import com.example.nubo.utils.detectLineMarkdown
import com.example.nubo.utils.ListType
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
import com.example.nubo.R
import com.example.nubo.utils.toggleListForSelection

private val CHIP_HEIGHT = 45.dp
private val CHIP_LONG_WIDTH = 75.dp
private val CHIP_SHORT_WIDTH = 55.dp

@Composable
fun MarkdownToolbar(
    rtState: RichTextState,
    editorFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    // 현재 라인의 마크다운 상태 추적
    var currentHeadingLevel by remember { mutableStateOf<Int?>(null) }
    var currentListType by remember { mutableStateOf<ListType?>(null) }
    var isBoldSelected by remember { mutableStateOf(false) }
    var hasSelection by remember { mutableStateOf(false) }

    // 포커스 이동 후 액션 실행 헬퍼 함수
    fun focusThen(action: () -> Unit) {
        editorFocusRequester.requestFocus()
        action()
    }

    // 커서 위치나 텍스트 변경 시 현재 마크다운 상태 감지
    LaunchedEffect(rtState.annotatedString, rtState.selection) {
        val state = detectLineMarkdown(rtState)
        currentHeadingLevel = state.headingLevel
        currentListType = state.listType

        // 텍스트 선택 여부 확인
        hasSelection = rtState.selection.start != rtState.selection.end

        // Bold 상태 감지
        isBoldSelected = (rtState.currentSpanStyle.fontWeight == FontWeight.Bold)
    }

    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier,
        color = Grey5
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 제목 버튼 (H2)
            HeadingButton(
                text = "제목",
                isSelected = currentHeadingLevel == 2,
                onClick = {
                    focusThen {
                        toggleHeadingMarkdown(rtState, 2)
                    }
                },
                textSize = AppTextStyles.title_semibold_24
            )

            // 부제목 버튼 (H3)
            HeadingButton(
                text = "부제목",
                isSelected = currentHeadingLevel == 3,
                onClick = {
                    focusThen {
                        toggleHeadingMarkdown(rtState, 3)
                    }
                },
                textSize = AppTextStyles.subtitle_semibold_20
            )

            // 본문 버튼
            HeadingButton(
                text = "본문",
                isSelected = currentHeadingLevel == null && currentListType == null,
                onClick = {
                    focusThen {
                        clearHeadingMarkdown(rtState)
                    }
                },
                textSize = AppTextStyles.b2_medium_16
            )

            val BOLD_STYLE = remember { SpanStyle(fontWeight = FontWeight.Bold) }

            // 굵게 버튼 (Bold) - 선택 여부에 따라 다르게 동작
            FilterChip(
                modifier = Modifier.height(CHIP_HEIGHT),
                selected = isBoldSelected,
                onClick = {
                    focusThen {
                        if (hasSelection) {
                            // 텍스트가 선택되어 있으면 선택된 텍스트만 굵게
                            rtState.toggleSpanStyle(BOLD_STYLE)
                        } else {
                            // 선택이 없으면 현재 커서 위치부터 입력되는 텍스트에 적용
                            rtState.toggleSpanStyle(BOLD_STYLE)
                        }
                        // 즉시 UI 반영
                        isBoldSelected = !isBoldSelected
                    }
                },
                label = {
                    Text(
                        "B",
                        style = AppTextStyles.title_semibold_24
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PurpleMain500,
                    selectedLabelColor = Color.White,
                    containerColor = Grey20,
                    labelColor = GreyMain300
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isBoldSelected,
                    borderColor = Color.Transparent,
                    selectedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    disabledSelectedBorderColor = Color.Transparent,
                    borderWidth = 0.dp
                )
            )

            // 순서 없는 리스트 버튼
            FilledTonalIconButton(
                onClick = {
                    focusThen {
                        toggleListForSelection(rtState, ordered = false)
                    }
                },
                modifier = Modifier.height(CHIP_HEIGHT).width(CHIP_SHORT_WIDTH),
                shape = RoundedCornerShape(8.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (currentListType == ListType.UNORDERED)
                        PurpleMain500 else Purple50,
                    contentColor = if (currentListType == ListType.UNORDERED)
                        Color.White else GreyMain300
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.list_toggles),
                    contentDescription = "순서 없는 리스트",
                )
            }

            // 순서 있는 리스트 버튼
            FilledTonalIconButton(
                onClick = {
                    focusThen {
                        toggleListForSelection(rtState, ordered = true)
                    }
                },
                modifier = Modifier.height(CHIP_HEIGHT).width(CHIP_SHORT_WIDTH),
                shape = RoundedCornerShape(8.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (currentListType == ListType.ORDERED)
                        PurpleMain500 else Purple50,
                    contentColor = if (currentListType == ListType.ORDERED)
                        Color.White else GreyMain300
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.list_numbers),
                    contentDescription = "순서 있는 리스트",
                )
            }
        }
    }
}

/**
 * 헤딩 버튼 컴포저블
 */
@Composable
private fun HeadingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textSize: TextStyle,
    isSelected: Boolean = false
) {
    Box(
        modifier = modifier
            .size(CHIP_LONG_WIDTH, CHIP_HEIGHT)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) PurpleMain500 else Grey10
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Black,
            style = textSize
        )
    }
}
