package com.example.nubo.utils

import android.util.Log
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.math.max
import kotlin.math.min

private const val DEBUG_TAG = "MarkdownDebug"

// CommonMark 호환 정규화: 모든 곳에서 사용할 표준 마크다운
fun standardizeMarkdown(md: String): String {
    var result = md

    // 1단계: 개행 정규화 (\r\n, \r → \n)
    result = result.replace("\r\n", "\n").replace("\r", "\n")

    // 2단계: 탭을 공백 4칸으로
    result = result.replace("\t", "    ")

    // 3단계: 리스트 블록 정규화 (들여쓰기 유지, 기호 통일)
    result = normalizeListBlocks(result)

    // 4단계: 헤딩 표준화 (H1 제거, H2/H3만 허용)
    result = normalizeHeadings(result)

    // 5단계: 여분 공백 정리
    result = cleanupSpaces(result)

    return result.trimEnd()
}

/**
 * 헤딩 정규화: H1 제거, H4~H6 제거
 */
private fun normalizeHeadings(src: String): String {
    return src
        .replace(Regex("(?m)^# "), "## ")  // H1 → H2로 상향 조정
        .replace(Regex("(?m)^#{4,6}\\s+"), "")  // H4~H6 제거
}

/**
 * 리스트 블록 정규화: 들여쓰기 보존, 기호 표준화
 *
 * 특징:
 * - 들여쓰기 레벨별로 0칸, 4칸, 8칸... 으로 정렬
 * - ordered: 항상 "1. " (렌더러가 자동 번호)
 * - unordered: 항상 "- "
 * - 빈 줄로 리스트 블록 분리
 */
private val rxOrdered   = Regex("^(\\s*)(\\d+)\\.\\s+(.*)$")
private val rxUnordered = Regex("^(\\s*)([\\-*•●○◦▪▫])(\\s*)(.*)$")
private val rxOnlySpaces = Regex("^\\s*$")

private fun normalizeListBlocks(src: String): String {
    val lines = src.split('\n')
    val out = StringBuilder(src.length)

    var i = 0
    while (i < lines.size) {
        val line = lines[i]

        val mOrd = rxOrdered.matchEntire(line)
        val mUnord = rxUnordered.matchEntire(line)

        // 리스트가 아니면 그대로 출력 (우측 공백만 제거)
        if (mOrd == null && mUnord == null) {
            out.append(line.trimEnd())
            if (i != lines.lastIndex) out.append('\n')
            i++
            continue
        }

        // ─── 리스트 블록 시작 ───
        if (out.isNotEmpty() && out.last() != '\n') out.append('\n')

        val items = mutableListOf<Triple<String, Boolean, String>>()
        var consumedBlankLine = false

        // 연속된 리스트 아이템 수집
        while (i < lines.size) {
            val L = lines[i]

            // 빈 줄 만나면 블록 종료
            if (rxOnlySpaces.matches(L)) {
                consumedBlankLine = true
                i++
                break
            }

            val a = rxOrdered.matchEntire(L)
            val b = rxUnordered.matchEntire(L)

            if (a != null) {
                val indent = a.groupValues[1]  // 들여쓰기 보존
                val content = a.groupValues[3].trimEnd()
                items.add(Triple(indent, true, content))
                i++
            } else if (b != null) {
                val indent = b.groupValues[1]  // 들여쓰기 보존
                val content = b.groupValues[4].trimEnd()
                items.add(Triple(indent, false, content))
                i++
            } else {
                // 리스트 아닌 줄 만나면 블록 종료
                break
            }
        }

        // 리스트 아이템 출력
        items.forEachIndexed { idx, (indent, isOrd, content) ->
            val normalized = normalizeIndent(indent)
            out.append(normalized)
            out.append(if (isOrd) "1. " else "- ")
            out.append(content)
            if (idx != items.lastIndex) out.append('\n')
        }

        // 블록 구분 (다음 줄이 있으면 개행)
        if (i < lines.size) out.append('\n')

        // 소비한 빈 줄 보존 (1줄만)
        if (consumedBlankLine) {
            out.append('\n')
        }
    }

    return out.toString().trimEnd()
}

