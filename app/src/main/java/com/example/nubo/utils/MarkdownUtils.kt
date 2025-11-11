package com.example.nubo.utils

import android.util.Log
import android.view.Choreographer
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.math.max
import kotlin.math.min

/* =========================================
 * 공통 상수/유틸
 * ========================================= */

private const val TAG = "MarkdownUtils"
private const val INDENT_UNIT = 3 // 들여쓰기 3칸 고정

// 인덱스를 문자열 길이 범위 내로 제한
private fun clamp(i: Int, len: Int) = i.coerceIn(0, len)

// 개행 정규화: \r\n, \r → \n
private fun normalizeNewLines(s: String) = s.replace("\r\n", "\n").replace("\r", "\n")

// 탭 → 공백 2칸
private fun tabsToSpaces(s: String) = s.replace("\t", "  ")

// **텍스트** 만 있는(앞뒤 공백 제외) 라인을 감지
private fun isStrongOnlyLine(line: String): Boolean {
    val t = line.trim()
    // **...** 패턴만 있고, 앞뒤가 ** 로 감싸진 순수 굵은 텍스트
    return Regex("^\\*\\*[^*].*[^*]\\*\\*\$").matches(t)
}

/* =========================================
 * 디스플레이 텍스트(annotatedString.text) 라인 탐색
 * ========================================= */

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

    for (index in mdLines.indices) {
        val mdLine = mdLines[index]
        val normalizedMd = normalizeText(mdLine)
        if (normalizedMd.isEmpty() || normalizedSegment.isEmpty()) continue

        // 1) 정확 일치 → 즉시 확정하고 루프 종료
        if (normalizedMd == normalizedSegment) {
            bestMatchIndex = index
            bestMatchScore = 1.0
            break
        }

        // 2) 근사 매칭
        val score = when {
            normalizedSegment.startsWith(normalizedMd) -> 0.9
            normalizedMd.startsWith(normalizedSegment) -> 0.8
            else -> {
                val n = minOf(30, normalizedMd.length, normalizedSegment.length)
                if (normalizedMd.take(n) == normalizedSegment.take(n)) 0.85 else 0.0
            }
        }

        if (score > bestMatchScore) {
            bestMatchScore = score
            bestMatchIndex = index
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

        // ── 추가: 굵게만 있는 라인은 리스트 아님 ──
        if (isStrongOnlyLine(mdLine)) {
            val headingLevel = when {
                mdLine.trimStart().startsWith("## ") -> 2
                mdLine.trimStart().startsWith("### ") -> 3
                mdLine.trimStart().startsWith("# ") -> 2
                else -> null
            }
            return LineMarkdownState(headingLevel = headingLevel, listType = null)
        }

        val headingLevel = when {
            mdLine.startsWith("## ") -> 2
            mdLine.startsWith("### ") -> 3
            // 서버 규칙: "# "는 허용 안하므로 이 경우 UI에서는 2로 간주
            mdLine.startsWith("# ") -> 2
            else -> null
        }

        val listType = when {
            mdLine.matches(Regex("^\\s*\\d+\\.\\s+.+$")) -> ListType.ORDERED
            mdLine.matches(Regex("^\\s*[-*+]\\s+.+$")) -> ListType.UNORDERED
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
    // 1) 현재 상태 저장
    val beforeMd = state.toMarkdown()
    val beforeDisplayText = state.annotatedString.text
    val beforeCursor = state.selection.end

    Log.d("CursorDebug", "Before: cursor=$beforeCursor, displayLen=${beforeDisplayText.length}, mdLen=${beforeMd.length}")

    // 2) 라인 매핑
    val mapping = mapCursorToMarkdownLine(state)

    // 3) 들여쓰기 보존 및 새 라인 생성
    val indent = Regex("^\\s*").find(mapping.mdLine)?.value ?: ""
    val lineNoIndent = mapping.mdLine.removePrefix(indent)

    val isOrdered = lineNoIndent.matches(Regex("^\\d+\\.\\s+.*"))
    val isUnordered = lineNoIndent.matches(Regex("^[-*+]\\s+.*"))

    val pureContent = lineNoIndent
        .replace(Regex("^#{1,6}\\s+"), "")
        .replace(Regex("^\\d+\\.\\s+"), "")
        .replace(Regex("^[-*+]\\s+"), "")

    // 토글 로직
    val newPrefix = when {
        ordered && isOrdered -> ""
        ordered && !isOrdered -> "1. "
        !ordered && isUnordered -> ""
        else -> "- "
    }

    val newLine = indent + newPrefix + pureContent

    // 4) 마크다운 업데이트
    val mdAfter = replaceRangeSafe(beforeMd, mapping.mdLineStart, mapping.mdLineEnd, newLine)

    // 5) **핵심 개선**: 커서 위치를 상대적으로 계산
    // 디스플레이 라인 시작점 기준으로 오프셋 계산
    val displayLineStart = mapping.displayLineStart
    val cursorOffsetInLine = beforeCursor - displayLineStart

    // 접두사 길이 변화 계산
    val oldPrefixLen = when {
        isOrdered -> Regex("^\\d+\\.\\s+").find(lineNoIndent)?.value?.length ?: 0
        isUnordered -> 2 // "- "
        else -> 0
    }

    val newPrefixLen = when {
        newPrefix == "1. " -> 3
        newPrefix == "- " -> 2
        else -> 0
    }

    val prefixDelta = newPrefixLen - oldPrefixLen

    // 6) 마크다운 설정 (한 번만!)
    state.setMarkdown(mdAfter)

    // 7) **개선된 커서 계산**: 새 텍스트 기준으로 다시 계산
    val newDisplayText = state.annotatedString.text

    // 새 디스플레이에서 같은 라인의 시작점 찾기
    val newLineStart = lineStartInAnnotated(newDisplayText, beforeCursor.coerceAtMost(newDisplayText.length))

    // 커서를 상대 위치 + 접두사 변화량으로 조정
    val newCursor = (newLineStart + cursorOffsetInLine + prefixDelta)
        .coerceIn(0, newDisplayText.length)

    // 8) 커서 설정 (한 번만!)
    state.selection = TextRange(newCursor)

    Log.d("CursorDebug", "After: cursor=$newCursor, displayLen=${newDisplayText.length}, mdLen=${mdAfter.length}, prefixDelta=$prefixDelta")
}

// lineStartInAnnotated 함수 (이미 존재하지만 필요시 추가)
private fun lineStartInAnnotated(text: String, index: Int): Int {
    if (text.isEmpty()) return 0
    val i = index.coerceIn(0, text.length)
    val prev = text.lastIndexOf('\n', (i - 1).coerceAtLeast(0))
    return if (prev == -1) 0 else prev + 1
}

/* =========================================
 * 저장용 정규화 + 필터링 (서버 규격 준수)
 *  - 줄바꿈/탭/들여쓰기/불릿/숫자/헤딩 표준화
 *  - 허용 외 문법 제거
 * ========================================= */

// 우측 공백 제거
private fun String.rstrip(): String {
    var end = this.length
    while (end > 0 && this[end - 1].isWhitespace() && this[end - 1] != '\n') end--
    return this.substring(0, end)
}

/* =========================================
 * 리스트 블록 정규화
 * - 들여쓰기를 캡처하여 그대로 보존
 * - ordered는 "1. "로 통일 (렌더러 자동 번호)
 * - unordered는 "- "로 통일
 * ========================================= */

private val rxOrdered   = Regex("^(\\s*)(\\d+)\\.\\s+(.*)$")   // ← indent 그룹 포함
private val rxUnordered = Regex("^(\\s*)([\\-*•])\\s+(.*)$")
private val rxOnlySpaces = Regex("^\\s*$")

private fun normalizeListBlocks(src: String): String {
    val lines = src.split('\n')
    val out = StringBuilder(src.length)

    fun snapIndent(indent: String): String {
        if (indent.isEmpty()) return ""
        // Snap to 3-space grid (server/renderer rule)
        val snapped = maxOf(INDENT_UNIT, (indent.length / INDENT_UNIT) * INDENT_UNIT)
        return " ".repeat(snapped)
    }

    var i = 0
    while (i < lines.size) {
        val line = lines[i]

        val mOrdStart = rxOrdered.matchEntire(line)
        val mUnStart  = rxUnordered.matchEntire(line)

        if (mOrdStart == null && mUnStart == null) {
            out.append(line.trimEnd())
            if (i != lines.lastIndex) out.append('\n')
            i++
            continue
        }

        // 블록 앞쪽 공백 1줄 확보
        if (out.isNotEmpty() && out.last() != '\n') out.append('\n')

        val items = mutableListOf<Triple<String, Boolean, String>>() // (indent, isOrdered, content)
        while (i < lines.size) {
            val L = lines[i]
            if (rxOnlySpaces.matches(L)) { i++; continue } // 내부 빈 줄 제거

            val a = rxOrdered.matchEntire(L)
            val b = rxUnordered.matchEntire(L)
            if (a != null) {
                items += Triple(a.groupValues[1], true,  a.groupValues[3].trimEnd())
            } else if (b != null) {
                items += Triple(b.groupValues[1], false, b.groupValues[3].trimEnd())
            } else break
            i++
        }

        items.forEachIndexed { idx, (indent, isOrd, content) ->
            out.append(snapIndent(indent))
            out.append(if (isOrd) "1. " else "- ")
            out.append(content.trim())
            if (idx != items.lastIndex) out.append('\n')
        }

        // 블록 뒤쪽 구분 한 줄
        if (i < lines.size) out.append('\n')
    }
    return out.toString().trimEnd()
}

/* =========================================
 * 여분 공백 정리
 * - 문단 사이 3줄 이상 → 1줄
 * - 트레일링 스페이스 제거
 * ========================================= */

private val rxMultiBlank = Regex("\n{3,}")

private fun cleanupSpaces(src: String): String {
    return src
        .lines()
        .joinToString("\n") { it.trimEnd() }
        .replace(rxMultiBlank, "\n\n")
        .trimEnd()
}

/* =========================================
 * 허용 마크다운만 유지 (인라인/블록 장식 평문화) + 불릿 기호 통일
 *  - 들여쓰기는 보존
 * ========================================= */

fun sanitizeToAllowedMarkdown(md: String): String {
    return md
        // 헤딩 정리: H1 제거, H4~H6 제거 (H2/H3만 허용)
        .replace(Regex("(?m)^#\\s+"), "")
        .replace(Regex("(?m)^#{4,6}\\s+"), "")
        // 인용/코드/이미지/링크/인라인코드 제거 → 평문화
        .replace(Regex("(?m)^>\\s+"), "")
        .replace(Regex("(?s)```.*?```"), "")
        .replace(Regex("!\\[[^\\]]*]\\([^)]*\\)"), "")
        .replace(Regex("\\[([^\\]]+)]\\([^)]*\\)"), "$1")
        .replace(Regex("`([^`]*)`"), "$1")
        // 이탤릭 기호 제거 (굵게는 ** 그대로 두되, 렌더러가 무시해도 텍스트 보존)
        .replace(Regex("(?<!\\*)\\*(?!\\*)([^*]+)(?<!\\*)\\*(?!\\*)"), "$1")
        .replace(Regex("_(.+?)_"), "$1")
        // 불릿 기호 통일 (• 등 → -), 들여쓰기는 그대로 둠
        .replace(Regex("(?m)^(\\s*)[•●○◦▪▫](\\s+)"), "$1- ")
        .trim()
}

/* =========================================
 * 서버 저장용: 리스트 들여쓰기 최소 3칸으로 보정
 * ========================================= */

private fun bumpIndentIfList(line: String): String {
    val leadingSpaces = line.takeWhile { it == ' ' }.length
    val body = line.drop(leadingSpaces)
    val isList = Regex("^((\\d+)\\.\\s+)|([-*+•●○◦▪▫]\\s+)").containsMatchIn(body)
    if (!isList) return line // 리스트가 아니면 그대로

    // 최소 단위 3칸 보장
    val fixed = if (leadingSpaces in 1..2) 3 else leadingSpaces
    return " ".repeat(fixed) + body
}

fun sanitizeAndNormalizeForServer(md: String): String {
    val nl = normalizeNewLines(tabsToSpaces(md))
    return nl
        .lines()
        .joinToString("\n") { raw -> bumpIndentIfList(raw.rstrip()) }
        // 이하 기존 치환은 유지 (헤딩/H4~H6/링크/코드/이미지 등 평문화)
        .replace(Regex("(?m)^#\\s+"), "")
        .replace(Regex("(?m)^#{4,6}\\s+"), "")
        .replace(Regex("(?m)^>\\s+"), "")
        .replace(Regex("(?s)```.*?```"), "")
        .replace(Regex("!\\[[^\\]]*]\\([^)]*\\)"), "")
        .replace(Regex("\\[([^\\]]+)]\\([^)]*\\)"), "$1")
        .replace(Regex("`([^`]*)`"), "$1")
        .replace(Regex("(?<!\\*)\\*(?!\\*)([^*]+)(?<!\\*)\\*(?!\\*)"), "$1")
        .replace(Regex("_(.+?)_"), "$1")
        // 불릿 기호 통일만 수행 (들여쓰기는 위에서 보존/보정)
        .replace(Regex("(?m)^(\\s*)[•●○◦▪▫](\\s+)"), "$1- ")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

/**
 * 저장/표시 공용 정규화 엔트리
 * - 1단계: 허용 마크다운만 남김 (코드, 인라인코드, 링크 등 제거/평문화)
 * - 2단계: 리스트 블록 정규화(들여쓰기 보존, 기호 통일, ordered는 1. 고정)
 * - 3단계: 여분 공백/빈줄 정리
 */
fun canonicalizeMarkdown(md: String): String {
    val sanitized = sanitizeToAllowedMarkdown(md)
    val listFixed = normalizeListBlocks(sanitized)
    return cleanupSpaces(listFixed)
}
