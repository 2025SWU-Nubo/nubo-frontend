package com.example.nubo.utils

import android.util.Log
import android.view.Choreographer
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.math.max
import kotlin.math.min



private const val DEBUG_TAG = "MarkdownDebug"

// Markdown 파서 보호용: 목록 토큰 뒤에 바로 오는 ** 앞에 ZWSP 삽입
private val rxListThenBold = Regex("(?m)^(\\s*(?:\\d+\\.\\s+|[-*+]\\s+))(\\*\\*)(.+)$")
private val rxNestedOrdered = Regex("(?m)^(\\s{2,})\\d+\\.\\s+")

fun demoteNestedOrderedToBullets(md: String): String =
    md.replace(rxNestedOrdered) { m -> "${m.groupValues[1]}- " }

/** Insert ZWSP between list token and starting **bold** to avoid parser bug */
fun shimListBoldBug(md: String): String =
    md.replace(rxListThenBold) { m -> m.groupValues[1] + "\u200B" + m.groupValues[2] + m.groupValues[3] }

/** Remove the ZWSP we inserted (for saving / VM state) */
fun stripShimForServer(md: String): String =
    md.replace("\u200B**", "**")


/**
 * 서버에서 받은 원본 마크다운과 정규화 과정을 디버깅
 */
fun debugMarkdownNormalization(originalMd: String, tag: String = "Normalization") {
    Log.d(DEBUG_TAG, "═══════════════════════════════════════")
    Log.d(DEBUG_TAG, "[$tag] 원본 마크다운 길이: ${originalMd.length}")
    Log.d(DEBUG_TAG, "[$tag] 원본 줄 수: ${originalMd.lines().size}")
    Log.d(DEBUG_TAG, "[$tag] 원본 내용:")
    originalMd.lines().forEachIndexed { idx, line ->
        Log.d(DEBUG_TAG, "  [$idx] '${line}' (len=${line.length})")
    }

    // 1단계: sanitize
    val sanitized = sanitizeToAllowedMarkdown(originalMd)
    Log.d(DEBUG_TAG, "[$tag] Sanitized 길이: ${sanitized.length}")
    Log.d(DEBUG_TAG, "[$tag] Sanitized 내용:")
    sanitized.lines().forEachIndexed { idx, line ->
        Log.d(DEBUG_TAG, "  [$idx] '${line}'")
    }

    // 2단계: normalizeListBlocks
    val listFixed = normalizeListBlocks(sanitized)
    Log.d(DEBUG_TAG, "[$tag] ListFixed 길이: ${listFixed.length}")
    Log.d(DEBUG_TAG, "[$tag] ListFixed 내용:")
    listFixed.lines().forEachIndexed { idx, line ->
        Log.d(DEBUG_TAG, "  [$idx] '${line}'")
    }

    // 3단계: cleanupSpaces
    val final = cleanupSpaces(listFixed)
    Log.d(DEBUG_TAG, "[$tag] Final 길이: ${final.length}")
    Log.d(DEBUG_TAG, "[$tag] Final 내용:")
    final.lines().forEachIndexed { idx, line ->
        Log.d(DEBUG_TAG, "  [$idx] '${line}'")
    }
    Log.d(DEBUG_TAG, "═══════════════════════════════════════")
}

/**
 * RichTextState의 상태를 디버깅
 */
