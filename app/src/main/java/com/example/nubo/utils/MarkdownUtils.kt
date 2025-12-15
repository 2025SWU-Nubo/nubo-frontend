package com.example.nubo.utils

import android.util.Log
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState

// 디버그용 로그 태그
private const val DEBUG_TAG = "MarkdownDebug"
private const val TAG = "MarkdownUtils"

//
// ────────────────────────────────────────────────────────────────
// 1. 외부에서 사용하는 공용 타입들
// ────────────────────────────────────────────────────────────────
//

/**
 * 현재 커서가 위치한 줄의 마크다운 상태를 표현하는 데이터 클래스
 *
 * @param headingLevel  현재 줄이 헤딩이면 2 또는 3  아니면 null
 * @param listType      현재 줄이 리스트이면 ORDERED / UNORDERED  아니면 null
 */
data class LineMarkdownState(
    val headingLevel: Int? = null,
    val listType: ListType? = null
)

/**
 * 리스트 타입 구분용 enum
 */
enum class ListType {
    ORDERED,
    UNORDERED
}

//
// ────────────────────────────────────────────────────────────────
// 2. 서버 ↔ 에디터 공통 마크다운 정규화 엔트리 함수
// ────────────────────────────────────────────────────────────────
//

// - 복잡한 세그먼트 매칭 대신 "줄 번호 기준"으로 바로 매핑
private fun mapCursorToMarkdownLineSimple(state: RichTextState): LineMapping {
    val displayText = state.annotatedString.text
    val md = state.toMarkdown()
    val cursorPos = clamp(state.selection.end, displayText.length)

    // 화면 기준 현재 줄 인덱스 계산
    val displayLineIndex = displayText
        .substring(0, cursorPos)
        .count { it == '\n' }

    val displayLines = displayText.split('\n')
    val mdLines = md.split('\n')

    val safeIndex = displayLineIndex.coerceIn(0, mdLines.lastIndex)
    val mdLine = mdLines[safeIndex]
    val displayLine = displayLines.getOrElse(safeIndex) { "" }

    // 화면 텍스트에서 현재 줄 시작 위치 계산
    var displayLineStart = 0
    for (i in 0 until safeIndex) {
        displayLineStart += displayLines[i].length + 1 // 개행 포함
    }
    val displayLineEnd = displayLineStart + displayLine.length

    // 마크다운 텍스트에서 현재 줄 시작 위치 계산
    var mdLineStart = 0
    for (i in 0 until safeIndex) {
        mdLineStart += mdLines[i].length + 1
    }
    val mdLineEnd = mdLineStart + mdLine.length

    return LineMapping(
        mdLineStart = mdLineStart,
        mdLineEnd = mdLineEnd,
        mdLine = mdLine,
        matchedLineIndex = safeIndex,
        displayLineStart = displayLineStart,
        displayLineEnd = displayLineEnd,
        selectedSegmentText = displayLine,
        segmentOffsetInDisplayLine = 0,
        segmentContentStartInSegment = 0,
        cursorOffsetInDisplayedContent = cursorPos - displayLineStart
    )
}


/**
 * 서버에서 내려오는 마크다운을
 * 에디터 / 내부 유틸에서 공통으로 처리하기 쉽게 정규화하는 함수
 *
 * - 개행 코드 통일
 * - 탭 → 4칸 공백
 * - 리스트 블록 정규화 (기호/들여쓰기 규칙 통일)
 * - 헤딩 레벨 정리 (H2/H3만 허용)
 * - 여분 공백 정리
 */
fun standardizeMarkdown(md: String): String {
    var result = md

    // 1) 개행 정규화 (\r\n, \r → \n)
    result = result.replace("\r\n", "\n").replace("\r", "\n")

    // 2) 탭 문자를 공백 4칸으로 치환
    result = result.replace("\t", "    ")

    // 3) 리스트 블록 정규화 (기호 통일, 들여쓰기 정리)
    result = normalizeListBlocks(result)

    // 4) 헤딩 레벨 정규화 (H1, H4~H6 제거 / 조정)
    result = normalizeHeadings(result)

    // 5) 여분 공백 및 과도한 빈 줄 정리
    result = cleanupSpaces(result)

    return result.trimEnd()
}