/**
 * 들여쓰기 정규화: 4칸 단위로 정렬
 * 0칸 → 0칸
 * 1~3칸 → 4칸
 * 4~7칸 → 4칸
 * 8~11칸 → 8칸
 */
private fun normalizeIndent(indent: String): String {
    if (indent.isEmpty()) return ""
    val spaces = indent.length
    // 4칸 단위로 반올림
    val normalized = ((spaces + 2) / 4) * 4
    return " ".repeat(normalized)
}

/**
 * 여분 공백 정리
 * - 연속 빈 줄 (3줄 이상) → 2줄 (1개의 빈 줄)
 * - 각 줄의 우측 공백 제거
 */
private val rxMultiBlank = Regex("\n{3,}")

private fun cleanupSpaces(src: String): String {
    return src
        .lines()
        .joinToString("\n") { it.trimEnd() }
        .replace(rxMultiBlank, "\n\n")
        .trimEnd()
}

// ═══════════════════════════════════════════════════════════════
// 기존 유틸 함수들 (호환성 유지)
// ═══════════════════════════════════════════════════════════════

private const val TAG = "MarkdownUtils"

private fun clamp(i: Int, len: Int) = i.coerceIn(0, len)

data class LineMarkdownState(
    val headingLevel: Int? = null,
    val listType: ListType? = null
)

enum class ListType {
    ORDERED,
    UNORDERED
}

private data class LineMapping(
    val mdLineStart: Int,
    val mdLineEnd: Int,
    val mdLine: String,
    val matchedLineIndex: Int,
    val displayLineStart: Int,
    val displayLineEnd: Int,
    val selectedSegmentText: String,
    val segmentOffsetInDisplayLine: Int,
    val segmentContentStartInSegment: Int,
    val cursorOffsetInDisplayedContent: Int
)

private fun lineStartInAnnotated(text: String, index: Int): Int {
    if (text.isEmpty()) return 0
    val i = index.coerceIn(0, text.length)
    val prev = text.lastIndexOf('\n', (i - 1).coerceAtLeast(0))
    return if (prev == -1) 0 else prev + 1
}

private fun lineEndInAnnotated(text: String, index: Int): Int {
    if (text.isEmpty()) return 0
    val i = clamp(index, text.length)
    val next = text.indexOf('\n', i)
    return if (next == -1) text.length else next
}

private fun normalizeText(text: String): String {
    return text
        .replace("\t", " ")
        .trimStart()
        .replace(Regex("^[•●○◦▪▫]\\s+"), "")
        .replace(Regex("^[-*+]\\s+"), "")
        .replace(Regex("^\\d+\\.\\s+"), "")
        .replace(Regex("^#{1,6}\\s+"), "")
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        .trim()
}


private fun findListTokensInDisplayLine(displayLine: String): List<Int> {
    val tokens = mutableListOf<Int>()

    // 1) Soft break by multiple spaces
    //    Treat 2 or more consecutive spaces as a "pseudo newline"
    Regex(" {2,}").findAll(displayLine).forEach { m ->
        val after = m.range.last + 1  // first char after spaces
        if (after < displayLine.length) {
            tokens += after
        }
    }

    // 2) Bullet tokens
    Regex("[•●○◦▪▫]").findAll(displayLine).forEach { m ->
        tokens += m.range.first
    }

    // 3) Unordered list with hyphen-like bullets
    Regex("(^|\\s)([-–—])\\s").findAll(displayLine).forEach { m ->
        val lead = m.groups[1]?.value ?: ""
        tokens += m.range.first + lead.length
    }

    // 4) Ordered list "1. "
    Regex("(^|\\s)(\\d+)\\.\\s").findAll(displayLine).forEach { m ->
        val lead = m.groups[1]?.value ?: ""
        tokens += m.range.first + lead.length
    }

    return tokens.distinct().sorted()
}

