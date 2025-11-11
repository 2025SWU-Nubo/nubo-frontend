package com.example.nubo.utils

import android.util.Log
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.math.max
import kotlin.math.min

/* =========================================
 * 공통 상수/유틸
 * ========================================= */

private const val TAG = "MarkdownUtils"
private const val INDENT_UNIT = 2 // 들여쓰기 2칸 고정

// 인덱스를 문자열 길이 범위 내로 제한
private fun clamp(i: Int, len: Int) = i.coerceIn(0, len)

// 개행 정규화: \r\n, \r → \n
private fun normalizeNewLines(s: String) = s.replace("\r\n", "\n").replace("\r", "\n")

// 탭 → 공백 2칸
private fun tabsToSpaces(s: String) = s.replace("\t", "  ")

/* =========================================
 * 디스플레이 텍스트(annotatedString.text) 라인 탐색
 * ========================================= */

// 디스플레이 텍스트에서 현재 커서가 포함된 라인의 시작 인덱스
private fun lineStartInAnnotated(text: String, index: Int): Int {
    if (text.isEmpty()) return 0
    val i = clamp(index, text.length)
    val prev = text.lastIndexOf('\n', (i - 1).coerceAtLeast(0))
    return if (prev == -1) 0 else prev + 1
}

// 디스플레이 텍스트에서 현재 커서가 포함된 라인의 끝 인덱스
private fun lineEndInAnnotated(text: String, index: Int): Int {
    if (text.isEmpty()) return 0
    val i = clamp(index, text.length)
    val next = text.indexOf('\n', i)
    return if (next == -1) text.length else next
}

/* =========================================
 * 안전한 문자열 교체
 * ========================================= */

private fun replaceRangeSafe(src: String, start: Int, end: Int, with: String): String {
    val s = clamp(min(start, end), src.length)
    val e = clamp(max(start, end), src.length)
    return buildString(src.length - (e - s) + with.length) {
        append(src, 0, s)
        append(with)
        append(src, e, src.length)
    }
}

/* =========================================
 * 상태/타입
 * ========================================= */

data class LineMarkdownState(
    val headingLevel: Int? = null,
    val listType: ListType? = null
)

enum class ListType {
    ORDERED,   // 1. 2. 3. (서버 규칙상 실제 저장은 항상 "1. ")
    UNORDERED  // - (서버 규칙상 불릿은 항상 "- ")
}

/* =========================================
 * 라인 매핑 구조
 * ========================================= */

private data class LineMapping(
    // 마크다운 기준
    val mdLineStart: Int,
    val mdLineEnd: Int,
    val mdLine: String,
    val matchedLineIndex: Int,
    // 디스플레이 기준
    val displayLineStart: Int,
    val displayLineEnd: Int,
    val selectedSegmentText: String,
    val segmentOffsetInDisplayLine: Int,
    val segmentContentStartInSegment: Int,
    val cursorOffsetInDisplayedContent: Int
)

/* =========================================
 * 정규화/비교 유틸
 * ========================================= */

// 비교를 위해 접두사 제거
private fun normalizeText(text: String): String {
    return text
        .replace("\t", " ")
        .trimStart()
        // 디스플레이 불릿류
        .replace(Regex("^[•●○◦▪▫]\\s+"), "")
        // MD 불릿/번호/헤딩
        .replace(Regex("^[-*+]\\s+"), "")
        .replace(Regex("^\\d+\\.\\s+"), "")
        .replace(Regex("^#{1,6}\\s+"), "")
        // 인라인 굵게 등은 비교에서 제거
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        .trim()
}

/* =========================================
 * 디스플레이 접두사 길이 측정
 * ========================================= */