//
// ────────────────────────────────────────────────────────────────
// 3. 헤딩 / 리스트 / 공백 정규화 유틸
// ────────────────────────────────────────────────────────────────
//

/**
 * 헤딩 정규화
 *
 * - "# " (H1)은 "## " (H2)로 상향 조정
 * - "#### ~ ######" (H4~H6)는 제거
 *
 * 서비스 정책
 * - 실제로는 H2, H3까지만 사용하므로 나머지는 정규화 단계에서 정리
 */
private fun normalizeHeadings(src: String): String {
    return src
        .replace(Regex("(?m)^# "), "## ")      // H1 → H2
        .replace(Regex("(?m)^#{4,6}\\s+"), "") // H4~H6 제거
}

/**
 * 리스트 패턴 매칭용 정규식
 *
 * - rxOrdered   : "숫자. 내용"
 * - rxUnordered : "- 내용", "• 내용" 등 불릿형 리스트
 * - rxOnlySpaces: 공백만 있는 줄 (빈 줄 처리용)
 */
private val rxOrdered = Regex("^(\\s*)(\\d+)\\.\\s+(.*)$")
private val rxUnordered = Regex("^(\\s*)([\\-*•●○◦▪▫])(\\s*)(.*)$")
private val rxOnlySpaces = Regex("^\\s*$")

/**
 * 리스트 블록 정규화
 *
 * 목적
 *  - 서버에서 오는 다양한 리스트 표기( *, -, 1. , • 등)를
 *    에디터에서 일관되게 처리할 수 있도록 하나의 규칙으로 통일
 *
 * 규칙
 *  - ordered list  : 항상 "1. " 로 시작 (번호는 렌더러가 다시 매김)
 *  - unordered list: 항상 "- " 로 시작
 *  - 들여쓰기는 4칸 단위로 맞춰 계층 구조 유지
 *  - 빈 줄이 나오면 "하나의 리스트 블록 종료"로 간주
 */
private fun normalizeListBlocks(src: String): String {
    val lines = src.split('\n')
    val out = StringBuilder(src.length)

    var i = 0
    while (i < lines.size) {
        val line = lines[i]

        val mOrd = rxOrdered.matchEntire(line)
        val mUnord = rxUnordered.matchEntire(line)

        // 현재 줄이 리스트가 아니면 그대로 출력 (우측 공백만 제거)
        if (mOrd == null && mUnord == null) {
            out.append(line.trimEnd())
            if (i != lines.lastIndex) out.append('\n')
            i++
            continue
        }

        // ─ 리스트 블록 시작 지점 ─
        // 이전에 내용이 있고 마지막 문자가 개행이 아니면 줄 한번 내려줌
        if (out.isNotEmpty() && out.last() != '\n') out.append('\n')

        val items = mutableListOf<Triple<String, Boolean, String>>()
        var consumedBlankLine = false

        // 연속된 리스트 줄들을 하나의 블록으로 모으는 루프
        while (i < lines.size) {
            val L = lines[i]

            // 완전 빈 줄(공백만 있는 줄)을 만나면 리스트 블록 종료
            if (rxOnlySpaces.matches(L)) {
                consumedBlankLine = true
                i++
                break
            }

            val a = rxOrdered.matchEntire(L)
            val b = rxUnordered.matchEntire(L)

            if (a != null) {
                val indent = a.groupValues[1]      // 들여쓰기 그대로 보존
                val content = a.groupValues[3].trimEnd()
                items.add(Triple(indent, true, content))
                i++
            } else if (b != null) {
                val indent = b.groupValues[1]
                val content = b.groupValues[4].trimEnd()
                items.add(Triple(indent, false, content))
                i++
            } else {
                // 리스트 패턴이 아닌 줄을 만나면 블록 종료
                break
            }
        }

        // 수집한 리스트 아이템을 정규화해서 출력
        items.forEachIndexed { idx, (indent, isOrd, content) ->
            val normalizedIndent = normalizeIndent(indent)
            out.append(normalizedIndent)

            if (isOrd) {
                // Renumber ordered list from 1 for each block
                val number = idx + 1
                out.append("$number. ")
            } else {
                out.append("- ")
            }

            out.append(content)
            if (idx != items.lastIndex) out.append('\n')
        }

        // 다음 줄이 더 있으면 블록 구분용 개행 하나 추가
        if (i < lines.size) out.append('\n')

        // 중간에 소비한 빈 줄이 있었다면 한 줄만 다시 추가 (2줄 이상이 되지 않게)
        if (consumedBlankLine) {
            out.append('\n')
        }
    }

    return out.toString().trimEnd()
}