//private fun findListTokensInDisplayLine(displayLine: String): List<Int> {
//    val tokens = mutableListOf<Int>()
//
//    Regex("[•●○◦▪▫]").findAll(displayLine).forEach { m ->
//        tokens += m.range.first
//    }
//    Regex("(^|\\s)([-–—])\\s").findAll(displayLine).forEach { m ->
//        val lead = m.groups[1]?.value ?: ""
//        tokens += m.range.first + lead.length
//    }
//    Regex("(^|\\s)(\\d+)\\.\\s").findAll(displayLine).forEach { m ->
//        val lead = m.groups[1]?.value ?: ""
//        tokens += m.range.first + lead.length
//    }
//    return tokens.distinct().sorted()
//}

fun detectLineMarkdown(state: RichTextState): LineMarkdownState {
    return try {
        val mapping = mapCursorToMarkdownLine(state)
        val mdLine = mapping.mdLine

        val headingLevel = when {
            mdLine.startsWith("### ") -> 3
            mdLine.startsWith("## ") || mdLine.startsWith("# ") -> 2
            else -> null
        }

        val listType = when {
            mdLine.matches(Regex("^\\s*\\d+\\.\\s+.+$")) -> ListType.ORDERED
            mdLine.matches(Regex("^\\s*[-*+•●○◦▪▫]\\s+.+$")) -> ListType.UNORDERED
            else -> null
        }

        LineMarkdownState(headingLevel, listType)
    } catch (e: Exception) {
        Log.e(TAG, "detectLineMarkdown error", e)
        LineMarkdownState(null, null)
    }
}

