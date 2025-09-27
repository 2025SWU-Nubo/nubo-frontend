package com.example.nubo.ui.screen.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.text.SpanStyle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nubo.ui.theme.Grey30
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.utils.toggleListForSelection
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


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
    val canUndo by viewModel.canUndoAiEdit.collectAsState()

    val focusManager = LocalFocusManager.current

    // 포커스/경계
    var editorFocused by remember { mutableStateOf(false) }
    var editorBounds by remember { mutableStateOf<Rect?>(null) }
    var toolbarBounds by remember { mutableStateOf<Rect?>(null) }

    // EditCardScreen() 내부 최상단 근처
    val editorFocusRequester = remember { FocusRequester() }   // 에디터 포커스 요청자

    // 키보드 표시 여부 관찰
    val keyboardVisible by rememberKeyboardVisible()

    var keepToolbar by remember { mutableStateOf(false) }      // 툴바 가시성 유지 플래그

    // FAB 가시성 로컬 상태(조건 연동)
    var fabVisible by remember { mutableStateOf(true) }
    LaunchedEffect(showAiBar, editorFocused) {
        fabVisible = !showAiBar && !editorFocused
    }

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

    // 키보드 내려가면 AI 바 닫기
    LaunchedEffect(keyboardVisible) {
        if (!keyboardVisible && showAiBar) {
            viewModel.toggleAiBar(false)
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
                visible =  !showAiBar,
                modifier = Modifier
                    .zIndex(30f)
                    .padding(bottom = if (editorFocused && keyboardVisible && !showAiBar) 72.dp else 0.dp),
                enter = EnterTransition.None,
                exit = ExitTransition.None
            ) {
                FloatingActionButton(
                    onClick = {
//                        fabVisible = false
//                        focusManager.clearFocus(force = true)
                        viewModel.toggleAiBar(true)
                    },
                    containerColor = Grey5,
                    contentColor = Color.Unspecified,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 5.dp
                    ),
                    modifier = Modifier
                        .size(56.dp),

                    shape = CircleShape
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ai_prompt_logo),
                        contentDescription = "AI 요약 편집",
                        tint = Color.Unspecified,
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
                        val down = awaitFirstDown(requireUnconsumed = true)
                        val p = down.position
                        val inEditor = editorBounds?.contains(p) == true
                        val inToolbar = toolbarBounds?.contains(p) == true
                        if (inToolbar) return@awaitEachGesture
                        if (!inEditor) focusManager.clearFocus(force = true)
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
                Surface( color = Color.White ) {
                    NoSelectionToolbar {
                        NoSelectionToolbar {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)                // ← 좌우 여백 여기!
                                    .onGloballyPositioned { editorBounds = it.boundsInParent() } // ← 패딩 포함한 영역을 에디터로 간주
                            ) {
                                RichTextEditor(
                                    state = rtState,
                                    colors = RichTextEditorDefaults.richTextEditorColors(
                                        containerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 220.dp)
                                        .focusRequester(editorFocusRequester)
                                        .onFocusChanged { fs ->
                                            editorFocused = fs.isFocused
                                            if (fs.isFocused) viewModel.toggleAiBar(false)
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // ── 마크다운 툴바: 에디터 포커스 && AI 바 닫힘 ──
            AnimatedVisibility(
                visible = editorFocused && keyboardVisible && !showAiBar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
//                    .imePadding()
                    .zIndex(10f),
                enter = fadeIn(animationSpec = tween(50)) +
                    slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(50, easing = FastOutLinearInEasing)
                    ),
                exit  = fadeOut(animationSpec = tween(30)) +
                    slideOutVertically(
                        targetOffsetY = { it / 3 },
                        animationSpec = tween(30, easing = LinearOutSlowInEasing)
                    )
            ) {
                MarkdownToolbar(
                    rtState = rtState,
                    editorFocusRequester = editorFocusRequester,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { toolbarBounds = it.boundsInParent() }
                )
            }

            // 키보드를 내리면 ai 프롬 프트 바가 보이지 않도록 추가
            // ── AI 프롬프트 바: FAB로 열릴 때만 ──
            AnimatedVisibility(
                visible = showAiBar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
//                    .imePadding()
                    .fillMaxWidth()
                    .zIndex(20f),
                enter = fadeIn(animationSpec = tween(50)) +
                    slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(50, easing = FastOutLinearInEasing)
                    ),
                exit  = fadeOut(animationSpec = tween(30)) +
                    slideOutVertically(
                        targetOffsetY = { it / 3 },
                        animationSpec = tween(30, easing = LinearOutSlowInEasing)
                    )
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
                    canUndo = canUndo,
                    onUndo = {viewModel.undoAiEdit()},
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
    editorFocusRequester: FocusRequester,
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
            fun focusThen(action: () -> Unit) {
                editorFocusRequester.requestFocus() // ← 포커스/선택 유지
                action()
            }

            AssistChip(
                onClick = { focusThen { toggleHeadingMarkdown(rtState, 2) } },
                label = { Text("H2", style = MaterialTheme.typography.titleMedium) }
            )
            AssistChip(
                onClick = { focusThen { toggleHeadingMarkdown(rtState, 3) } },
                label = { Text("H3", style = MaterialTheme.typography.titleSmall) }
            )
            FilterChip(
                selected = false,
                onClick = { focusThen { rtState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) } },
                label = { Text("B", fontWeight = FontWeight.Bold) }
            )
            AssistChip(
                onClick = { focusThen { rtState.toggleUnorderedList() } },
                label = { Text("• 리스트", style = MaterialTheme.typography.labelMedium) }
            )
            AssistChip(
                onClick = { focusThen { rtState.toggleOrderedList() } },
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
    canUndo: Boolean,            // 되돌리기 가능 여부
    onUndo: () -> Unit,          // 되돌리기 실행
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    // 내부 편집 상태를 TextFieldValue로 보관하여 커서 위치 제어
    var tfv by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }

    // 외부 value 변경을 내부 tfv에 반영
    // 외부 값이 바뀌었으면 커서를 끝으로 이동
    LaunchedEffect(value) {
        if (value != tfv.text) {
            tfv = tfv.copy(text = value, selection = TextRange(value.length))
        }
    }

    // 바가 열릴 때만 포커스 요청 → 키보드 자동 표시
    LaunchedEffect(showAiBar) {
        if (showAiBar) focusRequester.requestFocus()
    }

    var selectedPreset by remember { mutableStateOf<Int?>(null) }  //프리셋 칩 인덱스 상태
    val presets = remember {
        listOf("➔➔ 더 간결하게", "↔ 더 자세하게", "✎ 핵심만 하이라이트")
    }

    // 전송 가능 여부를 단일 상태로 관리
    val canSend by remember(loading, value) {
        mutableStateOf(!loading && value.isNotBlank())
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
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                presets.forEachIndexed { index, text ->
                    val cleaned = text.replace(Regex("^([➔↔✎ ]+)"), "")
                    val selected = selectedPreset == index

                    AssistChip(
                        onClick = {
                            val next = "$cleaned "
                            tfv = TextFieldValue(text = next, selection = TextRange(next.length)) // 커서 끝
                            onValueChange(next)
                            selectedPreset = index
                            focusRequester.requestFocus() // IME 유지
                        },
//                            val next = if (tfv.text.isBlank()) cleaned else "${tfv.text} $cleaned "
//                            tfv = TextFieldValue(text = next, selection = TextRange(next.length))
//                            onValueChange(next)
//                            selectedPreset = index  //선택 상태 갱신
//                            focusRequester.requestFocus()
                        label = {
                            Text(
                                text = cleaned,
                                style = if (selected) AppTextStyles.label_SemiBold_12 else AppTextStyles.label_medium_12, // 라벨 텍스트 스타일 적용
//                                color = MaterialTheme.colorScheme.onSurface, // 필요 시 색상 명시
                            )
                        },
                        leadingIcon = { Text(text.take(2)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selected) Purple100 else Grey30,
                            labelColor = if (selected) PurpleMain500 else Color.Black,
                            leadingIconContentColor = PurpleMain500,
                        ),
//                        colors = AssistChipDefaults.assistChipColors(
//                            containerColor = if(selected) Purple100 else Grey10,
//                            labelColor = if(selected) PurpleMain500 else Color.Black,
//                            leadingIconContentColor = PurpleMain500
//                        ),
                        // 선택 시 보라색 테두리
                        border = if (selected) BorderStroke(1.dp, PurpleMain500) else null,

//                        leadingIcon = { Text(text.take(2)) },
                        shape = RoundedCornerShape(45.dp)
                    )
                }

                /* 되돌리기 칩 */
//                AssistChip(
//                    onClick = {
//                        if (canUndo) {
//                            onUndo()
//                            selectedPreset = null
//                            focusRequester.requestFocus() // 키보드 유지
//                        }
//                    },
//                    enabled = canUndo,
//                    label = {  },
//                    leadingIcon = {
//                        Icon(
//                            painter = painterResource(R.drawable.replay),
//                            contentDescription = "되돌리기",
//                            tint = Color.Unspecified // 원본 색 유지(필요 없으면 제거)
//                        )
//                    }, // 필요시 리소스로 교체
//                    colors = AssistChipDefaults.assistChipColors(
//                        disabledContainerColor = Grey30,
//                        containerColor = Purple100,
//                        leadingIconContentColor = if(canUndo)PurpleMain500 else GreyMain300,
//                        labelColor = if(canUndo)PurpleMain500 else Color.Black
//                    ),
//                    border = if (canUndo) BorderStroke(1.dp, PurpleMain500) else null,
//                    shape = RoundedCornerShape(45.dp)
//                )
//            }

                IconOnlyChip(
                    enabled = canUndo,
                    onClick = {
                        onUndo()
                        selectedPreset = null
                        focusRequester.requestFocus() // 키보드 유지
                    },
                    containerColor = if (canUndo) Purple100 else Grey30,
                    contentColor = if (canUndo) PurpleMain500 else GreyMain300,
                    borderColor = if (canUndo) PurpleMain500 else null,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.replay), // SVG → Vector로 임포트된 아이콘
                            contentDescription = "되돌리기",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
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

                Box(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    TextField(
                        value = tfv,
                        onValueChange = { newValue ->
                            tfv = newValue
                            onValueChange(newValue.text)
                            selectedPreset = null // 사용자가 수정하면 칩 선택 해제
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = {Text(if (loading) "AI가 편집 중입니다..." else "더 간결하게 요약해줘.") },
                        singleLine = true,
                        enabled = true,
                        readOnly = loading,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor =Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (canSend){
                                    onSubmit()
                                    focusRequester.requestFocus()
                                }
                            }
                        )
                    )

                    if (loading){
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.White.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ){
                            Row (
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ){
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Al가 편집 중입니다...",
                                    style = AppTextStyles.b3_regular_14,
                                    color = Grey50
                                )
                            }
                        }
                    }

                }
                Spacer(Modifier.width(12.dp))

//                val sendContainerColor = if (canSend) PurpleMain500 else GreyMain100
                val sendContentColor = if (canSend) PurpleMain500 else GreyMain300


                FilledIconButton(
                    onClick = {
                        if(canSend) {
                            onSubmit()
                            focusRequester.requestFocus()
                        } },
                    enabled = true,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = sendContentColor
                    )
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = sendContentColor
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

@Composable
private fun IconOnlyChip(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color? = null,
    icon: @Composable () -> Unit,
) {
    Surface(
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        shape = RoundedCornerShape(45.dp),
        color = containerColor,
        contentColor = contentColor,
        border = borderColor?.let { BorderStroke(1.dp, it) },
        modifier = modifier
    ) {
        // 패딩만 최소로 — 여백 생기는 원인 제거
        Box(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            icon()
        }
    }
}