private fun measureRenderedPrefixLengthAround(displayText: String, contentStartIndex: Int): Int {
    val lookBehind = 16
    val from = max(0, contentStartIndex - lookBehind)
    val context = displayText.substring(from, contentStartIndex)

    val trimmed = context.trimStart()
    val leadingSpaces = context.length - trimmed.length

    if (trimmed.startsWith("• ")) return leadingSpaces + 2

    val m = Regex("(\\d+)\\.\\s+$").find(context)
    if (m != null) return m.value.length

    // 하이픈/불릿이 없고 단순 들여쓰기만 있는 경우
    return leadingSpaces
}

/* =========================================
 * 디스플레이 라인에서 리스트 토큰 경계 찾기
 * ========================================= */

private fun findListTokensInDisplayLine(displayLine: String): List<Int> {
    val tokens = mutableListOf<Int>()

    // 불릿 기호들
    Regex("[•●○◦▪▫]").findAll(displayLine).forEach { m ->
        tokens += m.range.first
    }
    // 하이픈 (-, –, —) 후 공백
    Regex("(^|\\s)([-–—])\\s").findAll(displayLine).forEach { m ->
        val lead = m.groups[1]?.value ?: ""
        val start = m.range.first + lead.length
        tokens += start
    }
    // 번호 목록: "  12. "
    Regex("(^|\\s)(\\d+)\\.\\s").findAll(displayLine).forEach { m ->
        val lead = m.groups[1]?.value ?: ""
        val start = m.range.first + lead.length
        tokens += start
    }

    return tokens.distinct().sorted()
}

/* =========================================
 * 라인 매핑 메인 로직
 * ========================================= */

private fun mapCursorToMarkdownLine(state: RichTextState): LineMapping {
    val displayText = state.annotatedString.text
    val md = state.toMarkdown()
    val cursorPos = clamp(state.selection.end, displayText.length)

    // 디스플레이 커서가 속한 라인
    val displayLineStart = lineStartInAnnotated(displayText, cursorPos)
    val displayLineEnd = lineEndInAnnotated(displayText, cursorPos)
    val displayLine = displayText.substring(displayLineStart, displayLineEnd)

    // 세그먼트 경계 계산
    val tokens = findListTokensInDisplayLine(displayLine)
    val rel = (cursorPos - displayLineStart).coerceAtLeast(0)
    val boundaries = buildList {
        add(0); addAll(tokens); add(displayLine.length)
    }.distinct().sorted()

    var segStart = 0
    var segEnd = displayLine.length
    for (i in 0 until boundaries.size - 1) {
        val s = boundaries[i]
        val e = boundaries[i + 1]
        if (rel in s until e) { segStart = s; segEnd = e; break }
    }
    if (segStart >= segEnd) { segStart = 0; segEnd = displayLine.length }

    val rawSegment = displayLine.substring(segStart, segEnd)
    val targetSegment = rawSegment.trim()
    val segmentOffsetInDisplayLine = segStart

    // 세그먼트 내 접두사 스킵 길이
    val prefixSkip = when {
        targetSegment.startsWith("•") ||
            targetSegment.startsWith("●") ||
            targetSegment.startsWith("○") ||
            targetSegment.startsWith("◦") ||
            targetSegment.startsWith("▪") ||
            targetSegment.startsWith("▫") -> 1
        Regex("^[-–—]\\s").containsMatchIn(targetSegment) -> 2
        Regex("^\\d+\\.\\s").find(targetSegment) != null ->
            Regex("^\\d+\\.\\s").find(targetSegment)!!.value.length
        else -> 0
    }

    val contentSegment =
        if (prefixSkip > 0 && targetSegment.length > prefixSkip) targetSegment.substring(prefixSkip)
        else targetSegment

    val normalizedSegment = normalizeText(contentSegment)

    // 마크다운 라인들과 유사도 매칭
    val mdLines = md.split('\n')
    var bestMatchIndex = -1
    var bestMatchScore = 0.0

    mdLines.forEachIndexed { index, mdLine ->
        val normalizedMd = normalizeText(mdLine)
        if (normalizedMd.isNotEmpty() && normalizedSegment.isNotEmpty()) {
            val score = when {
                normalizedMd == normalizedSegment -> 1.0
                normalizedSegment.startsWith(normalizedMd) -> 0.9
                normalizedMd.startsWith(normalizedSegment) -> 0.8
                else -> {
                    val n = minOf(30, normalizedMd.length, normalizedSegment.length)
                    if (normalizedMd.take(n) == normalizedSegment.take(n)) 0.85 else 0.0
                }
            }
            if (score > bestMatchScore) { bestMatchScore = score; bestMatchIndex = index }
        }
    }

    if (bestMatchIndex == -1) {
        // 디스플레이 라인 순서로 폴백
        val lineNumber = displayText.substring(0, displayLineStart).count { it == '\n' }
        bestMatchIndex = lineNumber.coerceIn(0, mdLines.lastIndex)
    }

    val mdLineStart = mdLines.take(bestMatchIndex).sumOf { it.length + 1 }
    val mdLineEnd = mdLineStart + mdLines[bestMatchIndex].length
    val mdLine = mdLines[bestMatchIndex]

    val cursorInSegment = (rel - segmentOffsetInDisplayLine).coerceAtLeast(0)
    val segmentContentStart = when {
        prefixSkip > 0 -> targetSegment.indexOfFirst { !it.isWhitespace() } + prefixSkip
        else -> 0
    }.coerceAtLeast(0)

    val adjustedOffset = (cursorInSegment - (if (prefixSkip > 0) prefixSkip else 0)).coerceAtLeast(0)

    return LineMapping(
        mdLineStart = mdLineStart,
        mdLineEnd = mdLineEnd,
        mdLine = mdLine,
        matchedLineIndex = bestMatchIndex,
        displayLineStart = displayLineStart,
        displayLineEnd = displayLineEnd,
        selectedSegmentText = targetSegment,
        segmentOffsetInDisplayLine = segmentOffsetInDisplayLine,
        segmentContentStartInSegment = segmentContentStart,
        cursorOffsetInDisplayedContent = adjustedOffset
    )
}

