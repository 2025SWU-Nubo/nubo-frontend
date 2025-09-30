package com.example.nubo.ui.screen.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.nubo.R
import com.example.nubo.data.dto.HighlightDto
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Grey20
import com.example.nubo.ui.theme.Grey5
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.GreyMain100
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.utils.sanitizeToAllowedMarkdown
import com.example.nubo.utils.toggleBoldMarkdown
import com.example.nubo.utils.toggleHeadingMarkdown
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditCardScreen(
    onBack: () -> Unit,
    onSave: () -> Unit,
    viewModel: EditCardViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    // 에디터 상태
    val rtState = rememberRichTextState()

    // 뷰모델 상태
    val uiState by viewModel.uiState.collectAsState()
    val showAiBar by viewModel.showAiBar.collectAsState()
    val aiQuery by viewModel.aiQuery.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val toast by viewModel.toast.collectAsState()

    val focusManager = LocalFocusManager.current

    // 포커스/경계
    var editorFocused by remember { mutableStateOf(false) }
    var editorBounds by remember { mutableStateOf<Rect?>(null) }
    var toolbarBounds by remember { mutableStateOf<Rect?>(null) }

    // 키보드 닫힘 열림 여부 상태
    val keyboardVisible by rememberKeyboardVisible()

    // 스낵바
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toast) {
        toast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeToast()
        }
    }

    // 뷰모델 Ready → 에디터 세팅
    LaunchedEffect(uiState) {
        val ready = uiState as? EditCardUiState.Ready ?: return@LaunchedEffect
        val current = rtState.toMarkdown()
        val target = sanitizeToAllowedMarkdown(ready.summary)
        if (current != target) rtState.setMarkdown(target)
    }

    // 에디터 → 뷰모델 동기화
    LaunchedEffect(rtState) {
        snapshotFlow { rtState.toMarkdown() }
            .collectLatest { md ->
                (uiState as? EditCardUiState.Ready)?.let {
                    if (it.summary != md) viewModel.updateSummary(md)
                }
            }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),


        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("요약 노트") },
                navigationIcon = {
                    IconButton(onClick = {
                        focusManager.clearFocus(force = true)
                        onBack()
                    }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val markdown = sanitizeToAllowedMarkdown(rtState.toMarkdown())
                        viewModel.updateSummary(markdown)
                        focusManager.clearFocus(force = true)
                        onSave()
                    }) {
                        Text(text = "완료", style = AppTextStyles.b1_bold_18, color = PurpleMain500)
                    }
                }
            )
        },

        // ── FAB: 간소화된 위치 계산 ──
        floatingActionButton = {
            AnimatedVisibility(
                visible =  !editorFocused && !showAiBar,
                modifier = Modifier.zIndex(30f)
            ) {
                FloatingActionButton(
                    onClick = {
                        focusManager.clearFocus(force = true)
                        viewModel.toggleAiBar(true)
                    },
                    containerColor = Grey5,
                    contentColor = androidx.compose.ui.graphics.Color.Unspecified,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 5.dp
                    ),
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ai_prompt_logo),
                        contentDescription = "AI 요약 편집",
                        tint = androidx.compose.ui.graphics.Color.Unspecified,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        },



        ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
//                .padding(innerPadding)
//                .consumeWindowInsets(innerPadding)
                .pointerInput(editorBounds, toolbarBounds) {
                    // 에디터/툴바 외 영역 터치 시 포커스 해제
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val p = down.position
                        val inEditor = editorBounds?.contains(p) == true
                        val inToolbar = toolbarBounds?.contains(p) == true
                        if (!inEditor && !inToolbar) {
                            focusManager.clearFocus(force = true)
                        }
                    }
                },
        ) {
            // 본문: 기본 패딩만 적용
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 시스템 기본 선택 툴바 숨김
                Surface(
                    color = Color.White
                ) {
                    NoSelectionToolbar {
                        RichTextEditor(
                            state = rtState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp)
                                .background(Color.White)
                                .onFocusChanged { fs ->
                                    editorFocused = fs.isFocused
                                    if (fs.isFocused) {
                                        // 에디터 포커스 시 AI 바 닫기
                                        viewModel.toggleAiBar(false)
                                    }
                                }
                                .onGloballyPositioned { editorBounds = it.boundsInParent() }
                        )
                    }
                }

                HorizontalDivider()

                // 라이브 미리보기
                val liveMd by remember(rtState) { derivedStateOf { rtState.toMarkdown() } }
                val sanitized = remember(liveMd) { sanitizeToAllowedMarkdown(liveMd) }
                RichText { Markdown(sanitized) }
            }

            // ── 마크다운 툴바: 에디터 포커스 && AI 바 닫힘 ──
            AnimatedVisibility(
                visible = editorFocused && !showAiBar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
//                    .imePadding()
                    .zIndex(10f),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                MarkdownToolbar(
                    rtState = rtState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { toolbarBounds = it.boundsInParent() }
                )
            }

            // 키보드를 내리면 ai 프롬 프트 바가 보이지 않도록 추가
            // ── AI 프롬프트 바: FAB로 열릴 때만 ──
            AnimatedVisibility(
                visible = showAiBar && keyboardVisible,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
//                    .imePadding()
                    .fillMaxWidth()
                    .zIndex(20f),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AiPromptBar(
                    value = aiQuery,
                    loading = aiLoading,
                    onValueChange = viewModel::onAiQueryChange,
                    onClose = { viewModel.toggleAiBar(false) },
                    onSubmit = {
                        viewModel.updateSummary(rtState.toMarkdown())
                        viewModel.requestAiEdit()
                    },
                    showAiBar = showAiBar,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
    }
}

