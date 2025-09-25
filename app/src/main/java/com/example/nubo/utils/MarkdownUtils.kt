package com.example.nubo.utils

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.math.max
import kotlin.math.min

// NOTE: Comments are in English only.

// ---- Helpers ----
private fun mdLineStart(md: String, caret: Int): Int {
    if (md.isEmpty()) return 0
    val idx = (caret - 1).coerceAtLeast(0)
    val prevNewline = md.lastIndexOf('\n', idx)
    return if (prevNewline == -1) 0 else prevNewline + 1
}

// ---- 1) Heading (H2/H3) ----
fun toggleHeadingMarkdown(state: RichTextState, level: Int /* 2 or 3 */) {
    val md = state.toMarkdown()
    val caret = state.selection.start.coerceIn(0, md.length)
    val start = mdLineStart(md, caret)
    val end = md.indexOf('\n', start).let { if (it == -1) md.length else it }
    val line = md.substring(start, end)

    val want = "#".repeat(level) + " "
    val normalized = line.replace(Regex("^#{1,6}\\s+"), "")
    val toggled = if (line.startsWith(want)) normalized else want + normalized

    val newMd = md.replaceRange(start, end, toggled)
    state.setMarkdown(newMd)
    state.selection = TextRange((start + toggled.length).coerceAtMost(newMd.length))
}

// ---- 2) Bold (** **) ----
fun toggleBoldMarkdown(state: RichTextState) {
    val md = state.toMarkdown()
    val sel = state.selection
    val a = sel.start.coerceIn(0, md.length)
    val b = sel.end.coerceIn(0, md.length)
    if (a == b) return

    val start = min(a, b)
    val end = max(a, b)
    val target = md.substring(start, end)
    val wrapper = "**"

    val already = target.startsWith(wrapper) && target.endsWith(wrapper) && target.length >= 4
    val newSegment = if (already) {
        target.removePrefix(wrapper).removeSuffix(wrapper)
    } else {
        "$wrapper$target$wrapper"
    }

    val newMd = md.replaceRange(start, end, newSegment)
    state.setMarkdown(newMd)
    state.selection = TextRange(start + newSegment.length)
}

// ---- 3) Bullet list (- ) ----
// ---- 4) Numbered list (1. ) ----
fun toggleListForSelection(state: RichTextState, ordered: Boolean) {
    val md = state.toMarkdown()
    val sel = state.selection
    val a = sel.start.coerceIn(0, md.length)
    val b = sel.end.coerceIn(0, md.length)
    val startLine = mdLineStart(md, min(a, b))
    val endIdx = max(a, b)
    val endLine = md.indexOf('\n', endIdx).let { if (it == -1) md.length else it }

    val block = md.substring(startLine, endLine)
    val lines = block.split('\n')

    val toggled = if (ordered) {
        val alreadyOrdered = lines.all { it.matches(Regex("^\\s*\\d+\\.\\s+.*")) }
        if (alreadyOrdered) {
            lines.map { it.replace(Regex("^\\s*\\d+\\.\\s+"), "") }
        } else {
            lines.mapIndexed { i, l ->
                val normalized = l
                    .replace(Regex("^\\s*[-*•]\\s+"), "")
                    .replace(Regex("^\\s*>\\s+"), "")
                "${i + 1}. $normalized"
            }
        }
    } else {
        val alreadyBulleted = lines.all { it.matches(Regex("^\\s*[-*•]\\s+.*")) }
        if (alreadyBulleted) {
            lines.map { it.replace(Regex("^\\s*[-*•]\\s+"), "") }
        } else {
            lines.map { l ->
                val normalized = l
                    .replace(Regex("^\\s*\\d+\\.\\s+"), "")
                    .replace(Regex("^\\s*>\\s+"), "")
                "- $normalized"
            }
        }
    }

    val newMd = md.replaceRange(startLine, endLine, toggled.joinToString("\n"))
    state.setMarkdown(newMd)
    state.selection = TextRange(startLine, startLine)
}

// ---- (Optional) Sanitize to allowed features ----
fun sanitizeToAllowedMarkdown(md: String): String {
    return md
        // Remove H1/H4~H6
        .replace(Regex("(?m)^#\\s+"), "")
        .replace(Regex("(?m)^#{4,6}\\s+"), "")
        // Remove block quotes, code fences, images
        .replace(Regex("(?m)^>\\s+"), "")
        .replace(Regex("(?s)```.*?```"), "")
        .replace(Regex("!\\[[^\\]]*]\\([^)]*\\)"), "")
        // Flatten links [text](url) -> text
        .replace(Regex("\\[([^\\]]+)]\\([^)]*\\)"), "$1")
        // Remove inline code `...` and italic *...*/_..._
        .replace(Regex("`([^`]*)`"), "$1")
        .replace(Regex("(?<!\\*)\\*(?!\\*)([^*]+)(?<!\\*)\\*(?!\\*)"), "$1")
        .replace(Regex("_(.+?)_"), "$1")
        .trim()
}