/* =========================================
 * 현재 커서 라인의 마크다운 상태 감지
 * ========================================= */

fun detectLineMarkdown(state: RichTextState): LineMarkdownState {
    return try {
        val mapping = mapCursorToMarkdownLine(state)
        val mdLine = mapping.mdLine

        val headingLevel = when {
            mdLine.startsWith("## ") -> 2
            mdLine.startsWith("### ") -> 3
            // 서버 규칙: "# "는 허용 안하므로 이 경우 UI에서는 2로 간주
            mdLine.startsWith("# ") -> 2
            else -> null
        }

        val listType = when {
            mdLine.matches(Regex("^\\s*\\d+\\.\\s+.*")) -> ListType.ORDERED
            mdLine.matches(Regex("^\\s*[-*+]\\s+.*")) -> ListType.UNORDERED
            else -> null
        }

        LineMarkdownState(headingLevel, listType)
    } catch (e: Exception) {
        Log.e(TAG, "detectLineMarkdown error", e)
        LineMarkdownState(null, null)
    }
}

/* =========================================
 * 헤딩 토글 (## / ###)  — 커서 복원 포함
 * ========================================= */

fun toggleHeadingMarkdown(state: RichTextState, level: Int): Int {
    val mapping = mapCursorToMarkdownLine(state)
    val md = state.toMarkdown()

    // 선행 공백(들여쓰기) 유지
    val indent = Regex("^\\s*").find(mapping.mdLine)?.value ?: ""
    val lineNoIndent = mapping.mdLine.removePrefix(indent)

    // 현재 헤딩 파싱
    val prefixMatch = Regex("^(#{1,6}\\s+)?(.*)$").find(lineNoIndent)
    val currentPrefix = prefixMatch?.groupValues?.get(1) ?: ""
    val content = prefixMatch?.groupValues?.get(2) ?: lineNoIndent

    val currentLevel = when {
        currentPrefix.startsWith("### ") -> 3
        currentPrefix.startsWith("## ") || currentPrefix.startsWith("# ") -> 2
        else -> null
    }

    // 서버 규칙: H2/H3만 유지. 같은 레벨 클릭 시 본문으로 해제
    val newPrefix = if (currentLevel == level) "" else "#".repeat(level.coerceIn(2, 3)) + " "
    val newLine = indent + newPrefix + content

    val newMd = replaceRangeSafe(md, mapping.mdLineStart, mapping.mdLineEnd, newLine)
    state.setMarkdown(newMd)

    // 커서 복원
    val newDisplayText = state.annotatedString.text
    val newDisplayLineStart = lineStartInAnnotated(newDisplayText, state.selection.end)
    val finalCursor = clamp(newDisplayLineStart + mapping.cursorOffsetInDisplayedContent, newDisplayText.length)
    return finalCursor
}