/**
 * 들여쓰기 정규화
 *
 * - 리스트의 계층 구조를 4칸 단위로 정리
 *   0칸       → 0칸
 *   1~3칸    → 4칸
 *   4~7칸    → 4칸
 *   8~11칸   → 8칸
 *   ...
 */
private fun normalizeIndent(indent: String): String {
    // - 들여쓰기 개수를 4칸 단위로 반올림
    // - 최종적으로 0칸 또는 4칸까지만 허용
    //   0칸  → 최상위 리스트
    //   4칸  → 하위 리스트 1단계
    if (indent.isEmpty()) return ""
    val spaces = indent.length
    // Round to multiple of 4
    val normalized = ((spaces + 2) / 4) * 4
    // Limit indent to one level (4 spaces)
    val clamped = normalized.coerceAtMost(4)
    return " ".repeat(clamped)
}

/**
 * 여분 공백 정리
 *
 * - 각 줄의 우측 공백 제거
 * - 연속된 빈 줄이 3줄 이상이면 2줄(실제 빈 줄 1개)로 축소
 */
private val rxMultiBlank = Regex("\n{3,}")

private fun cleanupSpaces(src: String): String {
    return src
        .lines()
        .joinToString("\n") { it.trimEnd() }
        .replace(rxMultiBlank, "\n\n")
        .trimEnd()
}

//
// ────────────────────────────────────────────────────────────────
// 4. 커서 위치 ↔ 마크다운 줄 매핑을 위한 유틸들
// ────────────────────────────────────────────────────────────────
//

/**
 * 인덱스를 [0, len] 범위로 강제(clamp)하는 함수
 */
private fun clamp(i: Int, len: Int) = i.coerceIn(0, len)

/**
 * RichTextState → 마크다운 라인 매핑에 사용할 내부 구조체
 */
private data class LineMapping(
    val mdLineStart: Int,                  // 전체 MD 텍스트에서 현재 줄 시작 오프셋
    val mdLineEnd: Int,                    // 전체 MD 텍스트에서 현재 줄 끝 오프셋
    val mdLine: String,                    // 현재 줄의 MD 원문
    val matchedLineIndex: Int,             // mdLines 배열 인덱스
    val displayLineStart: Int,             // 화면 표시 문자열에서 현재 줄 시작 오프셋
    val displayLineEnd: Int,               // 화면 표시 문자열에서 현재 줄 끝 오프셋
    val selectedSegmentText: String,       // 동일 줄 내에서 커서가 속한 "세그먼트"의 텍스트
    val segmentOffsetInDisplayLine: Int,   // 세그먼트 시작 지점(표시 줄 기준)
    val segmentContentStartInSegment: Int, // 세그먼트 내에서 내용이 시작되는 위치
    val cursorOffsetInDisplayedContent: Int// 세그먼트 내에서 커서가 내용 기준으로 얼마나 떨어져 있는지
)

/**
 * 화면 표시 텍스트에서
 * index가 포함된 줄의 시작 오프셋 계산
 */
private fun lineStartInAnnotated(text: String, index: Int): Int {
    if (text.isEmpty()) return 0
    val i = index.coerceIn(0, text.length)
    val prev = text.lastIndexOf('\n', (i - 1).coerceAtLeast(0))
    return if (prev == -1) 0 else prev + 1
}

/**
 * 화면 표시 텍스트에서
 * index가 포함된 줄의 끝 오프셋 계산
 */
private fun lineEndInAnnotated(text: String, index: Int): Int {
    if (text.isEmpty()) return 0
    val i = clamp(index, text.length)
    val next = text.indexOf('\n', i)
    return if (next == -1) text.length else next
}

/**
 * 마크다운 라인 / 세그먼트 비교를 위해
 * 불릿 / 번호 / 헤딩 등 마크다운 기호들을 제거하고
 * 순수 텍스트만 남기도록 정규화하는 함수
 */
