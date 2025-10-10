package com.example.nubo.ui.screen.editCard

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey5
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.utils.sanitizeToAllowedMarkdown
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.components.toast.AppToastHost
import com.example.components.toast.AppToastHostState
import com.example.components.toast.AppToastLayout
import com.example.components.toast.AppToastType
import com.example.components.toast.rememberAppToastHostState
import com.example.nubo.ui.screen.editCard.widgets.AiPromptBar
import com.example.nubo.ui.screen.editCard.widgets.MarkdownToolbar
import com.example.nubo.ui.screen.editCard.widgets.NoSelectionToolbar
import com.example.nubo.ui.theme.Grey700
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditCardScreen(
    onBack: () -> Unit,
    onSave: () -> Unit,
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
    val toastHost = rememberAppToastHostState()
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

    // 바 높이(대략치) — 토스트를 바 위로 띄우기 위한 패딩
    val aiBarHeight = 84.dp      // AiPromptBar 높이(+여유)
    val mdBarHeight = 64.dp      // MarkdownToolbar 높이(+여유)
    val toastBottomPadding = when {
        showAiBar -> aiBarHeight + 16.dp
        editorFocused && keyboardVisible && !showAiBar -> mdBarHeight + 16.dp
        else -> 16.dp
    }


    // FAB 가시성 로컬 상태(조건 연동)
    var fabVisible by remember { mutableStateOf(true) }
    LaunchedEffect(showAiBar, editorFocused) {
        fabVisible = !showAiBar && !editorFocused
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

    // VM의 문자열 토스트를 커스텀 토스트로 라우팅
    LaunchedEffect(toast) {
        toast?.let { msg ->
            // ... type 계산 생략 ...
            toastHost.show(
                title = AnnotatedString(msg),
                layout = AppToastLayout.TitleOnly,
                type = AppToastType.AI_RESULT,
                durationMillis = 2000
            )
            viewModel.consumeToast()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets.statusBars,
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
                    .imePadding()
                    .zIndex(30f)
                    .padding(bottom = if (editorFocused && keyboardVisible && !showAiBar) 64.dp else 30.dp),
                enter = EnterTransition.None,
                exit = ExitTransition.None
            ) {
                FloatingActionButton(
                    onClick = {
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
            /* 토스트 */
            AppToastHost(
                hostState = toastHost,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // 키보드(IME) 인셋을 반영해서 "항상 키보드 위"에 위치
                    .imePadding()
                    // 네비게이션 바 있는 기기에서 하단 소프트키 위로
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .zIndex(100f)
                    // AI바/마크다운바 위로 조금 더 띄우는 추가 여백만 계산
                    .padding(
                        bottom = when {
                            showAiBar -> 130.dp  // aiBarHeight + 여유
                            editorFocused && keyboardVisible && !showAiBar -> 64.dp + 12.dp   // mdBarHeight + 여유
                            else -> 12.dp
                        }
                    )
            )
//            AppToastOverlay(hostState = toastHost, modifier =
//                Modifier.padding(
//                bottom = when {
//                    showAiBar -> 130.dp  // aiBarHeight + 여유
//                    editorFocused && keyboardVisible && !showAiBar -> 64.dp + 12.dp   // mdBarHeight + 여유
//                    else -> 12.dp
//                }
//                ))

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

            // 3) AI 처리 오버레이
            //  애니메이션 추가
            val aiOverlayBottomPadding by animateDpAsState(
                targetValue = when {
                    showAiBar -> 200.dp  // aiBarHeight + 여유
                    else -> 0.dp
                },
                animationSpec = tween(
                    durationMillis = 150,
                    easing = FastOutLinearInEasing
                ),
                label = "aiOverlayPadding"
            )

            AiLoadingOverlay(
                visible = aiLoading,
                modifier = Modifier
                    .padding(bottom = aiOverlayBottomPadding)
                    .matchParentSize()            // Box 꽉 채움
                    .align(Alignment.Center)
                    .zIndex(12f),
                consumeTouch = true
            ) {
                Text("AI가 요약 노트를 다듬고 있어요", style = AppTextStyles.b2_medium_16, color = Grey700)
            }


            // ── 마크다운 툴바: 에디터 포커스 && AI 바 닫힘 ──
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
                    .imePadding()
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
fun AppToastOverlay(hostState: AppToastHostState,modifier: Modifier) {
    Popup(
        alignment = Alignment.BottomCenter,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            excludeFromSystemGesture = false
        )
    ) {
        AppToastHost(
            hostState = hostState,
            matchParentSize = false,
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .imePadding()                         // ← 키보드 위
                .windowInsetsPadding(WindowInsets.navigationBars) // ← 소프트키 위
//                .padding(bottom = 12.dp)              // 약간의 여유
        )
    }
}





