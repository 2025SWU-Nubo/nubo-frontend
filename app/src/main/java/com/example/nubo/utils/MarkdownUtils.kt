package com.example.nubo.utils

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.math.max
import kotlin.math.min

/* ============ Low-level helpers: 절대 문자열을 정규화하지 않음! ============ */

// 인덱스를 문자열 범위 내로 제한
private fun clamp(i: Int, len: Int) = i.coerceIn(0, len)

/**
 * 현재 인덱스가 포함된 라인의 시작(개행 직후) 인덱스를 반환
 */
private fun lineStart(md: String, index: Int): Int {
    if (md.isEmpty()) return 0
    val i = clamp(index, md.length)
    val prev = md.lastIndexOf('\n', (i - 1).coerceAtLeast(0))
    return if (prev == -1) 0 else prev + 1
}

/**
 * 현재 인덱스가 포함된 라인의 끝(개행 직전) 인덱스를 반환 (exclusive)
 */
private fun lineEnd(md: String, index: Int): Int {
    if (md.isEmpty()) return 0
    val i = clamp(index, md.length)
    val next = md.indexOf('\n', i)
    return if (next == -1) md.length else next
}

/**
 * 안전한 문자열 치환 (원문 길이·개행 그대로 유지)
 */
private fun replaceRangeSafe(src: String, start: Int, end: Int, with: String): String {
    val s = clamp(min(start, end), src.length)
    val e = clamp(max(start, end), src.length)
    return buildString(src.length - (e - s) + with.length) {
        append(src, 0, s)
        append(with)
        append(src, e, src.length)
    }
}

/* ============ 마크다운 상태 감지 ============ */

data class LineMarkdownState(
    val headingLevel: Int? = null,  // null, 2, 3
    val listType: ListType? = null   // ORDERED, UNORDERED, null
)

enum class ListType {
    ORDERED,    // 1. 2. 3.
    UNORDERED   // - * •
}

/**
 * 현재 커서가 위치한 라인의 마크다운 상태 감지
 */
fun detectLineMarkdown(state: RichTextState): LineMarkdownState {
    val md = state.toMarkdown()
    val caret = clamp(state.selection.end, md.length)

    val ls = lineStart(md, caret)
    val le = lineEnd(md, caret)
    val line = md.substring(ls, le).trimStart()

    // 헤딩 감지
    val headingLevel = when {
        line.startsWith("## ") -> 2
        line.startsWith("### ") -> 3
        else -> null
    }

    // 리스트 감지
    val listType = when {
        line.matches(Regex("^\\d+\\.\\s+.*")) -> ListType.ORDERED
        line.matches(Regex("^[-*•]\\s+.*")) -> ListType.UNORDERED
        else -> null
    }

    return LineMarkdownState(headingLevel, listType)
}

/* ============ Markdown toggles ============ */

/**
 * H2/H3 헤딩 토글 - 커서 위치 완벽 보존
 * 핵심: 실제 콘텐츠 내에서의 커서 위치를 기준으로 계산
 */