private fun normalizeText(text: String): String {
    return text
        .replace("\t", " ")
        .trimStart()
        // 1) heading marker first
        .replace(Regex("^#{1,6}\\s+"), "")
        // 2) then bullets and list markers
        .replace(Regex("^[•●○◦▪▫]\\s+"), "")
        .replace(Regex("^[-*+]\\s+"), "")
        .replace(Regex("^\\d+\\.\\s+"), "")
        // 3) bold markers
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        .trim()
}


/**
 * 한 줄의 화면 표시 문자열에서
 * "세그먼트 경계"가 될 수 있는 위치들을 찾아 인덱스 리스트로 반환
 *
 * 세그먼트 경계로 간주하는 것들
 * 1) 공백 2칸 이상 연속 (소프트 브레이크 → 문단 비슷하게 쪼개기)
 * 2) 불릿 기호 (• ● ○ ◦ ▪ ▫)
 * 3) 하이픈 기반 불릿 ("- 내용" 등)
 * 4) 번호 리스트 ("1. 내용" 등)
 *
 * 이렇게 잘라두면
 * 라이브러리가 실제로 한 줄에 쭉 붙여서 보여주더라도
 * "커서가 어느 세그먼트 안에 있는지" 를 알 수 있음
 */
private fun findListTokensInDisplayLine(displayLine: String): List<Int> {
    val tokens = mutableListOf<Int>()

    // 1) 연속된 공백(2칸 이상)을 세그먼트 경계로 간주
    Regex(" {2,}").findAll(displayLine).forEach { m ->
        val after = m.range.last + 1  // 공백들 바로 다음 위치
        if (after < displayLine.length) {
            tokens += after
        }
    }

    // 2) 불릿 기호
    Regex("[•●○◦▪▫]").findAll(displayLine).forEach { m ->
        tokens += m.range.first
    }

    // 3) 하이픈 기반 불릿 ("- 내용" 등)
    Regex("(^|\\s)([-–—])\\s").findAll(displayLine).forEach { m ->
        val lead = m.groups[1]?.value ?: ""
        tokens += m.range.first + lead.length
    }

    // 4) 번호 리스트 ("1. 내용" 등)
    Regex("(^|\\s)(\\d+)\\.\\s").findAll(displayLine).forEach { m ->
        val lead = m.groups[1]?.value ?: ""
        tokens += m.range.first + lead.length
    }

    return tokens.distinct().sorted()
}

/**
 * RichTextState 상의 현재 커서 위치를 기준으로
 * "어떤 마크다운 줄(mdLines[index])에 매핑되는지" 계산하는 핵심 함수
 *
 * 단계
 * 1) 화면 표시 텍스트에서 커서가 속한 줄 전체를 구한다
 * 2) 해당 줄을 여러 세그먼트(불릿/번호/2칸 이상 공백 기준)로 나눈다
 * 3) 커서가 속한 세그먼트를 targetSegment로 잡는다
 * 4) targetSegment의 "마크다운 기호 제거 + 정규화 텍스트"와
 *    전체 마크다운 mdLines들을 비교해서 가장 잘 맞는 줄을 찾는다
 */
/**
 * RichTextState 상의 현재 커서 위치를 기준으로
 * "어떤 마크다운 줄(mdLines[index])에 매핑되는지" 계산하는 핵심 함수
 *
 * 단계
 * 1) 화면 표시 텍스트에서 커서가 속한 줄 전체를 구한다
 * 2) 해당 줄을 여러 세그먼트(불릿/번호/2칸 이상 공백 기준)로 나눈다
 * 3) 커서가 속한 세그먼트를 targetSegment로 잡는다
 * 4) targetSegment의 "마크다운 기호 제거 + 정규화 텍스트"와
 *    전체 마크다운 mdLines들을 비교해서 가장 잘 맞는 줄을 찾는다
 */
