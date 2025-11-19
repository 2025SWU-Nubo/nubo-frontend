package com.example.nubo.ui.screen.editCard.widgets

import android.util.Log
import androidx.compose.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nubo.R
import com.example.nubo.ui.theme.*
import com.mohamedrejeb.richeditor.model.RichTextState
import com.example.nubo.utils.*
import kotlinx.coroutines.delay

private val CHIP_HEIGHT = 45.dp
private val CHIP_LONG_WIDTH = 75.dp
private val CHIP_SHORT_WIDTH = 55.dp

@Composable
fun MarkdownToolbar(
    rtState: RichTextState,
    editorFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    var currentHeadingLevel by remember { mutableStateOf<Int?>(null) }
    var currentListType by remember { mutableStateOf<ListType?>(null) }
    var isBoldSelected by remember { mutableStateOf(false) }
    var hasSelection by remember { mutableStateOf(false) }

    LaunchedEffect(rtState.annotatedString, rtState.selection) {
        val state = detectLineMarkdown(rtState)
        currentHeadingLevel = state.headingLevel
        currentListType = state.listType
        hasSelection = rtState.selection.start != rtState.selection.end
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
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 제목(H2)
            HeadingButton(
                text = "제목",
                isSelected = currentHeadingLevel == 2,
                onClick = {
                    editorFocusRequester.requestFocus()
                    val pos = toggleHeadingMarkdown(rtState, 2)
                    rtState.selection = TextRange(pos)
                    Log.d("Toolbar", "H2 clicked caret=$pos")
                },
                textSize = AppTextStyles.subtitle_semibold_20
            )

            // 부제목(H3)
            HeadingButton(
                text = "부제목",
                isSelected = currentHeadingLevel == 3,
                onClick = {
                    editorFocusRequester.requestFocus()
                    val pos = toggleHeadingMarkdown(rtState, 3)
                    rtState.selection = TextRange(pos)
                    Log.d("Toolbar", "H3 clicked caret=$pos")
                },
                textSize = AppTextStyles.b2_semibold_16
            )

            // 본문
            HeadingButton(
                text = "본문",
                isSelected = currentHeadingLevel == null && currentListType == null,
                onClick = {
                    editorFocusRequester.requestFocus()
                    val pos = clearHeadingMarkdown(rtState)
                    rtState.selection = TextRange(pos)
                    Log.d("Toolbar", "Clear heading caret=$pos")
                },
                textSize = AppTextStyles.label_medium_14
            )

            // 굵게(B) — 표시상 허용, 저장 시 서버 정규화에서 제거됨(텍스트만 남음)
            val BOLD_STYLE = remember { SpanStyle(fontWeight = FontWeight.Bold) }
            FilterChip(
                modifier = Modifier.height(CHIP_HEIGHT),
                selected = isBoldSelected,
                onClick = {
                    editorFocusRequester.requestFocus()
                    rtState.toggleSpanStyle(BOLD_STYLE)
                    isBoldSelected = !isBoldSelected
                },
                label = {
                    Text("B", style = AppTextStyles.b1_semibold_18)
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

            // 순서 없는 리스트 (- )
            FilledTonalIconButton(
                onClick = {
                    editorFocusRequester.requestFocus()

                    val before = rtState.selection
                    val beforeMd = rtState.toMarkdown()
                    Log.d("CursorDebug", "Before toggle list (ordered=false): start=${before.start}, end=${before.end}, len=${beforeMd.length}")

                    toggleListForSelection(rtState, ordered = false)

                    val after = rtState.selection
                    val afterMd = rtState.toMarkdown()
                    Log.d("CursorDebug", "After toggle list: start=${after.start}, end=${after.end}, len=${afterMd.length}")
                },
                modifier = Modifier.height(CHIP_HEIGHT).width(CHIP_SHORT_WIDTH),
                shape = RoundedCornerShape(8.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (currentListType == ListType.UNORDERED) PurpleMain500 else Purple50,
                    contentColor = if (currentListType == ListType.UNORDERED) Color.White else GreyMain300
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.list_toggles),
                    contentDescription = "순서 없는 리스트",
                )
            }

            // 순서 있는 리스트 (1. )
            FilledTonalIconButton(
                onClick = {
                    editorFocusRequester.requestFocus()
                    val before = rtState.selection
                    val beforeMd = rtState.toMarkdown()
                    Log.d("CursorDebug", "Before toggle list (ordered=true): start=${before.start}, end=${before.end}, len=${beforeMd.length}")

                    toggleListForSelection(rtState, ordered = true)

                    val after = rtState.selection
                    val afterMd = rtState.toMarkdown()
                    Log.d("CursorDebug", "After toggle list: start=${after.start}, end=${after.end}, len=${afterMd.length}")
                },
                modifier = Modifier.height(CHIP_HEIGHT).width(CHIP_SHORT_WIDTH),
                shape = RoundedCornerShape(8.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (currentListType == ListType.ORDERED) PurpleMain500 else Purple50,
                    contentColor = if (currentListType == ListType.ORDERED) Color.White else GreyMain300
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
            .background(if (isSelected) PurpleMain500 else Grey10)
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