fun toggleHeadingMarkdown(state: RichTextState, level: Int /* 2 or 3 */) {
    val md = state.toMarkdown()
    val caret = clamp(state.selection.end, md.length)

    val ls = lineStart(md, caret)
    val le = lineEnd(md, caret)
    val line = md.substring(ls, le)

    // 현재 라인의 헤딩 레벨 및 마크다운 접두사 길이 확인
    val currentLevel: Int?
    val currentPrefixLength: Int

    when {
        line.trimStart().startsWith("### ") -> {
            currentLevel = 3
            currentPrefixLength = line.indexOf("### ") + 4  // "### " = 4글자
        }
        line.trimStart().startsWith("## ") -> {
            currentLevel = 2
            currentPrefixLength = line.indexOf("## ") + 3  // "## " = 3글자
        }
        else -> {
            currentLevel = null
            currentPrefixLength = 0
        }
    }

    // 새로운 헤딩 접두사
    val newPrefix = if (currentLevel == level) {
        // 같은 레벨이면 제거 (본문으로)
        ""
    } else {
        // 다른 레벨로 변경
        "#".repeat(level) + " "
    }

    val newPrefixLength = newPrefix.length

    // 헤딩 마크다운 제거한 순수 텍스트
    val pureContent = line.replace(Regex("^\\s*#{1,6}\\s+"), "")

    // 새로운 라인 구성
    val newLine = newPrefix + pureContent

    // 커서의 실제 콘텐츠 내 위치 계산
    // 커서가 마크다운 접두사보다 앞에 있으면 콘텐츠 시작으로, 아니면 상대 위치 유지
    val caretOffsetInLine = caret - ls
    val caretInContent = if (caretOffsetInLine <= currentPrefixLength) {
        0  // 접두사 영역에 있었으면 콘텐츠 시작으로
    } else {
        caretOffsetInLine - currentPrefixLength  // 콘텐츠 내 상대 위치
    }

    // 새로운 커서 위치 = 라인 시작 + 새 접두사 길이 + 콘텐츠 내 위치
    val newCaret = clamp(ls + newPrefixLength + caretInContent, md.length)

    // 마크다운 업데이트
    val newMd = replaceRangeSafe(md, ls, le, newLine)

    state.setMarkdown(newMd)
    state.selection = TextRange(newCaret)
}

/**
 * 현재 라인의 모든 헤딩 마크다운 제거 (본문으로 되돌리기)
 */
fun clearHeadingMarkdown(state: RichTextState) {
    val md = state.toMarkdown()
    val caret = clamp(state.selection.end, md.length)

    val ls = lineStart(md, caret)
    val le = lineEnd(md, caret)
    val line = md.substring(ls, le)

    // 헤딩 마크다운이 없으면 아무것도 안 함
    if (!line.trimStart().startsWith("#")) return

    // 현재 접두사 길이 계산
    val prefixMatch = Regex("^\\s*#{1,6}\\s+").find(line)
    val currentPrefixLength = prefixMatch?.value?.length ?: 0

    // 모든 헤딩 제거
    val pureContent = line.replace(Regex("^\\s*#{1,6}\\s+"), "")

    // 커서의 콘텐츠 내 위치
    val caretOffsetInLine = caret - ls
    val caretInContent = if (caretOffsetInLine <= currentPrefixLength) {
        0
    } else {
        caretOffsetInLine - currentPrefixLength
    }

    // 새로운 커서 위치 (접두사 없음)
    val newCaret = clamp(ls + caretInContent, md.length)

    val newMd = replaceRangeSafe(md, ls, le, pureContent)
    state.setMarkdown(newMd)
    state.selection = TextRange(newCaret)
}

/**
 * 리스트 토글: 선택과 교차하는 "전체 줄들"에 대해 동작
 * 커서 위치를 콘텐츠 기준으로 보존
 */
