package com.example.nubo.ui.screen.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nubo.R
import com.example.nubo.data.dto.HighlightDto
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.utils.sanitizeToAllowedMarkdown
import com.example.nubo.utils.toggleBoldMarkdown
import com.example.nubo.utils.toggleHeadingMarkdown
import com.example.nubo.utils.toggleListForSelection
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.commonmark.Markdown
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditCardScreen(
    summary: String,
    highlights: List<HighlightDto>,
    onBack: () -> Unit,
    onSummaryChange: (String) -> Unit,
    onToggleHighlight: (start: Int, end: Int) -> Unit,
    onSave: () -> Unit
) {
    val rtState = rememberRichTextState()

    // --- IME & focus states ---
    val imeVisible = WindowInsets.isImeVisible
    var editorFocused by remember { mutableStateOf(false) }

    LaunchedEffect(summary) {
        rtState.setMarkdown(sanitizeToAllowedMarkdown(summary))
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("요약 노트") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val md = sanitizeToAllowedMarkdown(rtState.toMarkdown())
                        onSummaryChange(md)
                        onSave()
                    }) {
                        Text(text = "완료", style = AppTextStyles.b1_bold_18, color = PurpleMain500)
                    }
                }
            )
        }
    ) { innerPadding ->

        val scroll = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scroll)
                .imePadding(),              // 키보드가 올라올 때 안전
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- Toolbar (허용 5개만) ----
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .horizontalScroll(rememberScrollState()),
//                horizontalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                OutlinedButton(onClick = { toggleHeadingMarkdown(rtState, 2) }) { Text("H2") }
//                OutlinedButton(onClick = { toggleHeadingMarkdown(rtState, 3) }) { Text("H3") }
//                FilledTonalButton(onClick = { toggleBoldMarkdown(rtState) }) { Text("B") }
//                OutlinedButton(onClick = { toggleListForSelection(rtState, ordered = false) }) { Text("• list") }
//                OutlinedButton(onClick = { toggleListForSelection(rtState, ordered = true) }) { Text("1. list") }
//            }

            // --- Editor ---
            RichTextEditor(
                state = rtState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp)
                    .onKeyEvent { false }
                    .onFocusChanged { editorFocused = it.isFocused } // track focus
            )

            Divider()

            // ---- Live Preview ----
            val liveMarkdown by remember(rtState) { derivedStateOf { rtState.toMarkdown() } }
            val liveSafe = remember(liveMarkdown) { sanitizeToAllowedMarkdown(liveMarkdown) }

            RichText { Markdown(liveSafe) }
        }

        // -------- IME 위 액세서리 바 --------
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedVisibility(
                visible = imeVisible && editorFocused,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                // Surface is drawn "on top of" content, sitting exactly above keyboard.
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 6.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier
                        // push the bar up exactly by the IME height so it sticks to the keyboard top
                        .imePadding()
                        // avoid gesture nav bar overlap on devices without 3-button nav
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // --- your markdown actions (same handlers 그대로 재사용) ---
                        AssistChip(onClick = { toggleHeadingMarkdown(rtState, 2) }, label = { Text("H2") })
                        AssistChip(onClick = { toggleHeadingMarkdown(rtState, 3) }, label = { Text("H3") })
                        FilterChip(selected = false, onClick = { toggleBoldMarkdown(rtState) }, label = { Text("B") })
                        AssistChip(onClick = { toggleListForSelection(rtState, ordered = false) }, label = { Text("• list") })
                        AssistChip(onClick = { toggleListForSelection(rtState, ordered = true) }, label = { Text("1. list") })
                        // 필요하면 코드, 인용, 체크리스트 등 추가
                    }
                }
            }

        }


    }
}

