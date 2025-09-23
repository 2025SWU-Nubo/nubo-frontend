package com.example.nubo.ui.screen.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import com.example.nubo.R
import com.example.nubo.data.dto.HighlightDto
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey5
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.utils.sanitizeToAllowedMarkdown
import com.example.nubo.utils.toggleBoldMarkdown
import com.example.nubo.utils.toggleHeadingMarkdown
import com.example.nubo.utils.toggleListForSelection
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.commonmark.Markdown
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditCardScreen(
    summary: String,
    highlights: List<HighlightDto>,
    onBack: () -> Unit,
    onSummaryChange: (String) -> Unit,
    onToggleHighlight: (start: Int, end: Int) -> Unit,
    onSave: () -> Unit
) {
    val rtState = rememberRichTextState()
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current

    // === 상태 관리 ===
    // 에디터 포커스 상태
    var editorFocused by remember { mutableStateOf(false) }

    // 터치 영역 추적을 위한 경계 정보 (Compose Rect 사용)
    var editorBounds by remember { mutableStateOf<Rect?>(null) }
    var toolbarBounds by remember { mutableStateOf<Rect?>(null) }

    // === IME(키보드) 상태 계산 ===
    val imeInsets = WindowInsets.ime
    val navInsets = WindowInsets.navigationBars
    val imeHeight = imeInsets.getBottom(density)
    val navHeight = navInsets.getBottom(density)

    // 키보드 표시 상태 (높이가 0보다 크면 키보드가 올라왔다고 판단)
    val imeVisible by remember { derivedStateOf { imeHeight > 0 } }

    // 툴바를 키보드 바로 위에 위치시키기 위한 오프셋
    // IME 높이에서 네비게이션 바 높이를 뺀 만큼 올림
    val toolbarOffset = if (imeVisible) (imeHeight - navHeight).coerceAtLeast(0) else 0

    // === 수정 모드 상태 결정 ===
    // 수정 모드: 에디터에 포커스가 있고 키보드가 표시된 상태
    val isEditMode by remember { derivedStateOf { editorFocused && imeVisible } }

    // 디버깅용 로그 (개발 중에만 사용)
    LaunchedEffect(editorFocused, imeVisible, isEditMode) {
        println("EditCardScreen - editorFocused: $editorFocused, imeVisible: $imeVisible, isEditMode: $isEditMode")
    }

    // === 초기화 및 이벤트 처리 ===
    // 최초 진입 시 마크다운 내용 설정
    LaunchedEffect(summary) {
        rtState.setMarkdown(sanitizeToAllowedMarkdown(summary))
    }

    // 키보드가 내려가면 포커스를 강제로 해제하여 읽기 모드로 전환
    // 딜레이를 두어 키보드 애니메이션과 충돌하지 않도록 함
    LaunchedEffect(imeVisible) {
        if (!imeVisible && editorFocused) {
            // 키보드가 완전히 내려간 후 포커스 해제
            kotlinx.coroutines.delay(150)
            focusManager.clearFocus(force = true)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("요약 노트") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // 뒤로가기 시 포커스 해제 후 콜백 호출
                            focusManager.clearFocus(force = true)
                            onBack()
                        }
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            // 완료 버튼: 수정된 내용을 전달하고 키보드 내림
                            val markdown = sanitizeToAllowedMarkdown(rtState.toMarkdown())
                            onSummaryChange(markdown)
                            focusManager.clearFocus(force = true)
                            onSave()
                        }
                    ) {
                        Text(
                            text = "완료",
                            style = AppTextStyles.b1_bold_18,
                            color = PurpleMain500
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(editorBounds, toolbarBounds, imeVisible) {
                    // 에디터나 툴바 밖의 영역을 터치하면 포커스 해제
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val touchPosition = down.position

                        val touchedEditor = editorBounds?.contains(touchPosition) == true
                        val touchedToolbar = toolbarBounds?.contains(touchPosition) == true

                        // 키보드가 표시된 상태에서 에디터나 툴바가 아닌 곳을 터치하면 포커스 해제
                        if (imeVisible && !touchedEditor && !touchedToolbar) {
                            focusManager.clearFocus(force = true)
                        }
                    }
                }
        ) {
            // === 메인 콘텐츠 영역 ===
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .windowInsetsPadding(
                        // 키보드 상태에 따라 다른 inset 적용
                        if (imeVisible)
                            WindowInsets.ime.only(WindowInsetsSides.Bottom)
                        else
                            WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                    )
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // === 리치 텍스트 에디터 ===
                // 시스템 기본 선택 툴바를 숨기고 커스텀 툴바 사용
                NoSelectionToolbar {
                    RichTextEditor(
                        state = rtState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp)
                            .background(Grey5)
                            .onFocusChanged { focusState ->
                                editorFocused = focusState.isFocused
                            }
                            .onGloballyPositioned { coordinates ->
                                // 터치 영역 판단을 위한 에디터 경계 저장
                                editorBounds = coordinates.boundsInParent()
                            }
                    )
                }

                HorizontalDivider()

                // === 실시간 마크다운 미리보기 ===
                // 에디터의 내용을 실시간으로 렌더링하여 표시
                val liveMarkdown by remember(rtState) {
                    derivedStateOf { rtState.toMarkdown() }
                }
                val sanitizedMarkdown = remember(liveMarkdown) {
                    sanitizeToAllowedMarkdown(liveMarkdown)
                }

                RichText {
                    Markdown(sanitizedMarkdown)
                }
            }

            // === 마크다운 편집 툴바 ===
            // 에디터에 포커스가 있을 때만 표시 (키보드 상태 무관하게 테스트)
            AnimatedVisibility(
                visible = editorFocused,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(10f), // 다른 요소들 위에 표시
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MarkdownToolbar(
                    rtState = rtState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset {
                            // 키보드가 있을 때만 오프셋 적용
                            IntOffset(0, if (imeVisible) -toolbarOffset else 0)
                        }
                        .onGloballyPositioned { coordinates ->
                            // 터치 영역 판단을 위한 툴바 경계 저장
                            toolbarBounds = coordinates.boundsInParent()
                        }
                )
            }
        }
    }
}