/* =========================================
 * 헤딩 제거  — 커서 복원 포함
 * ========================================= */

fun clearHeadingMarkdown(state: RichTextState): Int {
    val mapping = mapCursorToMarkdownLine(state)
    val md = state.toMarkdown()

    val indent = Regex("^\\s*").find(mapping.mdLine)?.value ?: ""
    val lineNoIndent = mapping.mdLine.removePrefix(indent)

    if (!lineNoIndent.trimStart().startsWith("#")) {
        return state.selection.end
    }

    val content = lineNoIndent.replace(Regex("^#{1,6}\\s+"), "")
    val newLine = indent + content

    val newMd = replaceRangeSafe(md, mapping.mdLineStart, mapping.mdLineEnd, newLine)
    state.setMarkdown(newMd)

    val newDisplayText = state.annotatedString.text
    val newDisplayLineStart = lineStartInAnnotated(newDisplayText, state.selection.end)
    val finalCursor = clamp(newDisplayLineStart + mapping.cursorOffsetInDisplayedContent, newDisplayText.length)
    return finalCursor
}

/* =========================================
 * 리스트 토글 — 들여쓰기 유지 / 접두사 표준화 / 커서 정확 복원
 * ========================================= */

fun toggleListForSelection(state: RichTextState, ordered: Boolean) {
    val mapping = mapCursorToMarkdownLine(state)
    val mdBefore = state.toMarkdown()

    // 현재 라인 들여쓰기 보존
    val indent = Regex("^\\s*").find(mapping.mdLine)?.value ?: ""
    val lineNoIndent = mapping.mdLine.removePrefix(indent)

    val isOrdered = lineNoIndent.matches(Regex("^\\d+\\.\\s+.*"))
    val isUnordered = lineNoIndent.matches(Regex("^[-*+]\\s+.*"))

    // 순수 콘텐츠 추출
    val pureContent = lineNoIndent
        .replace(Regex("^#{1,6}\\s+"), "")
        .replace(Regex("^\\d+\\.\\s+"), "")
        .replace(Regex("^[-*+]\\s+"), "")

    val newPrefix = when {
        ordered && isOrdered -> ""            // 해제
        ordered && !isOrdered -> "1. "        // 서버 규칙: 숫자는 항상 1.
        !ordered && isUnordered -> ""         // 해제
        else -> "- "                          // 서버 규칙: 불릿은 항상 -
    }

    val newLine = indent + newPrefix + pureContent
    val mdAfter = replaceRangeSafe(mdBefore, mapping.mdLineStart, mapping.mdLineEnd, newLine)
    state.setMarkdown(mdAfter)

    // 커서 복원
    val newDisplayText = state.annotatedString.text
    val oldSegmentGlobalStart =
        mapping.displayLineStart + mapping.segmentOffsetInDisplayLine + mapping.segmentContentStartInSegment
    val desiredOffsetInSegment = mapping.cursorOffsetInDisplayedContent

    val occurrences = mutableListOf<Int>()
    var searchFrom = 0
    while (true) {
        val idx = newDisplayText.indexOf(pureContent, startIndex = searchFrom)
        if (idx == -1) break
        occurrences += idx
        searchFrom = idx + pureContent.length
    }

    if (occurrences.isEmpty()) {
        // 폴백
        val newMap = mapCursorToMarkdownLine(state)
        val lineStart = lineStartInAnnotated(newDisplayText, state.selection.end)
        val fallback = clamp(lineStart + newMap.cursorOffsetInDisplayedContent, newDisplayText.length)
        state.selection = TextRange(fallback)
        return
    }

    val bestOccurrence = occurrences.minByOrNull { kotlin.math.abs(it - oldSegmentGlobalStart) }!!
    val renderedPrefixLen = measureRenderedPrefixLengthAround(newDisplayText, bestOccurrence)

    val finalCursor = clamp(
        bestOccurrence + renderedPrefixLen + desiredOffsetInSegment,
        newDisplayText.length
    )
    state.selection = TextRange(finalCursor)
}