private fun mapCursorToMarkdownLine(state: RichTextState): LineMapping {
    val displayText = state.annotatedString.text
    val md = state.toMarkdown()
    val cursorPos = clamp(state.selection.end, displayText.length)

    val displayLineStart = lineStartInAnnotated(displayText, cursorPos)
    val displayLineEnd = lineEndInAnnotated(displayText, cursorPos)
    val displayLine = displayText.substring(displayLineStart, displayLineEnd)

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

    val rawSegment = displayLine.substring(segStart, segEnd)
    val targetSegment = rawSegment.trim()
    val segmentOffsetInDisplayLine = segStart

    val prefixSkip = when {
        targetSegment.startsWith("•") || targetSegment.startsWith("●") ||
            targetSegment.startsWith("○") || targetSegment.startsWith("◦") ||
            targetSegment.startsWith("▪") || targetSegment.startsWith("▫") -> 1
        Regex("^[-–—]\\s").containsMatchIn(targetSegment) -> 2
        Regex("^\\d+\\.\\s").find(targetSegment) != null ->
            Regex("^\\d+\\.\\s").find(targetSegment)!!.value.length
        else -> 0
    }

    val contentSegment =
        if (prefixSkip > 0 && targetSegment.length > prefixSkip)
            targetSegment.substring(prefixSkip) else targetSegment

    val normalizedSegment = normalizeText(contentSegment)
    val mdLines = md.split('\n')
    var bestMatchIndex = -1
    var bestMatchScore = 0.0

    for (index in mdLines.indices) {
        val mdLine = mdLines[index]
        val normalizedMd = normalizeText(mdLine)
        if (normalizedMd.isEmpty() || normalizedSegment.isEmpty()) continue

        if (normalizedMd == normalizedSegment) {
            bestMatchIndex = index
            bestMatchScore = 1.0
            break
        }

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
        val lineNumber = displayText.substring(0, displayLineStart).count { it == '\n' }
        bestMatchIndex = lineNumber.coerceIn(0, mdLines.lastIndex)
    }

    var mdLineStart = 0
    for (i in 0 until bestMatchIndex) {
        mdLineStart += mdLines[i].length + 1
    }

    val mdLine = mdLines[bestMatchIndex]
    val mdLineEnd = mdLineStart + mdLine.length

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

/**
 * 리스트 타입 추가
 *
 * - 본문/헤딩 상태 → 리스트 추가
 * - 리스트 상태 → 아무것도 하지 않음 (이미 같은 타입이므로)
 *
 * ordered: true면 "1. ", false면 "- "
 *
 * ✅ 헤딩을 리스트로 변경할 때: 헤딩 마커(#) 제거 후 리스트 마커 추가
 */
fun toggleListForSelection(state: RichTextState, ordered: Boolean) {
    val beforeMd = state.toMarkdown()
    val beforeCursor = state.selection.end

    val mapping = mapCursorToMarkdownLine(state)

    val indent = Regex("^\\s*").find(mapping.mdLine)?.value ?: ""
    val lineNoIndent = mapping.mdLine.removePrefix(indent)

    val isOrdered = lineNoIndent.matches(Regex("^\\d+\\.\\s+.*"))
    val isUnordered = lineNoIndent.matches(Regex("^[-*+•●○◦▪▫]\\s+.*"))
    val isHeading = lineNoIndent.matches(Regex("^#{1,6}\\s+.*"))

    // ✅ 이미 같은 타입의 리스트면 아무것도 하지 않음
    if ((ordered && isOrdered) || (!ordered && isUnordered)) {
        return
    }

    // ✅ 정확한 콘텐츠 추출 (헤딩, 리스트 마커 모두 제거)
    val pureContent = when {
        isHeading -> lineNoIndent.replaceFirst(Regex("^#{1,6}\\s+"), "")
        isOrdered -> lineNoIndent.replaceFirst(Regex("^\\d+\\.\\s+"), "")
        isUnordered -> lineNoIndent.replaceFirst(Regex("^[-*+•●○◦▪▫]\\s+"), "")
        else -> lineNoIndent
    }

    // ✅ 새 리스트 마커 추가
    val newPrefix = if (ordered) "1. " else "- "
    val newLine = indent + newPrefix + pureContent

    val mdLines = beforeMd.split('\n').toMutableList()
    mdLines[mapping.matchedLineIndex] = newLine

    val mdAfter = mdLines.joinToString("\n")
    state.setMarkdown(mdAfter)

    // 커서 위치 조정
    val oldPrefixLen = when {
        isHeading -> Regex("^#{1,6}\\s+").find(lineNoIndent)?.value?.length ?: 0
        isOrdered -> Regex("^\\d+\\.\\s+").find(lineNoIndent)?.value?.length ?: 0
        isUnordered -> Regex("^[-*+•●○◦▪▫]\\s+").find(lineNoIndent)?.value?.length ?: 0
        else -> 0
    }
    val newPrefixLen = newPrefix.length
    val prefixDelta = newPrefixLen - oldPrefixLen

    val displayLineStart = mapping.displayLineStart
    val cursorOffsetInLine = beforeCursor - displayLineStart
    val newDisplayText = state.annotatedString.text
    val newLineStart = lineStartInAnnotated(newDisplayText, beforeCursor.coerceAtMost(newDisplayText.length))
    val newCursor = (newLineStart + cursorOffsetInLine + prefixDelta).coerceIn(0, newDisplayText.length)

    state.selection = TextRange(newCursor)
}

/**
 * 본문으로 변경: 헤딩과 리스트 마커 모두 제거
 *
 * - 헤딩 상태 → 본문으로 변경 (# 제거)
 * - 리스트 상태 → 본문으로 변경 (- 또는 1. 제거)
 * - 본문 상태 → 아무것도 하지 않음
 */
fun clearHeadingMarkdown(state: RichTextState): Int {
    val beforeMd = state.toMarkdown()
    val beforeCursor = state.selection.end

    val mapping = mapCursorToMarkdownLine(state)

    val indent = Regex("^\\s*").find(mapping.mdLine)?.value ?: ""
    val lineNoIndent = mapping.mdLine.removePrefix(indent)

    val isHeading = lineNoIndent.trimStart().matches(Regex("^#{1,6}\\s+.*"))
    val isOrdered = lineNoIndent.matches(Regex("^\\d+\\.\\s+.*"))
    val isUnordered = lineNoIndent.matches(Regex("^[-*+•●○◦▪▫]\\s+.*"))

    // 헤딩도 리스트도 아니면 그대로 둠
    if (!isHeading && !isOrdered && !isUnordered) {
        return state.selection.end
    }

    // 리스트 / 헤딩 접두사 제거한 순수 내용
    val pureContent = when {
        isHeading -> lineNoIndent.replace(Regex("^#{1,6}\\s+"), "")
        isOrdered -> lineNoIndent.replaceFirst(Regex("^\\d+\\.\\s+"), "")
        isUnordered -> lineNoIndent.replaceFirst(Regex("^[-*+•●○◦▪▫]\\s+"), "")
        else -> lineNoIndent
    }.trimStart()  // 리스트 → 본문일 때는 들여쓰기도 제거해서 완전한 문단으로

    val mdLines = beforeMd.split('\n').toMutableList()
    val index = mapping.matchedLineIndex

    if (isOrdered || isUnordered) {
        // ▼▼▼ 리스트 → 본문일 때 핵심 로직 ▼▼▼

        // 1) 현재 줄을 "리스트 끝"용 빈 줄로 바꾸고
        mdLines[index] = ""

        // 2) 그 아래에 본문 줄을 새로 삽입
        mdLines.add(index + 1, pureContent)

        // 이렇게 되면
        //   1. 첫번째 줄
        //   ""            ← 리스트 블록 종료
        //   "두번째 줄"   ← 완전한 독립 문단
        //   1. 세번째 줄
    } else {
        // 헤딩 → 본문은 그냥 접두사만 제거
        mdLines[index] = indent + pureContent
    }

    val newMd = mdLines.joinToString("\n")
    state.setMarkdown(newMd)

    // 커서는 일단 새 문단 시작 쪽으로 대충 맞춰두자
    // (나중에 필요하면 다시 미세 조정)
    val newDisplayText = state.annotatedString.text
    val newCursor = newDisplayText.length.coerceAtMost(beforeCursor)
    state.selection = TextRange(newCursor)

    return newCursor
}


/**
 * 헤딩 토글: H2 ↔ H3 ↔ 본문
 * - 본문 → H2 추가
 * - H2 → H3으로 변경
 * - H3 → 본문으로 변경
 */
fun toggleHeadingMarkdown(state: RichTextState, level: Int): Int {
    val beforeMd = state.toMarkdown()
    val beforeCursor = state.selection.end

    val mapping = mapCursorToMarkdownLine(state)

    val indent = Regex("^\\s*").find(mapping.mdLine)?.value ?: ""
    val lineNoIndent = mapping.mdLine.removePrefix(indent)

    val prefixMatch = Regex("^(#{1,6}\\s+)?(.*)$").find(lineNoIndent)
    val currentPrefix = prefixMatch?.groupValues?.get(1) ?: ""
    val content = prefixMatch?.groupValues?.get(2) ?: lineNoIndent

    val currentLevel = when {
        currentPrefix.startsWith("### ") -> 3
        currentPrefix.startsWith("## ") || currentPrefix.startsWith("# ") -> 2
        else -> null
    }

    val newPrefix = if (currentLevel == level) "" else "#".repeat(level.coerceIn(2, 3)) + " "
    val newLine = indent + newPrefix + content

    val mdLines = beforeMd.split('\n').toMutableList()
    mdLines[mapping.matchedLineIndex] = newLine

    val newMd = mdLines.joinToString("\n")
    state.setMarkdown(newMd)

    // ✅ 커서 위치 조정: 접두사 길이 차이 계산
    val oldPrefixLen = currentPrefix.length
    val newPrefixLen = newPrefix.length
    val prefixDelta = newPrefixLen - oldPrefixLen

    val displayLineStart = mapping.displayLineStart
    val cursorOffsetInLine = beforeCursor - displayLineStart
    val newDisplayText = state.annotatedString.text
    val newLineStart = lineStartInAnnotated(newDisplayText, beforeCursor.coerceAtMost(newDisplayText.length))
    val newCursor = (newLineStart + cursorOffsetInLine + prefixDelta).coerceIn(0, newDisplayText.length)

    state.selection = TextRange(newCursor)
    return newCursor
}
