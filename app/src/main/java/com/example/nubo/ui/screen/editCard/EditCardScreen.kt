package com.example.nubo.ui.screen.editCard

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey5
import com.example.nubo.ui.theme.PurpleMain500
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.components.toast.AppToastHost
import com.example.components.toast.AppToastLayout
import com.example.components.toast.AppToastType
import com.example.components.toast.LocalAppToastHostState
import com.example.components.toast.rememberAppToastHostState
import com.example.nubo.ui.component.dialog.EditCardAlertDialog
import com.example.nubo.ui.screen.editCard.widgets.AiPromptBar
import com.example.nubo.ui.screen.editCard.widgets.MarkdownToolbar
import com.example.nubo.ui.screen.editCard.widgets.NoSelectionToolbar
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Grey700
import com.example.nubo.utils.standardizeMarkdown
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditCardScreen(
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSaveWithToast: (String) -> Unit,
    viewModel: EditCardViewModel = hiltViewModel()
) {
    // 에디터 상태
    val rtState = rememberRichTextState()

    // 뷰모델 상태
    val uiState by viewModel.uiState.collectAsState()
    val showAiBar by viewModel.showAiBar.collectAsState()
    val aiQuery by viewModel.aiQuery.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val toast by viewModel.toast.collectAsState()
    val toastHost = LocalAppToastHostState.current
    val canUndo by viewModel.canUndoAiEdit.collectAsState()
    val uiEventFlow = viewModel.uiEvent

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val scope = rememberCoroutineScope()
//    val editorBringIntoView = remember { BringIntoViewRequester() }

    var initialMarkdown by rememberSaveable { mutableStateOf("") }
    var currentMarkdown by rememberSaveable { mutableStateOf("") }
    var suppressVmSync by remember { mutableStateOf(false) }


    // AI 편집 적용 시 커서 위치 보존
    LaunchedEffect(Unit) {
        uiEventFlow.collect { event ->
            when (event) {
                is EditCardUiEvent.ApplyAiEdit -> {
                    suppressVmSync = true
                    val currentSelection = rtState.selection

                    val normalized = standardizeMarkdown(event.markdown)
                    rtState.setMarkdown(normalized)
                    currentMarkdown = normalized

                    val newLength = rtState.toMarkdown().length
                    val restoredCursor = currentSelection.end.coerceIn(0, newLength)
                    rtState.selection = TextRange(restoredCursor)

                    suppressVmSync = false
                }
                EditCardUiEvent.HideKeyboard -> {
                    keyboardController?.hide()
                }
            }
        }
    }

    // 포커스/경계
    var editorFocused by remember { mutableStateOf(false) }
    var editorBounds by remember { mutableStateOf<Rect?>(null) }
    var toolbarBounds by remember { mutableStateOf<Rect?>(null) }

    val editorFocusRequester = remember { FocusRequester() }

    // 키보드 표시 여부 관찰
    val keyboardVisible by rememberKeyboardVisible()

    val contentBottomInset = when {
        showAiBar -> 300.dp
        editorFocused && keyboardVisible && !showAiBar -> 72.dp + 12.dp
        else -> 0.dp
    }

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density) // px

    val scrollState = rememberScrollState()
    var rootHeight by remember { mutableStateOf(0) } // px


    LaunchedEffect(
        keyboardVisible,
        editorFocused,
        showAiBar,
        editorBounds,
        rtState.selection, // cursor move also triggers re-centering
        rootHeight,
        imeBottom
    ) {
        // Guard conditions
        if (!keyboardVisible) return@LaunchedEffect
        if (!editorFocused) return@LaunchedEffect
        if (showAiBar) return@LaunchedEffect

        val bounds = editorBounds ?: return@LaunchedEffect
        if (rootHeight == 0) return@LaunchedEffect

        // Visible height = whole content - IME overlay
        val visibleHeight = (rootHeight - imeBottom).coerceAtLeast(0)
        if (visibleHeight == 0) return@LaunchedEffect

        // Editor center in parent's coordinate space
        val editorCenter = bounds.top + bounds.height / 2f

        // We want editorCenter to be at visible center
        val visibleCenter = visibleHeight / 2f

        val currentScroll = scrollState.value.toFloat()
        val diff = editorCenter - visibleCenter
        val targetScroll = (currentScroll + diff)
            .coerceIn(0f, scrollState.maxValue.toFloat())

        // Avoid tiny oscillations
        if (kotlin.math.abs(targetScroll - currentScroll) > 4f) {
            scrollState.animateScrollTo(targetScroll.toInt())
        }
    }

    // 바 높이(대략치) — 토스트를 바 위로 띄우기 위한 패딩
    val aiBarHeight = 84.dp
    val mdBarHeight = 64.dp
    val toastBottomPadding = when {
        showAiBar -> aiBarHeight + 16.dp
        editorFocused && keyboardVisible && !showAiBar -> mdBarHeight + 16.dp
        else -> 16.dp
    }

    var didInit by rememberSaveable { mutableStateOf(false) }

    // 서버에서 받아온 "초기 요약 마크다운"을 저장해두어 변경 여부 판단
    val hasUnsavedChangesState = remember {
        derivedStateOf { currentMarkdown != initialMarkdown }
    }

    // "뒤로가기 확인" 다이얼로그 노출 상태
    var showLeaveConfirm by rememberSaveable { mutableStateOf(false) }

    // 초기화 LaunchedEffect 수정
    LaunchedEffect(uiState, didInit) {
        val ready = uiState as? EditCardUiState.Ready ?: return@LaunchedEffect

        Log.d("EditCardDebug", "━━━ 서버 데이터 수신 ━━━")
        Log.d("EditCardDebug", "원본 summary: '${ready.summary}'")

        val normalized = standardizeMarkdown(ready.summary)

        Log.d("EditCardDebug", "정규화 후: '${normalized}'")

        if (!didInit) {
            if (rtState.toMarkdown() != normalized) {
                Log.d("EditCardDebug", "setMarkdown 호출")
                rtState.setMarkdown(normalized)
            }

            initialMarkdown = normalized
            currentMarkdown = normalized
            didInit = true
        }
    }

    // 2️⃣ 에디터 → VM 동기화 수정
    LaunchedEffect(rtState) {
        snapshotFlow { rtState.toMarkdown() }
            .map { raw -> standardizeMarkdown(raw) }
            .distinctUntilChanged()
            .collectLatest { normalized ->
                currentMarkdown = normalized
                if (!suppressVmSync) {
                    (uiState as? EditCardUiState.Ready)?.let {
                        if (it.summary != normalized) {
                            viewModel.updateSummary(normalized)
                        }
                    }
                }
            }
    }

    // 키보드 내려가면 AI 바 닫기
    LaunchedEffect(keyboardVisible) {
        if (!keyboardVisible && showAiBar) {
            viewModel.toggleAiBar(false)
        }
    }

    // VM의 문자열 토스트를 커스텀 토스트로 라우팅
    LaunchedEffect(toast) {
        toast?.let { msg ->
            delay(400)

            toastHost.show(
                title = AnnotatedString(msg),
                layout = AppToastLayout.TitleOnly,
                type = AppToastType.AI_RESULT,
                durationMillis = 2000
            )
            viewModel.consumeToast()
        }
    }

    // 하드웨어 뒤로가기도 다이얼로그
    BackHandler(enabled = !aiLoading) {
        focusManager.clearFocus(force = true)
        if (hasUnsavedChangesState.value) showLeaveConfirm = true else onBack()
    }

    EditCardAlertDialog(
        visible = showLeaveConfirm,
        onKeepEditing = {
            showLeaveConfirm = false
        },
        onDiscardAndExit = {
            showLeaveConfirm = false
            onBack()
        },
        onDismiss = {
            showLeaveConfirm = false
        }
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = { Text("요약 노트") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            focusManager.clearFocus(force = true)
                            if (hasUnsavedChangesState.value) showLeaveConfirm = true else onBack()
                        },
                        enabled = !aiLoading
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        Log.d("EditCardDebug", "━━━ 저장 시작 ━━━")

                        // standardizeMarkdown 사용
                        val markdown = standardizeMarkdown(rtState.toMarkdown())
                        Log.d("EditCardDebug", "정규화 후: '${markdown}'")

                        viewModel.updateSummary(markdown)
                        initialMarkdown = markdown
                        currentMarkdown = markdown

                        focusManager.clearFocus(force = true)
                        viewModel.save(onSuccess = { onSaveWithToast("요약 노트 수정 완료되었어요.") })
                        onSave()
                    },
                        enabled = !aiLoading
                    ) {
                        Text(text = "완료", style = AppTextStyles.b1_bold_18, color = PurpleMain500)
                    }
                },
                modifier = Modifier.drawBehind {
                    val y = size.height
                    drawLine(
                        color = Grey50,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end   = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            )
        },
    ) { innerPadding ->



        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    // 전체 뷰포트 높이 저장
                    rootHeight = coords.size.height
                }
                .pointerInput(editorBounds, toolbarBounds) {
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
            /* 본문 */
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(bottom = contentBottomInset),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(color = Color.White) {
                    NoSelectionToolbar {
                        NoSelectionToolbar {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .onGloballyPositioned { editorBounds = it.boundsInParent() }
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
                                        //  - min: 기본 높이
                                        //  - max: 키보드 올라와도 화면 안에 들어갈 수 있게 적당히 작은 값
//                                        .heightIn(min = 220.dp, max = 320.dp)
                                        .focusRequester(editorFocusRequester)
//                                        .bringIntoViewRequester(editorBringIntoView)
                                        .onGloballyPositioned { coords ->
                                            Log.d("EditCardDebug", "에디터 렌더링 크기: ${coords.size}")
                                            Log.d("EditCardDebug", "에디터 표시 텍스트: '${rtState.annotatedString.text}'")
                                            Log.d("EditCardDebug", "에디터 표시 길이: ${rtState.annotatedString.text.length}")
                                        } .onFocusChanged { fs ->
                                            // editor focus flag for toolbar and scroll logic
                                            editorFocused = fs.isFocused

                                            if (fs.isFocused) {
                                                // when editor gains focus, AI bar는 닫아두기
                                                viewModel.toggleAiBar(false)
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }

            /* AI 로딩 오버레이 */
            val aiOverlayBottomPadding by animateDpAsState(
                targetValue = when {
                    showAiBar -> 300.dp
                    else -> 0.dp
                },
                animationSpec = tween(
                    durationMillis = 120,
                    easing = FastOutLinearInEasing
                ),
                label = "aiOverlayPadding"
            )

            AiLoadingOverlay(
                visible = aiLoading,
                modifier = Modifier
                    .padding(bottom = aiOverlayBottomPadding)
                    .matchParentSize()
                    .align(Alignment.Center)
                    .zIndex(12f),
                leftDotResId = R.drawable.ai_loading1,
                centerDotResId = R.drawable.ai_loading2,
                rightDotResId = R.drawable.ai_loading3,
                consumeTouch = true
            ) {
                Text("AI가 요약 노트를 다듬고 있어요", style = AppTextStyles.b2_medium_16, color = Grey700)
            }

            /* 마크다운 툴바 */
            AnimatedVisibility(
                visible = editorFocused && keyboardVisible && !showAiBar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    .zIndex(10f),
                enter = fadeIn(animationSpec = tween(50)) +
                    slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(50, easing = FastOutLinearInEasing)
                    ),
                exit = fadeOut(animationSpec = tween(30)) +
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

            /* AI 프롬프트 바 */
            AnimatedVisibility(
                visible = showAiBar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .fillMaxWidth()
                    .zIndex(20f),
                enter = fadeIn(animationSpec = tween(50)) +
                    slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(50, easing = FastOutLinearInEasing)
                    ),
                exit = fadeOut(animationSpec = tween(30)) +
                    slideOutVertically(
                        targetOffsetY = { it / 3 },
                        animationSpec = tween(30, easing = LinearOutSlowInEasing)
                    )
            ) {
                AiPromptBar(
                    value = aiQuery,
                    loading = aiLoading,
                    onValueChange = viewModel::onAiQueryChange,
                    onClose = { if (!aiLoading) viewModel.toggleAiBar(false) },
                    onSubmit = {
                        // AI 제출시도 standardizeMarkdown 사용
                        val md = standardizeMarkdown(rtState.toMarkdown())
                        viewModel.updateSummary(md)
                        viewModel.requestAiEdit()
                    },
                    showAiBar = showAiBar,
                    canUndo = canUndo,
                    onUndo = { if (!aiLoading) viewModel.undoAiEdit() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedVisibility(
                visible = !showAiBar,
                modifier = Modifier
                    .align(Alignment.BottomEnd) // 오른쪽 아래 고정
                    .imePadding()
                    .zIndex(30f) // 토스트보다 낮게
                    .padding(
                        end = 20.dp,
                        bottom = if (editorFocused && keyboardVisible && !showAiBar)
                            70.dp else 60.dp
                    ),
                enter = EnterTransition.None,
                exit = ExitTransition.None
            ) {
                FloatingActionButton(
                    onClick = {
                        if (!aiLoading) viewModel.toggleAiBar(true)
                    },
                    containerColor = Grey5,
                    contentColor = Color.Unspecified,
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
                        tint = Color.Unspecified,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun rememberKeyboardVisible(): State<Boolean> {
    val density = LocalDensity.current
    val ime = WindowInsets.ime

    val isVisible = remember { mutableStateOf(false) }

    LaunchedEffect(ime, density) {
        snapshotFlow { ime.getBottom(density) > 0 }
            .collect { visible -> isVisible.value = visible }
    }
    return isVisible
}