/* ───────────────────────────────────────────────────────────────
   마크다운 편집 툴바
   ─────────────────────────────────────────────────────────────── */
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
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = { toggleHeadingMarkdown(rtState, 2) },
                label = { Text("H2", style = MaterialTheme.typography.titleMedium) }
            )
            AssistChip(
                onClick = { toggleHeadingMarkdown(rtState, 3) },
                label = { Text("H3", style = MaterialTheme.typography.titleSmall) }
            )
            FilterChip(
                selected = false,
                onClick = { toggleBoldMarkdown(rtState) },
                label = { Text("B", fontWeight = FontWeight.Bold) }
            )
            AssistChip(
                onClick = { runCatching { rtState.toggleUnorderedList() } },
                label = { Text("• 리스트", style = MaterialTheme.typography.labelMedium) }
            )
            AssistChip(
                onClick = { runCatching { rtState.toggleOrderedList() } },
                label = { Text("1 리스트", style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

/* ───────────────────────────────────────────────────────────────
   AI 프롬프트 입력바 (프리셋 칩 + 입력 + 전송)
   ─────────────────────────────────────────────────────────────── */
@Composable
private fun AiPromptBar(
    value: String,
    loading: Boolean,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit,
    onSubmit: () -> Unit,
    showAiBar: Boolean,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    var selectedPreset by remember { mutableStateOf<Int?>(null) }  //프리셋 칩 인덱스 상태

    val presets = remember {
        listOf("➔➔ 더 간결하게", "↔ 더 자세하게", "✎ 핵심만 하이라이트")
    }

    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        color = Grey5,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 프리셋 칩 영역
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presets.forEachIndexed { index, text ->
                    val cleaned = text.replace(Regex("^([➔↔✎ ]+)"), "")
                    val selected = selectedPreset == index

                    presets.forEach { text ->
                        AssistChip(
                            onClick = {
                                val next = if (value.isBlank()) cleaned else "$value $cleaned "
                                onValueChange(next)
                                selectedPreset = index  //선택 상태 갱신
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Grey50,
                                labelColor = Color.Black,
                                leadingIconContentColor = PurpleMain500
                            ),
                            // 선택 시 보라색 테두리
                            border = if (selected) BorderStroke(1.dp, PurpleMain500) else null,
                            label = {
                                Text(
                                    text = cleaned,
                                    style = AppTextStyles.label_medium_12, // 라벨 텍스트 스타일 적용
//                                color = MaterialTheme.colorScheme.onSurface, // 필요 시 색상 명시
                                )
                            },
                            leadingIcon = { Text(text.take(2)) },
                            shape = RoundedCornerShape(45.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ai_prompt_logo),
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.Unspecified,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    TextField(
                        value = value,
                        onValueChange = {
                            onValueChange(it)
                            if(it.isBlank()) selectedPreset = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        placeholder = { Text("더 간결하게 요약해줘.") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor =Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { if (!loading && value.isNotBlank()) onSubmit() }
                        ),
                        enabled = !loading
                    )

                    Spacer(Modifier.width(12.dp))

                    FilledIconButton(
                        onClick = onSubmit,
                        enabled = !loading,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = if (value.isNotBlank()) PurpleMain500 else GreyMain300
                        )
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                painterResource(R.drawable.ai_prompt_send),
                                contentDescription = "전송",
                                tint = Color.Unspecified
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ───────────────────────────────────────────────────────────────
   시스템 기본 텍스트 선택 툴바 숨김
   ─────────────────────────────────────────────────────────────── */
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
            ) { /* 표시 안 함 */ }
            override fun hide() { /* 없음 */ }
        }
    }
    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalTextToolbar provides noToolbar
    ) { content() }
}

@Composable
fun rememberKeyboardVisible(): State<Boolean> {
    // 한글 주석: 키보드(IME) 영역의 하단값을 픽셀로 가져오기 위해 Density 필요
    val density = LocalDensity.current
    // 한글 주석: Compose에서 제공하는 IME 인셋
    val ime = WindowInsets.ime

    // 한글 주석: 외부에서 관찰 가능한 가시성 상태
    val isVisible = remember { mutableStateOf(false) }

    // 한글 주석: 인셋 하단값이 0 초과이면 키보드가 올라온 상태로 판단
    LaunchedEffect(ime, density) {
        snapshotFlow { ime.getBottom(density) > 0 }
            .collect { visible -> isVisible.value = visible }
    }
    return isVisible
}