/**
 * 마크다운 편집을 위한 커스텀 툴바
 * 키보드 위에 표시되며 다양한 마크다운 스타일을 적용할 수 있는 버튼들을 제공
 */
@Composable
private fun MarkdownToolbar(
    rtState: RichTextState,
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
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .horizontalScroll(rememberScrollState()), // 버튼이 많을 경우 가로 스크롤 가능
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // H2 제목 버튼
            AssistChip(
                onClick = { toggleHeadingMarkdown(rtState, 2) },
                label = { Text("H2", style = MaterialTheme.typography.titleMedium) }
            )

            // H3 제목 버튼
            AssistChip(
                onClick = { toggleHeadingMarkdown(rtState, 3) },
                label = { Text("H3", style = MaterialTheme.typography.titleSmall) }
            )

            // 볼드 텍스트 버튼
            FilterChip(
                selected = false,
                onClick = { toggleBoldMarkdown(rtState) },
                label = { Text("B", fontWeight = FontWeight.Bold) }
            )

            // 순서 없는 리스트 버튼
            AssistChip(
                onClick = { toggleListForSelection(rtState, ordered = false) },
                label = { Text("• 리스트", style = MaterialTheme.typography.labelMedium) }
            )

            // 순서 있는 리스트 버튼
            AssistChip(
                onClick = { toggleListForSelection(rtState, ordered = true) },
                label = { Text("1. 리스트", style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

/**
 * 시스템 기본 텍스트 선택 툴바를 숨기는 컴포넌트
 * 커스텀 툴바를 사용하기 위해 기본 복사/붙여넣기 툴바를 비활성화
 */
@Composable
private fun NoSelectionToolbar(content: @Composable () -> Unit) {
    val noToolbar = remember {
        object : androidx.compose.ui.platform.TextToolbar {
            override val status = androidx.compose.ui.platform.TextToolbarStatus.Hidden

            override fun showMenu(
                rect: androidx.compose.ui.geometry.Rect,
                onCopyRequest: (() -> Unit)?,
                onPasteRequest: (() -> Unit)?,
                onCutRequest: (() -> Unit)?,
                onSelectAllRequest: (() -> Unit)?
            ) {
                // 기본 툴바 표시하지 않음
            }

            override fun hide() {
                // 툴바 숨김 처리 (빈 구현)
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalTextToolbar provides noToolbar
    ) {
        content()
    }
}