fun debugRichTextState(state: RichTextState, tag: String = "RichTextState") {
    val markdown = state.toMarkdown()
    val displayText = state.annotatedString.text
    val selection = state.selection

    Log.d(DEBUG_TAG, "═══════════════════════════════════════")
    Log.d(DEBUG_TAG, "[$tag] 마크다운 길이: ${markdown.length}")
    Log.d(DEBUG_TAG, "[$tag] 디스플레이 텍스트 길이: ${displayText.length}")
    Log.d(DEBUG_TAG, "[$tag] 커서 위치: start=${selection.start}, end=${selection.end}")

    Log.d(DEBUG_TAG, "[$tag] 마크다운 내용:")
    markdown.lines().forEachIndexed { idx, line ->
        Log.d(DEBUG_TAG, "  MD[$idx] '${line}' (len=${line.length})")
    }

    Log.d(DEBUG_TAG, "[$tag] 디스플레이 내용:")
    displayText.lines().forEachIndexed { idx, line ->
        Log.d(DEBUG_TAG, "  DISP[$idx] '${line}' (len=${line.length})")
    }
    Log.d(DEBUG_TAG, "═══════════════════════════════════════")
}

/**
 * 라인 매핑 과정을 디버깅
 */
fun debugLineMapping(state: RichTextState, tag: String = "LineMapping") {
    try {
        val mapping = mapCursorToMarkdownLine(state)

        Log.d(DEBUG_TAG, "═══════════════════════════════════════")
        Log.d(DEBUG_TAG, "[$tag] 매핑 결과:")
        Log.d(DEBUG_TAG, "  - MD 라인 인덱스: ${mapping.matchedLineIndex}")
        Log.d(DEBUG_TAG, "  - MD 라인 시작: ${mapping.mdLineStart}")
        Log.d(DEBUG_TAG, "  - MD 라인 끝: ${mapping.mdLineEnd}")
        Log.d(DEBUG_TAG, "  - MD 라인 내용: '${mapping.mdLine}'")
        Log.d(DEBUG_TAG, "  - 디스플레이 라인 시작: ${mapping.displayLineStart}")
        Log.d(DEBUG_TAG, "  - 디스플레이 라인 끝: ${mapping.displayLineEnd}")
        Log.d(DEBUG_TAG, "  - 선택된 세그먼트: '${mapping.selectedSegmentText}'")
        Log.d(DEBUG_TAG, "  - 세그먼트 오프셋: ${mapping.segmentOffsetInDisplayLine}")
        Log.d(DEBUG_TAG, "  - 커서 오프셋: ${mapping.cursorOffsetInDisplayedContent}")
        Log.d(DEBUG_TAG, "═══════════════════════════════════════")
    } catch (e: Exception) {
        Log.e(DEBUG_TAG, "[$tag] 라인 매핑 실패", e)
    }
}

/**
 * 문자열 비교 디버깅 (정규화 전후)
 */
fun debugTextComparison(text1: String, text2: String, label1: String = "Text1", label2: String = "Text2") {
    Log.d(DEBUG_TAG, "═══════════════════════════════════════")
    Log.d(DEBUG_TAG, "텍스트 비교:")
    Log.d(DEBUG_TAG, "[$label1] 길이=${text1.length}")
    Log.d(DEBUG_TAG, "[$label2] 길이=${text2.length}")

    val norm1 = normalizeText(text1)
    val norm2 = normalizeText(text2)

    Log.d(DEBUG_TAG, "정규화 후:")
    Log.d(DEBUG_TAG, "[$label1] '${norm1}' (len=${norm1.length})")
    Log.d(DEBUG_TAG, "[$label2] '${norm2}' (len=${norm2.length})")
    Log.d(DEBUG_TAG, "일치 여부: ${norm1 == norm2}")

    if (norm1 != norm2) {
        // 첫 번째 차이점 찾기
        val minLen = minOf(norm1.length, norm2.length)
        for (i in 0 until minLen) {
            if (norm1[i] != norm2[i]) {
                Log.d(DEBUG_TAG, "첫 번째 차이점 위치: $i")
                Log.d(DEBUG_TAG, "  [$label1]: '${norm1[i]}' (code=${norm1[i].code})")
                Log.d(DEBUG_TAG, "  [$label2]: '${norm2[i]}' (code=${norm2[i].code})")
                break
            }
        }
    }
    Log.d(DEBUG_TAG, "═══════════════════════════════════════")
}

