package com.example.nubo.ui.screen.editCard.widgets

import androidx.compose.*
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun MarkdownToolbar(
    rtState: RichTextState,
    editorFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier,
        color = Grey5
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            fun focusThen(action: () -> Unit) { editorFocusRequester.requestFocus(); action() }

            AssistChip(onClick = { focusThen { toggleHeadingMarkdown(rtState, 2) } }, label = { Text("H2", style = MaterialTheme.typography.titleMedium) })
            AssistChip(onClick = { focusThen { toggleHeadingMarkdown(rtState, 3) } }, label = { Text("H3", style = MaterialTheme.typography.titleSmall) })
            FilterChip(selected = false, onClick = { focusThen { rtState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) } }, label = { Text("B", fontWeight = FontWeight.Bold) })
            AssistChip(onClick = { focusThen { rtState.toggleUnorderedList() } }, label = { Text("• 리스트", style = MaterialTheme.typography.labelMedium) })
            AssistChip(onClick = { focusThen { rtState.toggleOrderedList() } }, label = { Text("1 리스트", style = MaterialTheme.typography.labelMedium) })
        }
    }
}