private fun mapCursorToMarkdownLine(state: RichTextState): LineMapping {
    val displayText = state.annotatedString.text
    val md = state.toMarkdown()
    val cursorPos = clamp(state.selection.end, displayText.length)


    Log.d(DEBUG_TAG, "cursorPos=$cursorPos, displayLen=${displayText.length}")
    Log.d(DEBUG_TAG, "displayText='${displayText.replace("\n", "\\n")}'")
    Log.d(DEBUG_TAG, "md='${md.replace("\n", "\\n")}'")

    // 1) 화면 표시 기준 현재 줄 구하기
    val displayLineStart = lineStartInAnnotated(displayText, cursorPos)
    val displayLineEnd = lineEndInAnnotated(displayText, cursorPos)
    val displayLine = displayText.substring(displayLineStart, displayLineEnd)

    // 2) 세그먼트 경계 토큰 계산
    val tokens = findListTokensInDisplayLine(displayLine)
    val rel = (cursorPos - displayLineStart).coerceAtLeast(0)
    val boundaries = buildList {
        add(0)
        addAll(tokens)
        add(displayLine.length)
    }.distinct().sorted()

    // 3) 커서가 속한 세그먼트 범위 찾기
    var segStart = 0
    var segEnd = displayLine.length
    for (i in 0 until boundaries.size - 1) {
        val s = boundaries[i]
        val e = boundaries[i + 1]
        if (rel in s until e) {
            segStart = s
            segEnd = e
            break
        }
    }

    val rawSegment = displayLine.substring(segStart, segEnd)
    val targetSegment = rawSegment.trim()
    val segmentOffsetInDisplayLine = segStart

    // 4) 세그먼트의 앞쪽 리스트/헤딩 마커 길이 계산
    val prefixSkip = when {
        targetSegment.startsWith("•") || targetSegment.startsWith("●") ||
            targetSegment.startsWith("○") || targetSegment.startsWith("◦") ||
            targetSegment.startsWith("▪") || targetSegment.startsWith("▫") -> 1

        Regex("^[-–—]\\s").containsMatchIn(targetSegment) -> 2

        Regex("^\\d+\\.\\s").find(targetSegment) != null ->
            Regex("^\\d+\\.\\s").find(targetSegment)!!.value.length

        else -> 0
    }

    // 5) 세그먼트에서 실제 내용 부분만 추출
    val contentSegment =
        if (prefixSkip > 0 && targetSegment.length > prefixSkip)
            targetSegment.substring(prefixSkip)
        else
            targetSegment

    val normalizedSegment = normalizeText(contentSegment)
    val mdLines = md.split('\n')

    // 6) 마크다운 라인들 중에서 가장 비슷한 줄 찾기
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

    // 7) 그래도 못 찾은 경우(매칭 실패) → 심플 매핑으로 폴백
    if (bestMatchIndex == -1) {
        // 여기서 우리가 만든 심플 버전을 폴백으로 사용
        return mapCursorToMarkdownLineSimple(state)
    }

    // 8) 선택된 mdLines[bestMatchIndex]의 시작/끝 오프셋 계산
    var mdLineStart = 0
    for (i in 0 until bestMatchIndex) {
        mdLineStart += mdLines[i].length + 1 // 줄 끝 + 개행
    }

    val mdLine = mdLines[bestMatchIndex]
    val mdLineEnd = mdLineStart + mdLine.length

    // 9) 커서가 세그먼트 내에서 어느 위치인지 계산
    val cursorInSegment = (rel - segmentOffsetInDisplayLine).coerceAtLeast(0)
    val segmentContentStart = when {
        prefixSkip > 0 -> targetSegment.indexOfFirst { !it.isWhitespace() } + prefixSkip
        else -> 0
    }.coerceAtLeast(0)

    val adjustedOffset =
        (cursorInSegment - (if (prefixSkip > 0) prefixSkip else 0)).coerceAtLeast(0)

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


//
// ────────────────────────────────────────────────────────────────
// 5. 현재 줄이 헤딩/리스트/본문인지 감지하는 함수
// ────────────────────────────────────────────────────────────────
//

/**
 * RichTextState 기준으로
 * "현재 커서가 올려져 있는 줄"이 어떤 마크다운 상태인지 감지
 *
 * - 헤딩이면 headingLevel = 2 또는 3
 * - 리스트면 listType = ORDERED / UNORDERED
 * - 아무것도 아니면 둘 다 null
 */
fun detectLineMarkdown(state: RichTextState): LineMarkdownState {
    return try {
        val mapping = mapCursorToMarkdownLine(state)
        val mdLine = mapping.mdLine
        Log.d(DEBUG_TAG, "mdLine raw='${mapping.mdLine}'")


        // 🔥 앞 공백 날리고 판별해야 숫자 소제목도 인식됨
        val trimmed = mdLine.trimStart()

        val headingLevel = when {
            trimmed.startsWith("### ") -> 3
            trimmed.startsWith("## ") || trimmed.startsWith("# ") -> 2
            else -> null
        }

        val isHeadingLine = headingLevel != null

        // 리스트는 "헤딩이 아닌 줄"에서만 검사
        val targetForList = trimmed
        val listType = if (!isHeadingLine) {
            when {
                targetForList.matches(Regex("^\\d+\\.\\s+.+$")) -> ListType.ORDERED
                targetForList.matches(Regex("^[-*+•●○◦▪▫]\\s+.+$")) -> ListType.UNORDERED
                else -> null
            }
        } else {
            null
        }

        LineMarkdownState(headingLevel, listType)
    } catch (e: Exception) {
        Log.e(TAG, "detectLineMarkdown error", e)
        LineMarkdownState(null, null)
    }
}

//
// ────────────────────────────────────────────────────────────────
// 6. 리스트 토글: 본문/헤딩 ↔ 리스트
// ────────────────────────────────────────────────────────────────
//

/**
 * 현재 줄을 리스트로 변경하는 함수
 *
 * 동작 규칙
 * - 현재 줄이 본문 또는 헤딩일 때  → 리스트 마커 추가
 * - 이미 리스트(ordered/unordered)일 때
 *   동일 타입으로 또 호출하면 아무것도 하지 않음
 *
 * @param ordered true  → "1. " 형태의 번호 리스트
 *                false → "- " 형태의 불릿 리스트
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

    // 헤딩 줄에서는 "숫자 리스트" 생성 금지 (본문에서만 허용)
    if (isHeading && ordered) {
        return
    }

    // 이미 같은 타입의 리스트라면 아무 작업도 하지 않음
    if ((ordered && isOrdered) || (!ordered && isUnordered)) {
        return
    }

    // 현재 줄의 순수 내용만 추출 (헤딩/리스트 마커 제거)
    val pureContent = when {
        isHeading -> lineNoIndent.replaceFirst(Regex("^#{1,6}\\s+"), "")
        isOrdered -> lineNoIndent.replaceFirst(Regex("^\\d+\\.\\s+"), "")
        isUnordered -> lineNoIndent.replaceFirst(Regex("^[-*+•●○◦▪▫]\\s+"), "")
        else -> lineNoIndent
    }

    // 새 리스트 마커 결정
    val newPrefix = if (ordered) "1. " else "- "
    val newLine = indent + newPrefix + pureContent

    // 해당 MD 줄 교체
    val mdLines = beforeMd.split('\n').toMutableList()
    mdLines[mapping.matchedLineIndex] = newLine

    val mdAfter = mdLines.joinToString("\n")
    state.setMarkdown(mdAfter)

    // 커서 위치 보정
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
    val newLineStart =
        lineStartInAnnotated(newDisplayText, beforeCursor.coerceAtMost(newDisplayText.length))
    val newCursor =
        (newLineStart + cursorOffsetInLine + prefixDelta).coerceIn(0, newDisplayText.length)

    state.selection = TextRange(newCursor)
}

//
// ────────────────────────────────────────────────────────────────
// 7. 헤딩/리스트 → 본문 (A안: 리스트 라인 비우고 아래에 본문 추가)
// ────────────────────────────────────────────────────────────────
//

/**
 * 현재 줄을 "순수 본문"으로 만드는 함수
 *
 * 처리 규칙
 * - 헤딩(#, ##, ### ...) 이면 헤딩 마커만 제거
 * - 리스트(1. / - / 불릿) 이면
 *   👉 A안 적용
 *      1) 현재 줄을 "" (빈 줄)로 바꿔서 리스트 블록을 끊고
 *      2) 그 아래 줄에 본문 내용을 새로 삽입
 *      → 결과적으로 리스트 밖에 있는 완전한 독립 문단이 됨
 *
 * - 이미 본문이면 아무것도 하지 않고 현재 커서만 그대로 반환
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

    // 이미 헤딩도 리스트도 아닌 순수 본문이면 아무 작업도 하지 않음
    if (!isHeading && !isOrdered && !isUnordered) {
        return state.selection.end
    }

    // 헤딩 / 리스트 접두사를 제거한 순수 내용
    val pureContent = when {
        isHeading -> lineNoIndent.replace(Regex("^#{1,6}\\s+"), "")
        isOrdered -> lineNoIndent.replaceFirst(Regex("^\\d+\\.\\s+"), "")
        isUnordered -> lineNoIndent.replaceFirst(Regex("^[-*+•●○◦▪▫]\\s+"), "")
        else -> lineNoIndent
    }.trimStart() // 리스트 → 본문일 때 들여쓰기도 제거해서 진짜 문단으로

    val mdLines = beforeMd.split('\n').toMutableList()
    val index = mapping.matchedLineIndex

    if (isOrdered || isUnordered) {
        // ──────────────────────────────────────
        //  리스트 → 본문 (A안 핵심)
        // ──────────────────────────────────────
        //
        // 예시
        //   1. 첫 번째 줄
        //   1. 두 번째 줄   ← 여기에서 본문으로 변경
        //   1. 세 번째 줄
        //
        // 처리 후
        //   1. 첫 번째 줄
        //   ""                 ← 리스트 블록 끊는 빈 줄
        //   두 번째 줄         ← 완전한 독립 문단
        //   1. 세 번째 줄      ← 아래 리스트는 별도 블록으로 취급 가능
        //
        // 이렇게 해야 나중에 본문 줄을 위로 올리더라도
        // "리스트 내부 문장"으로 잘못 인식되는 걸 막을 수 있음

        // 1) 현재 리스트 줄을 빈 줄로 변경
        mdLines[index] = ""

        // 2) 빈 줄 아래에 순수 본문 내용 삽입
        mdLines.add(index + 1, pureContent)
    } else {
        // 헤딩 → 본문
        // 단순히 헤딩 마커만 제거하고 기존 위치를 유지
        mdLines[index] = indent + pureContent
    }

    // 변경된 마크다운을 다시 적용
    val newMd = mdLines.joinToString("\n")
    state.setMarkdown(newMd)

    // 커서는 일단 기존 위치 근처로 유지
    val newDisplayText = state.annotatedString.text
    val newCursor = newDisplayText.length.coerceAtMost(beforeCursor)
    state.selection = TextRange(newCursor)

    return newCursor
}

//
// ────────────────────────────────────────────────────────────────
// 8. 헤딩 토글 (본문 ↔ H2 ↔ H3)
// ────────────────────────────────────────────────────────────────
//

/**
 * 현재 줄의 헤딩 레벨을 토글하는 함수
 *
 * 레벨 규칙
 * - level = 2 → H2 ("## ")
 * - level = 3 → H3 ("### ")
 *
 * 동작
 * - 본문 → level에 해당하는 헤딩으로 변경
 * - 이미 같은 level 헤딩 → 본문으로 되돌림
 * - H2 <-> H3 변경도 지원
 */
// English comment: Do NOT adjust caret by markdown prefix length, selection is based on displayed text
fun toggleHeadingMarkdown(state: RichTextState, level: Int) {
    val beforeMd = state.toMarkdown()
    val mapping = mapCursorToMarkdownLine(state)

    val rawLine = mapping.mdLine
    val trimmed = rawLine.trimStart()

    val currentLevel = when {
        trimmed.startsWith("### ") -> 3
        trimmed.startsWith("## ") || trimmed.startsWith("# ") -> 2
        else -> null
    }

    val newPrefix =
        if (currentLevel == level) ""
        else "#".repeat(level.coerceIn(2, 3)) + " "

    val content = stripMdLineMarkers(rawLine)
    val newLine = newPrefix + content

    val mdLines = beforeMd.split('\n').toMutableList()
    mdLines[mapping.matchedLineIndex] = newLine

    state.setMarkdown(mdLines.joinToString("\n"))
}

// English comment: Strip heading and list markers to keep only the plain text content
private fun stripMdLineMarkers(line: String): String {
    var s = line.trimStart()

    // Remove heading markers first
    s = s.replaceFirst(Regex("^#{1,6}\\s+"), "")

    // Remove ordered list marker like "1. "
    s = s.replaceFirst(Regex("^\\d+\\.\\s+"), "")

    // Remove unordered list marker like "- " or bullets
    s = s.replaceFirst(Regex("^[-*+•●○◦▪▫]\\s+"), "")

    return s.trimStart()
}