/* =========================================
 * 저장용 정규화 + 필터링 (서버 규격 준수)
 *  - 줄바꿈/탭/들여쓰기/불릿/숫자/헤딩 표준화
 *  - 허용 외 문법 제거
 * ========================================= */

fun sanitizeAndNormalizeForServer(original: String): String {
    val nl = normalizeNewLines(tabsToSpaces(original))

    val normalizedLines = nl.split('\n').map { raw ->
        val line = raw.rstrip()
        // 선행 공백 개수 → 2의 배수로 내림 정규화
        val leadingSpaces = line.takeWhile { it == ' ' }.length
        val normalizedIndent = " ".repeat(leadingSpaces - (leadingSpaces % INDENT_UNIT))
        val body = line.drop(leadingSpaces)

        // 헤딩 표준화: # → ##, ####+ 제거
        if (body.startsWith("#")) {
            return@map when {
                body.startsWith("### ") -> normalizedIndent + "### " + body.removePrefix("### ").trimStart()
                body.startsWith("## ")  -> normalizedIndent + "## "  + body.removePrefix("## ").trimStart()
                body.startsWith("# ")   -> normalizedIndent + "## "  + body.removePrefix("# ").trimStart()
                body.startsWith("####") -> normalizedIndent + body.replace(Regex("^#{4,}\\s*"), "").trimStart()
                else -> normalizedIndent + body // 방어
            }
        }

        // 리스트 표준화
        // 1) 번호 목록 → "1. "
        if (Regex("^\\d+\\.\\s+").containsMatchIn(body)) {
            val content = body.replace(Regex("^\\d+\\.\\s+"), "")
            return@map normalizedIndent + "1. " + content
        }
        // 2) 불릿 목록 → "- "
        if (Regex("^[-*+•●○◦▪▫]\\s+").containsMatchIn(body)) {
            val content = body.replace(Regex("^[-*+•●○◦▪▫]\\s+"), "")
            return@map normalizedIndent + "- " + content
        }

        // 일반 문장
        normalizedIndent + body
    }

    // 허용 외 문법/인라인 장식 제거
    val joined = normalizedLines.joinToString("\n")
        .replace(Regex("(?m)^>\\s+"), "")                  // 인용 제거
        .replace(Regex("(?s)```.*?```"), "")               // 코드블록 제거
        .replace(Regex("!\\[[^\\]]*]\\([^)]*\\)"), "")     // 이미지 제거
        .replace(Regex("\\[([^\\]]+)]\\([^)]*\\)"), "$1")  // 링크 텍스트만
        .replace(Regex("`([^`]*)`"), "$1")                 // 인라인 코드 제거
        .replace(Regex("(?<!\\*)\\*(?!\\*)([^*]+)(?<!\\*)\\*(?!\\*)"), "$1") // *italic*
        .replace(Regex("_(.+?)_"), "$1")                   // _underline_

    // 연속 공백 라인 2개 이상 → 최대 2개
    return joined
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

// 우측 공백 제거
private fun String.rstrip(): String {
    var end = this.length
    while (end > 0 && this[end - 1].isWhitespace() && this[end - 1] != '\n') end--
    return this.substring(0, end)
}
