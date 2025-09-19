package com.example.nubo.ui.screen.card

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.nubo.R
import com.example.nubo.data.dto.HighlightDto


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCardScreen(
    summary: String,
    highlights: List<HighlightDto>,
    onBack: () -> Unit,
    onSummaryChange: (String) -> Unit,
    onToggleHighlight: (start: Int, end: Int) -> Unit,
    onSave: () -> Unit
) {
    var tfv by remember(summary) { mutableStateOf(TextFieldValue(summary)) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("카드 수정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onSave) { Text("저장") }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- Editor
            OutlinedTextField(
                value = tfv,
                onValueChange = {
                    tfv = it
                    onSummaryChange(it.text)
                },
                label = { Text("요약 (Markdown 포함)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                maxLines = 12
            )

            // --- Highlight toggle by current selection
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    onToggleHighlight(tfv.selection.start, tfv.selection.end)
                }) { Text("형광펜 토글") }
            }

            Divider()

            // --- Live preview (simple text with background spans)
            Text(
                text = buildHighlighted(summary = tfv.text, highlights = highlights),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Build an AnnotatedString with background spans for the given ranges
@Composable
private fun buildHighlighted(
    summary: String,
    highlights: List<HighlightDto>
): AnnotatedString {
    // NOTE: This preview does not render Markdown; it focuses on showing highlight ranges.
    val builder = AnnotatedString.Builder()
    var idx = 0
    while (idx < summary.length) {
        val mark = highlights.firstOrNull { it.rangeStart == idx }
        if (mark != null) {
            val end = mark.rangeEnd.coerceAtMost(summary.length)
            builder.withStyle(
                SpanStyle(background = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
            ) {
                append(summary.substring(idx, end))
            }
            idx = end
        } else {
            // append until next highlight start or end of text
            val nextStart = highlights.map { it.rangeStart }.filter { it > idx }.minOrNull() ?: summary.length
            builder.append(summary.substring(idx, nextStart))
            idx = nextStart
        }
    }
    return builder.toAnnotatedString()
}
