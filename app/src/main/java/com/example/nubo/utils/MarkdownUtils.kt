package com.example.nubo.utils

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.math.max
import kotlin.math.min

/* ============ Low-level helpers: 절대 문자열을 정규화하지 않음! ============ */

// clamp
private fun clamp(i: Int, len: Int) = i.coerceIn(0, len)

// 현재 인덱스가 포함된 라인의 시작(개행 직후) 인덱스
private fun lineStart(md: String, index: Int): Int {
    if (md.isEmpty()) return 0
    val i = clamp(index, md.length)
    val prev = md.lastIndexOf('\n', (i - 1).coerceAtLeast(0))
    return if (prev == -1) 0 else prev + 1
}

// 현재 인덱스가 포함된 라인의 끝(개행 직전) 인덱스(exclusive)
// CRLF를 사용하는 경우에도 문자열은 그대로 유지(치환 X), '\n'까지만 찾음
private fun lineEnd(md: String, index: Int): Int {
    if (md.isEmpty()) return 0
    val i = clamp(index, md.length)
    val next = md.indexOf('\n', i)
    return if (next == -1) md.length else next
}

// 안전 치환(원문 길이·개행 그대로 유지)
private fun replaceRangeSafe(src: String, start: Int, end: Int, with: String): String {
    val s = clamp(min(start, end), src.length)
    val e = clamp(max(start, end), src.length)
    return buildString(src.length - (e - s) + with.length) {
        append(src, 0, s)
        append(with)
        append(src, e, src.length)
    }
}

/* ============ Markdown toggles ============ */

/** H2/H3: 커서가 위치한 "현재 줄"만 토글 (선택 무시) */
fun toggleHeadingMarkdown(state: RichTextState, level: Int /* 2 or 3 */) {
    val md = state.toMarkdown()
    val caret = clamp(state.selection.start, md.length)

    val ls = lineStart(md, caret)
    val le = lineEnd(md, caret)
    val line = md.substring(ls, le)

    val want = "#".repeat(level) + " "
    // 기존 어떤 헤딩이든 제거(문자열은 그대로 유지: 치환 없음, 앞부분만 정규식)
    val normalized = line.replace(Regex("^\\s*#{1,6}\\s+"), "")

    val toggled = if (line.startsWith(want)) {
        normalized
    } else {
        want + normalized
    }

    val newMd = replaceRangeSafe(md, ls, le, toggled)
    state.setMarkdown(newMd)

    // 커서는 해당 줄 끝으로
    val newCaret = clamp(ls + toggled.length, newMd.length)
    state.selection = TextRange(newCaret)
}

/** Bold: 선택된 구간만 **…** 로 토글 (선택 없으면 아무 것도 안 함) */
fun toggleBoldMarkdown(state: RichTextState) {
    val md = state.toMarkdown()
    val sel = state.selection

    val a = clamp(sel.start, md.length)
    val b = clamp(sel.end, md.length)
    if (a == b) return

    val s = min(a, b)
    val e = max(a, b)

    val target = md.substring(s, e)
    val wrapper = "**"

    // 선택 내부가 이미 **텍스트** 형태인지 검사
    val already = target.length >= 4 && target.startsWith(wrapper) && target.endsWith(wrapper)
    val newSeg = if (already) target.removePrefix(wrapper).removeSuffix(wrapper)
    else "$wrapper$target$wrapper"

    val newMd = replaceRangeSafe(md, s, e, newSeg)
    state.setMarkdown(newMd)

    // 커서는 치환된 영역 끝으로
    state.selection = TextRange(s + newSeg.length)
}

/** 리스트 토글: 선택과 교차하는 "전체 줄들"에 대해 동작 (혼합 상태도 일관 처리) */
fun toggleListForSelection(state: RichTextState, ordered: Boolean) {
    val md = state.toMarkdown()
    val sel = state.selection

    val a = clamp(sel.start, md.length)
    val b = clamp(sel.end, md.length)

    val from = min(a, b)
    val to   = max(a, b)

    val start = lineStart(md, from)
    val end   = lineEnd(md, to) // 마지막 포함 줄의 끝까지

    val block = md.substring(start, end)
    val lines = block.split('\n')

    val allOrdered = lines.all { it.matches(Regex("^\\s*\\d+\\.\\s+.*")) }
    val allBulleted = lines.all { it.matches(Regex("^\\s*[-*•]\\s+.*")) }

    val toggled = if (ordered) {
        if (allOrdered) {
            lines.map { it.replace(Regex("^\\s*\\d+\\.\\s+"), "") }
        } else {
            var n = 1
            lines.map { l ->
                val norm = l
                    .replace(Regex("^\\s*[-*•]\\s+"), "")
                    .replace(Regex("^\\s*>\\s+"), "")
                    .replace(Regex("^\\s*\\d+\\.\\s+"), "")
                "${n++}. $norm"
            }
        }
    } else {
        if (allBulleted) {
            lines.map { it.replace(Regex("^\\s*[-*•]\\s+"), "") }
        } else {
            lines.map { l ->
                val norm = l
                    .replace(Regex("^\\s*\\d+\\.\\s+"), "")
                    .replace(Regex("^\\s*>\\s+"), "")
                    .replace(Regex("^\\s*[-*•]\\s+"), "")
                "- $norm"
            }
        }
    }

    val replaced = toggled.joinToString("\n")
    val newMd = replaceRangeSafe(md, start, end, replaced)
    state.setMarkdown(newMd)

    // 커서를 블록 끝으로(원하는 줄이 밀리는 느낌 최소화)
    val newCaret = start + replaced.length
    state.selection = TextRange(newCaret)
}

/** 저장 시에만 sanitize. 편집 중엔 절대 normalize/치환 금지 */
fun sanitizeToAllowedMarkdown(md: String): String {
    return md
        .replace(Regex("(?m)^#\\s+"), "")          // H1 제거
        .replace(Regex("(?m)^#{4,6}\\s+"), "")     // H4~H6 제거
        .replace(Regex("(?m)^>\\s+"), "")          // 인용 제거
        .replace(Regex("(?s)```.*?```"), "")       // 코드펜스 제거
        .replace(Regex("!\\[[^\\]]*]\\([^)]*\\)"), "") // 이미지 제거
        .replace(Regex("\\[([^\\]]+)]\\([^)]*\\)"), "$1") // 링크 → 텍스트
        .replace(Regex("`([^`]*)`"), "$1")         // 인라인 코드 제거
        // *italic* / _italic_ 제거 (bold는 유지)
        .replace(Regex("(?<!\\*)\\*(?!\\*)([^*]+)(?<!\\*)\\*(?!\\*)"), "$1")
        .replace(Regex("_(.+?)_"), "$1")
        .trim()
}