fun toggleListForSelection(state: RichTextState, ordered: Boolean) {
    val md = state.toMarkdown()
    val sel = state.selection

    val a = clamp(sel.start, md.length)
    val b = clamp(sel.end, md.length)

    val from = min(a, b)
    val to   = max(a, b)

    val start = lineStart(md, from)
    val end   = lineEnd(md, to)

    val block = md.substring(start, end)
    val lines = block.split('\n')

    // 커서가 있는 라인의 인덱스와 라인 내 위치 계산
    val caretLineStart = lineStart(md, to)
    val caretLine = lines.indexOfFirst { lineText ->
        val lineStartInBlock = block.indexOf(lineText)
        start + lineStartInBlock == caretLineStart
    }.coerceAtLeast(0)

    val caretOffsetInLine = to - caretLineStart

    // 현재 상태 확인
    val allOrdered = lines.all { line ->
        line.trim().isEmpty() || line.matches(Regex("^\\s*\\d+\\.\\s+.*"))
    }
    val allBulleted = lines.all { line ->
        line.trim().isEmpty() || line.matches(Regex("^\\s*[-*•]\\s+.*"))
    }

    // 현재 커서 라인의 접두사 길이 계산
    val currentLine = lines.getOrNull(caretLine) ?: ""
    val currentPrefixLength = when {
        currentLine.matches(Regex("^\\s*\\d+\\.\\s+.*")) -> {
            val match = Regex("^\\s*\\d+\\.\\s+").find(currentLine)
            match?.value?.length ?: 0
        }
        currentLine.matches(Regex("^\\s*[-*•]\\s+.*")) -> {
            val match = Regex("^\\s*[-*•]\\s+").find(currentLine)
            match?.value?.length ?: 0
        }
        else -> 0
    }

    val toggled = if (ordered) {
        if (allOrdered) {
            lines.map { it.replace(Regex("^\\s*\\d+\\.\\s+"), "") }
        } else {
            var n = 1
            lines.map { l ->
                if (l.trim().isEmpty()) {
                    l
                } else {
                    val norm = l
                        .replace(Regex("^\\s*[-*•]\\s+"), "")
                        .replace(Regex("^\\s*>\\s+"), "")
                        .replace(Regex("^\\s*\\d+\\.\\s+"), "")
                    "${n++}. $norm"
                }
            }
        }
    } else {
        if (allBulleted) {
            lines.map { it.replace(Regex("^\\s*[-*•]\\s+"), "") }
        } else {
            lines.map { l ->
                if (l.trim().isEmpty()) {
                    l
                } else {
                    val norm = l
                        .replace(Regex("^\\s*\\d+\\.\\s+"), "")
                        .replace(Regex("^\\s*>\\s+"), "")
                        .replace(Regex("^\\s*[-*•]\\s+"), "")
                    "- $norm"
                }
            }
        }
    }

    // 새로운 커서 라인의 접두사 길이 계산
    val newLine = toggled.getOrNull(caretLine) ?: ""
    val newPrefixLength = when {
        newLine.matches(Regex("^\\s*\\d+\\.\\s+.*")) -> {
            val match = Regex("^\\s*\\d+\\.\\s+").find(newLine)
            match?.value?.length ?: 0
        }
        newLine.matches(Regex("^\\s*[-*•]\\s+.*")) -> {
            val match = Regex("^\\s*[-*•]\\s+").find(newLine)
            match?.value?.length ?: 0
        }
        else -> 0
    }

    // 콘텐츠 내 커서 위치 계산
    val caretInContent = if (caretOffsetInLine <= currentPrefixLength) {
        0
    } else {
        caretOffsetInLine - currentPrefixLength
    }

    val replaced = toggled.joinToString("\n")
    val newMd = replaceRangeSafe(md, start, end, replaced)
    state.setMarkdown(newMd)

    // 새로운 커서 라인의 시작 위치 계산
    val newCaretLineStart = start + toggled.take(caretLine).sumOf { it.length + 1 }
    val newCaret = clamp(newCaretLineStart + newPrefixLength + caretInContent, newMd.length)

    state.selection = TextRange(newCaret)
}

/**
 * 저장 시에만 sanitize. 편집 중엔 절대 normalize/치환 금지
 */
fun sanitizeToAllowedMarkdown(md: String): String {
    return md
        .replace(Regex("(?m)^#\\s+"), "")
        .replace(Regex("(?m)^#{4,6}\\s+"), "")
        .replace(Regex("(?m)^>\\s+"), "")
        .replace(Regex("(?s)```.*?```"), "")
        .replace(Regex("!\\[[^\\]]*]\\([^)]*\\)"), "")
        .replace(Regex("\\[([^\\]]+)]\\([^)]*\\)"), "$1")
        .replace(Regex("`([^`]*)`"), "$1")
        .replace(Regex("(?<!\\*)\\*(?!\\*)([^*]+)(?<!\\*)\\*(?!\\*)"), "$1")
        .replace(Regex("_(.+?)_"), "$1")
        .trim()
}
