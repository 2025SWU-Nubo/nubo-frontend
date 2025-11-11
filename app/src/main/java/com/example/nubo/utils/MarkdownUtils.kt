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

/**
 * 저장/표시 공용 정규화 엔트리
 * - 1단계: 허용 마크다운만 남김 (코드, 인라인코드, 링크 등 제거/평문화)
 * - 2단계: 리스트 블록 정규화(들여쓰기 제거, 빈줄 제거, 기호 통일, ordered는 1. 고정)
 * - 3단계: 여분 공백/빈줄 정리
 */
fun canonicalizeMarkdown(md: String): String {
    val sanitized = sanitizeToAllowedMarkdown(md)
    val listFixed = normalizeListBlocks(sanitized)
    return cleanupSpaces(listFixed)
}

/* =========================================
 * 리스트 블록 정규화
 * - 목표:
 *   1) 최상위(열 0)에서만 시작하도록 들여쓰기 제거
 *   2) ordered는 모두 "1. " 로 저장 (렌더러가 자동 번호)
 *   3) unordered는 "-" 하나로 통일
 *   4) 아이템 사이 빈 줄 제거 → 연속 블록 유지 (자동 번호 끊김 방지)
 *   5) 블록 앞뒤로는 필요 시 1줄만 남김
 * ========================================= */

private val rxOrdered = Regex("^\\s*(\\d+)\\.\\s+(.*)$")
private val rxUnordered = Regex("^\\s*([\\-*•])\\s+(.*)$")
private val rxOnlySpaces = Regex("^\\s*$")

private fun normalizeListBlocks(src: String): String {
    val lines = src.split('\n')
    val out = StringBuilder(src.length)

    var i = 0
    while (i < lines.size) {
        val line = lines[i]

        // 1) 리스트 블록 시작인지 감지
        val isOrderedStart = rxOrdered.matches(line)
        val isUnorderedStart = rxUnordered.matches(line)

        if (!isOrderedStart && !isUnorderedStart) {
            // 리스트 블록이 아니면 있는 그대로(단, 뒤 공백 제거)
            out.append(line.trimEnd())
            if (i != lines.lastIndex) out.append('\n')
            i++
            continue
        }

        // 2) 리스트 블록 수집
        val blockStart = i
        val blockItems = mutableListOf<Pair<Boolean, String>>() // (isOrdered, content)
        while (i < lines.size) {
            val L = lines[i]
            // 빈 줄이면 "블록 내부"에서는 스킵 (연속성 유지)
            if (rxOnlySpaces.matches(L)) {
                // 내부 빈 줄은 건너뜀
                i++
                continue
            }
            val mOrd = rxOrdered.matchEntire(L)
            val mUn  = rxUnordered.matchEntire(L)
            if (mOrd != null) {
                blockItems += true to mOrd.groupValues[2].trimEnd()
            } else if (mUn != null) {
                blockItems += false to mUn.groupValues[2].trimEnd()
            } else {
                // 리스트가 아닌 라인을 만나면 블록 종료
                break
            }
            i++
        }
        val blockEnd = i - 1

        // 3) 블록을 정규화 출력
        //    - ordered → "1. <content>"
        //    - unordered → "- <content>"
        //    - 아이템 사이에 빈 줄 넣지 않음
        val isOrderedBlock = blockItems.firstOrNull()?.first == true
        blockItems.forEachIndexed { idx, (isOrd, content) ->
            if (isOrd) {
                out.append("1. ").append(content.trim())
            } else {
                out.append("- ").append(content.trim())
            }
            if (idx != blockItems.lastIndex) out.append('\n')
        }

        // 4) 블록 뒤쪽 처리
        // 다음 라인이 존재하고, 그 다음 내용이 비문장/문단이면 블록과 분리용 공백 1줄만 유지
        val hasNext = (blockEnd + 1) < lines.lastIndex
        val nextLine = lines.getOrNull(blockEnd + 1)
        if (nextLine != null) {
            // 다음 라인이 리스트가 아니고 빈 줄이 아니라면, 구분용 빈 줄 1개 추가
            val nextIsList = rxOrdered.matches(nextLine) || rxUnordered.matches(nextLine)
            if (!rxOnlySpaces.matches(nextLine) && !nextIsList) {
                out.append('\n')
            }
            // 원본에 빈 줄이 여러 개 있었다면 하나로 축약 (cleanupSpaces에서도 한 번 더 정리됨)
            if (!nextIsList) out.append('\n')
        } else if (i <= lines.lastIndex) {
            // 범위 보호: 보통 여기 안 들어옴
            out.append('\n')
        }
    }

    // 마지막 여분 빈 줄 제거
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
 * 기존: sanitizeToAllowedMarkdown() 유지
 * (필요 추가: 리스트 앞 들여쓰기 제거 & 불릿 기호 통일)
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
        // 리스트 앞 불필요한 들여쓰기 제거 (열 0 정렬)
        .replace(Regex("(?m)^\\s+(\\d+\\.\\s+)"), "$1")
        .replace(Regex("(?m)^\\s*([\\-*•])\\s+"), "$1 ") // 기호 뒤 공백 1개로 통일
        // 불릿 기호 통일 (• 등 → -)
        .replace(Regex("(?m)^[•]\\s+"), "- ")
        .trim()
}