/**
 * 개행 문자 분석
 */
fun debugNewLines(text: String, tag: String = "NewLines") {
    Log.d(DEBUG_TAG, "═══════════════════════════════════════")
    Log.d(DEBUG_TAG, "[$tag] 개행 문자 분석:")
    Log.d(DEBUG_TAG, "  - \\n 개수: ${text.count { it == '\n' }}")
    Log.d(DEBUG_TAG, "  - \\r 개수: ${text.count { it == '\r' }}")
    Log.d(DEBUG_TAG, "  - \\r\\n 개수: ${text.split("\r\n").size - 1}")

    text.forEachIndexed { idx, char ->
        if (char == '\n' || char == '\r') {
            Log.d(DEBUG_TAG, "  위치 $idx: '${char.code}' (${if (char == '\n') "\\n" else "\\r"})")
        }
    }
    Log.d(DEBUG_TAG, "═══════════════════════════════════════")
}

/**
 * EditCardScreen에서 호출할 통합 디버깅 함수
 */
fun debugFullPipeline(
    serverMarkdown: String,
    richTextState: RichTextState,
    tag: String = "FullPipeline"
) {
    Log.d(DEBUG_TAG, "\n\n")
    Log.d(DEBUG_TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    Log.d(DEBUG_TAG, "전체 파이프라인 디버깅 시작: $tag")
    Log.d(DEBUG_TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

    // 1. 서버에서 받은 원본
    Log.d(DEBUG_TAG, "\n[1단계] 서버 원본 마크다운:")
    debugNewLines(serverMarkdown, "서버 원본")
    debugMarkdownNormalization(serverMarkdown, "서버->정규화")

    // 2. RichTextState 상태
    Log.d(DEBUG_TAG, "\n[2단계] RichTextState 현재 상태:")
    debugRichTextState(richTextState, "현재 상태")

    // 3. 라인 매핑
    Log.d(DEBUG_TAG, "\n[3단계] 라인 매핑:")
    debugLineMapping(richTextState, "현재 커서")

    // 4. 비교
    Log.d(DEBUG_TAG, "\n[4단계] 서버 vs RichText 비교:")
    val canonical = canonicalizeMarkdown(serverMarkdown)
    val current = richTextState.toMarkdown()
    debugTextComparison(canonical, current, "서버(정규화)", "RichText")

    Log.d(DEBUG_TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    Log.d(DEBUG_TAG, "전체 파이프라인 디버깅 종료: $tag")
    Log.d(DEBUG_TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")
}


/* =========================================
 * 공통 상수/유틸
 * ========================================= */

private const val TAG = "MarkdownUtils"

// 인덱스를 문자열 길이 범위 내로 제한
private fun clamp(i: Int, len: Int) = i.coerceIn(0, len)

// 개행 정규화: \r\n, \r → \n
private fun normalizeNewLines(s: String) = s.replace("\r\n", "\n").replace("\r", "\n")

// 탭 → 공백 4칸
private fun tabsToSpaces(s: String) = s.replace("\t", "    ")

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

/* =========================================
 * 헤딩 제거  — 커서 복원 포함
 * ========================================= */

/* =========================================
 * 리스트 토글 — 들여쓰기 유지 / 접두사 표준화 / 커서 정확 복원
 * ========================================= */

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
        return if (indent.isEmpty()) "" else " ".repeat(((indent.length + 3) / 4) * 4) // 4칸 반올림
    }

    var i = 0
    while (i < lines.size) {
        val line = lines[i]

        val mOrdStart = rxOrdered.matchEntire(line)
        val mUnStart  = rxUnordered.matchEntire(line)

        if (mOrdStart == null && mUnStart == null) {
            // 리스트가 아니면 있는 그대로(우측 공백만 제거) 출력
            out.append(line.trimEnd())
            if (i != lines.lastIndex) out.append('\n')
            i++
            continue
        }

        // 리스트 블록 시작 전, 블론 간 1줄 간격 보장(중복 \n 방지)
        if (out.isNotEmpty() && out.last() != '\n') out.append('\n')

        // -- 리스트 아이템 수집
        val items = mutableListOf<Triple<String, Boolean, String>>() // (indent, isOrdered, content)
        var consumedBlankLineInsideBlock = false

        while (i < lines.size) {
            val L = lines[i]

            // 빈 줄을 만나면 블록 종료(빈 줄은 나중에 그대로 1줄 보존)
            if (rxOnlySpaces.matches(L)) {
                consumedBlankLineInsideBlock = true
                i++
                break
            }

            val a = rxOrdered.matchEntire(L)
            val b = rxUnordered.matchEntire(L)
            if (a != null) {
                items += Triple(a.groupValues[1], true,  a.groupValues[3].trimEnd())
                i++
            } else if (b != null) {
                items += Triple(b.groupValues[1], false, b.groupValues[3].trimEnd())
                i++
            } else {
                break
            }
        }

        // ── 리스트 아이템 “한 번만” 출력
        items.forEachIndexed { idx, (indent, isOrd, content) ->
            out.append(snapIndent(indent))
            out.append(if (isOrd) "1. " else "- ")
            out.append(content.trimEnd())
            if (idx != items.lastIndex) out.append('\n')
        }

        // 블록 뒤쪽 구분(다음 줄이 존재하면 줄바꿈 추가)
        if (i < lines.size) out.append('\n')

        // 방금 소비한 빈 줄을 1줄로 보존
        if (consumedBlankLineInsideBlock) {
            out.append('\n')
        }
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
        .replace(Regex("(?m)^#{4,6}\\s+"), "") // H4~H6만 제거
        // .replace(Regex("(?m)^>\\s+"), "")    // blockquote 유지하려면 주석
        // 코드/링크 유지하려면 아래 3개 주석
        // .replace(Regex("(?s)```.*?```"), "")
        // .replace(Regex("\\[([^\\]]+)]\\([^)]*\\)"), "$1")
        // .replace(Regex("`([^`]*)`"), "$1")
        .replace(Regex("!\\[[^\\]]*]\\([^)]*\\)"), "") // 이미지는 정책상 제거
        .replace(Regex("(?<!\\*)\\*(?!\\*)([^*]+)(?<!\\*)\\*(?!\\*)"), "$1") // 단일 이탤릭 평문화
        .replace(Regex("_(.+?)_"), "$1")
        .replace(Regex("(?m)^(\\s*)[•●○◦▪▫](\\s+)"), "$1- ")
        .trimEnd()
}

/* =========================================
 * 서버 저장용: 리스트 들여쓰기 최소 3칸으로 보정
 * ========================================= */

private fun bumpIndentIfList(line: String): String {
    val leading = line.takeWhile { it == ' ' }.length
    val body = line.drop(leading)
    val isList = Regex("^((\\d+)\\.\\s+)|([-*+•●○◦▪▫]\\s+)").containsMatchIn(body)
    if (!isList) return line // 리스트가 아니면 그대로

    val fixed = when (leading) {
        0 -> 0               // 최상위는 0칸
        in 1..3 -> 4         // 서브레벨 최소 4칸
        else -> ((leading + 3) / 4) * 4 // 4칸 단위 반올림
    }

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


/* =========================================
 * 개선된 라인 매핑 - 개행 문자 올바른 처리
 * ========================================= */

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
    if (segStart >= segEnd) { segStart = 0; segEnd = displayLine.length }

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
        if (prefixSkip > 0 && targetSegment.length > prefixSkip) targetSegment.substring(prefixSkip)
        else targetSegment

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

    // **핵심 수정**: 개행 문자를 올바르게 포함
    var mdLineStart = 0
    for (i in 0 until bestMatchIndex) {
        mdLineStart += mdLines[i].length + 1 // +1은 '\n'
    }

    val mdLine = mdLines[bestMatchIndex]
    val mdLineEnd = mdLineStart + mdLine.length  // 개행 문자는 제외 (다음 라인 시작 직전)

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
 * 개선된 toggleListForSelection - 개행 보존
 * ========================================= */

/* =========================================
 * 핵심 수정: toggleListForSelection
 * - 리스트 해제 시 이전 라인이 헤딩이면 빈 줄 삽입
 * ========================================= */

fun toggleListForSelection(state: RichTextState, ordered: Boolean) {
    val beforeMd = state.toMarkdown()
    val beforeDisplayText = state.annotatedString.text
    val beforeCursor = state.selection.end

    Log.d("CursorDebug", "Before toggle: cursor=$beforeCursor, ordered=$ordered")

    val mapping = mapCursorToMarkdownLine(state)

    val indent = Regex("^\\s*").find(mapping.mdLine)?.value ?: ""
    val lineNoIndent = mapping.mdLine.removePrefix(indent)

    val isOrdered = lineNoIndent.matches(Regex("^\\d+\\.\\s+.*"))
    val isUnordered = lineNoIndent.matches(Regex("^[-*+]\\s+.*"))

    val pureContent = lineNoIndent
        .replace(Regex("^#{1,6}\\s+"), "")
        .replace(Regex("^\\d+\\.\\s+"), "")
        .replace(Regex("^[-*+]\\s+"), "")

    val newPrefix = when {
        ordered && isOrdered -> ""
        ordered && !isOrdered -> "1. "
        !ordered && isUnordered -> ""
        else -> "- "
    }

    val newLine = indent + newPrefix + pureContent

    val mdLines = beforeMd.split('\n').toMutableList()

    // **핵심 수정 1**: 리스트를 본문으로 변경할 때 이전 라인 체크
    val isRemovingList = (isOrdered || isUnordered) && newPrefix.isEmpty()
    if (isRemovingList && mapping.matchedLineIndex > 0) {
        val prevLine = mdLines[mapping.matchedLineIndex - 1]
        val isPrevHeading = prevLine.trimStart().matches(Regex("^#{1,6}\\s+.*"))

        // 이전 라인이 헤딩이면 빈 줄 삽입
        if (isPrevHeading) {
            Log.d("CursorDebug", "Inserting blank line after heading: '$prevLine'")
            mdLines.add(mapping.matchedLineIndex, "")  // 현재 라인 앞에 빈 줄 추가
        }
    }

    // **핵심 수정 2**: 인덱스 조정 (빈 줄이 추가되었을 수 있음)
    val targetIndex = if (isRemovingList && mapping.matchedLineIndex > 0) {
        val prevLine = mdLines[mapping.matchedLineIndex - 1]
        val isPrevHeading = prevLine.trimStart().matches(Regex("^#{1,6}\\s+.*"))
        if (isPrevHeading) mapping.matchedLineIndex + 1 else mapping.matchedLineIndex
    } else {
        mapping.matchedLineIndex
    }

    mdLines[targetIndex] = newLine
    val mdAfter = mdLines.joinToString("\n")

    Log.d("CursorDebug", "Line $targetIndex: '${mapping.mdLine}' -> '$newLine'")

    val displayLineStart = mapping.displayLineStart
    val cursorOffsetInLine = beforeCursor - displayLineStart

    // 📍정규식으로 실제 접두사 길이를 추출해서 사용
    val ordMatch = Regex("^\\d+\\.\\s+").find(lineNoIndent)?.value
    val unordMatch = Regex("^[-*+]\\s+").find(lineNoIndent)?.value


    val oldPrefixLen = when {
        isOrdered   -> ordMatch?.length ?: 0
        isUnordered -> unordMatch?.length ?: 0
        else        -> 0
    }
    val newPrefixLen = when {
        newPrefix == "1. " -> 3
        newPrefix == "- "  -> 2
        else               -> 0
    }


    val prefixDelta = newPrefixLen - oldPrefixLen

    state.setMarkdown(mdAfter)

    val newDisplayText = state.annotatedString.text
    val newLineStart = lineStartInAnnotated(newDisplayText, beforeCursor.coerceAtMost(newDisplayText.length))
    val newCursor = (newLineStart + cursorOffsetInLine + prefixDelta)
        .coerceIn(0, newDisplayText.length)

    state.selection = TextRange(newCursor)

    Log.d("CursorDebug", "After toggle: cursor=$newCursor, prefixDelta=$prefixDelta")
}

/* =========================================
 * 추가 개선: clearHeadingMarkdown
 * - 헤딩 해제 시 다음 라인이 본문이면 빈 줄 삽입
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

    val mdLines = md.split('\n').toMutableList()
    mdLines[mapping.matchedLineIndex] = newLine

    // **추가**: 다음 라인이 본문(헤딩/리스트 아님)이면 빈 줄 삽입
    if (mapping.matchedLineIndex < mdLines.lastIndex) {
        val nextLine = mdLines[mapping.matchedLineIndex + 1]
        val isNextPlainText = !nextLine.trimStart().matches(Regex("^(#{1,6}\\s+|\\d+\\.\\s+|[-*+]\\s+).*"))

        if (isNextPlainText && nextLine.isNotBlank()) {
            Log.d("MarkdownUtils", "Inserting blank line before plain text: '$nextLine'")
            mdLines.add(mapping.matchedLineIndex + 1, "")
        }
    }

    val newMd = mdLines.joinToString("\n")

    state.setMarkdown(newMd)

    val newDisplayText = state.annotatedString.text
    val newDisplayLineStart = lineStartInAnnotated(newDisplayText, state.selection.end)
    val finalCursor = clamp(newDisplayLineStart + mapping.cursorOffsetInDisplayedContent, newDisplayText.length)
    return finalCursor
}

/* =========================================
 * 보완: toggleHeadingMarkdown
 * - 헤딩 추가 시 다음 라인과의 빈 줄 관리
 * ========================================= */

fun toggleHeadingMarkdown(state: RichTextState, level: Int): Int {
    val mapping = mapCursorToMarkdownLine(state)
    val md = state.toMarkdown()

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

    val mdLines = md.split('\n').toMutableList()
    mdLines[mapping.matchedLineIndex] = newLine

    // **개선**: 헤딩 추가/제거 시 다음 라인과의 간격 조정
    val isAddingHeading = currentLevel == null && newPrefix.isNotEmpty()
    val isRemovingHeading = currentLevel != null && newPrefix.isEmpty()

    if (isAddingHeading && mapping.matchedLineIndex < mdLines.lastIndex) {
        val nextLine = mdLines[mapping.matchedLineIndex + 1]
        // 다음 라인이 빈 줄이 아니고 본문이면 빈 줄 삽입
        if (nextLine.isNotBlank() && !nextLine.trimStart().startsWith("#")) {
            mdLines.add(mapping.matchedLineIndex + 1, "")
        }
    } else if (isRemovingHeading && mapping.matchedLineIndex < mdLines.lastIndex) {
        val nextLine = mdLines[mapping.matchedLineIndex + 1]
        // 다음이 빈 줄이면 제거 (중복 방지)
        if (nextLine.isBlank() && mapping.matchedLineIndex + 2 <= mdLines.lastIndex) {
            mdLines.removeAt(mapping.matchedLineIndex + 1)
        }
    }

    val newMd = mdLines.joinToString("\n")

    state.setMarkdown(newMd)

    val newDisplayText = state.annotatedString.text
    val newDisplayLineStart = lineStartInAnnotated(newDisplayText, state.selection.end)
    val finalCursor = clamp(newDisplayLineStart + mapping.cursorOffsetInDisplayedContent, newDisplayText.length)
    return finalCursor
}
