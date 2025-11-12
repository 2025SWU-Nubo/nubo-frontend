
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
import com.example.components.toast.rememberAppToastHostState
import com.example.nubo.ui.component.dialog.EditCardAlertDialog
import com.example.nubo.ui.screen.editCard.widgets.AiPromptBar
import com.example.nubo.ui.screen.editCard.widgets.MarkdownToolbar
import com.example.nubo.ui.screen.editCard.widgets.NoSelectionToolbar
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Grey700
import com.example.nubo.utils.canonicalizeMarkdown
import com.example.nubo.utils.debugFullPipeline
import com.example.nubo.utils.debugMarkdownNormalization
import com.example.nubo.utils.debugNewLines
import com.example.nubo.utils.debugRichTextState
import com.example.nubo.utils.demoteNestedOrderedToBullets
import com.example.nubo.utils.sanitizeToAllowedMarkdown
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.example.nubo.utils.shimListBoldBug
import com.example.nubo.utils.stripShimForServer



@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditCardScreen(
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSaveWithToast: (String) -> Unit,  // 저장 성공 시 상세 화면에서 토스트를 띄우도록 메시지 전달
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
    val uiEventFlow = viewModel.uiEvent

    val focusManager = LocalFocusManager.current

    val scope = rememberCoroutineScope() // + NEW: for bringIntoView()

    // + NEW: bring-into-view requester for the editor
    val editorBringIntoView = remember { BringIntoViewRequester() }

    // 상태 먼저
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

                    // 1) AI 응답 마크다운을 정규화 후 세팅
                    val canon = canonicalizeMarkdown(event.markdown)
                    val noNestedOrder = demoteNestedOrderedToBullets(canon)
                    // 2) 에디터에 넣기 직전 파서 보호
                    val safe = shimListBoldBug(canon)
                    rtState.setMarkdown(safe)
                    currentMarkdown = canonicalizeMarkdown(stripShimForServer(rtState.toMarkdown()))

                    // 2) 커서 복원
                    val newLength = rtState.toMarkdown().length
                    val restoredCursor = currentSelection.end.coerceIn(0, newLength)
                    rtState.selection = TextRange(restoredCursor)

                    suppressVmSync = false
                }
            }
        }
    }



    // 포커스/경계
    var editorFocused by remember { mutableStateOf(false) }
    var editorBounds by remember { mutableStateOf<Rect?>(null) }
    var toolbarBounds by remember { mutableStateOf<Rect?>(null) }

    // EditCardScreen() 내부 최상단 근처
    val editorFocusRequester = remember { FocusRequester() }   // 에디터 포커스 요청자

    // 키보드 표시 여부 관찰
    val keyboardVisible by rememberKeyboardVisible()

    var keepToolbar by remember { mutableStateOf(false) }      // 툴바 가시성 유지 플래그

    // 하단 패딩(툴바 보정) 계산값을 Column에도 적용해서 마지막 줄까지 보이도록 하기
    val contentBottomInset = when {
        showAiBar -> 300.dp     // AiPromptBar + 여유
        editorFocused && keyboardVisible && !showAiBar -> 64.dp + 12.dp // MarkdownBar + 여유
        else -> 0.dp
    }

    // 키보드가 올라오면(=visible) 포커스 중일 때 에디터를 뷰포트로 스크롤
    LaunchedEffect(keyboardVisible, editorFocused, showAiBar) {
        if (keyboardVisible && editorFocused) {
            withFrameNanos { /* wait one frame for IME insets */ }
            scope.launch { editorBringIntoView.bringIntoView() }
        }
    }

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

    var didInit by rememberSaveable { mutableStateOf(false) }

// 서버에서 받아온 "초기 요약 마크다운"을 저장해두어 변경 여부 판단
    val hasUnsavedChangesState = remember {
        derivedStateOf { currentMarkdown != initialMarkdown }
    }

    // "뒤로가기 확인" 다이얼로그 노출 상태
    var showLeaveConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState, didInit) {
        val ready = uiState as? EditCardUiState.Ready ?: return@LaunchedEffect
        // ✅ 디버깅 1: 서버에서 받은 원본 확인
        Log.d("EditCardDebug", "━━━ 서버 데이터 수신 ━━━")
        Log.d("EditCardDebug", "원본 summary: '${ready.summary}'")
        Log.d("EditCardDebug", "원본 길이: ${ready.summary.length}")
        debugNewLines(ready.summary, "서버 원본")

        val target = canonicalizeMarkdown(ready.summary)
        val noNestedOrder = demoteNestedOrderedToBullets(target)
        val safeForEditor = shimListBoldBug(target)

        // ✅ 디버깅 2: 정규화 후 확인
        Log.d("EditCardDebug", "정규화 후: '${target}'")
        Log.d("EditCardDebug", "정규화 후 길이: ${target.length}")
        debugMarkdownNormalization(ready.summary, "초기 로드")

        if (!didInit) {
            if (rtState.toMarkdown() != safeForEditor){
                Log.d("EditCardDebug", "setMarkdown 호출 전 rtState: '${rtState.toMarkdown()}'")
                rtState.setMarkdown(safeForEditor)
                Log.d("EditCardDebug", "setMarkdown 호출 후 rtState: '${rtState.toMarkdown()}'")

                // ✅ 디버깅 3: 설정 후 RichTextState 상태 확인
                debugRichTextState(rtState, "초기 설정 후")
            }

            val canonical = canonicalizeMarkdown(stripShimForServer(rtState.toMarkdown()))
            initialMarkdown = canonical
            currentMarkdown = canonical

            // ✅ 디버깅 4: 전체 파이프라인 확인
            debugFullPipeline(ready.summary, rtState, "초기화 완료")
            didInit = true
        }
    }

    // 에디터 → 뷰모델 동기화 블록 교체
    LaunchedEffect(rtState) {
        snapshotFlow { rtState.toMarkdown() }
            .map { raw -> canonicalizeMarkdown(stripShimForServer(raw)) }
            .distinctUntilChanged()                 // ← 추가
            .collectLatest { canon ->
                currentMarkdown = canon
                if (!suppressVmSync) {
                    (uiState as? EditCardUiState.Ready)?.let {
                        if (it.summary != canon) {
                            viewModel.updateSummary(canon)
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

    // 하드웨어 뒤로가기도 다이얼로그
    BackHandler(enabled = !aiLoading) {
        focusManager.clearFocus(force = true)
        if (hasUnsavedChangesState.value) showLeaveConfirm = true else onBack()
    }

    EditCardAlertDialog(
        visible = showLeaveConfirm,
        onKeepEditing = {
            // 계속 편집 → 다이얼로그만 닫기
            showLeaveConfirm = false
        },
        onDiscardAndExit = {
            // 변경 폐기 후 나가기
            // 필요시 VM 초기화가 있다면 호출: viewModel.discardChanges()
            showLeaveConfirm = false
            onBack()
        },
        onDismiss = {
            // 바깥 클릭/백 등으로 닫힘
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
                            // 뒤로가기 눌렀을 떄 변경사항 있을 경우 경고 다이얼로그 노출
                            focusManager.clearFocus(force = true)
                            if (hasUnsavedChangesState.value) showLeaveConfirm = true else onBack()
                        },
                        enabled = !aiLoading //  AI 응답 대기 중에는 뒤로가기 비활성화
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        // ✅ 디버깅 6: 저장 전 상태 확인
                        Log.d("EditCardDebug", "━━━ 저장 시작 ━━━")
                        val beforeCanonical = rtState.toMarkdown()
                        Log.d("EditCardDebug", "정규화 전: '${beforeCanonical}'")

                        val markdown = canonicalizeMarkdown(stripShimForServer(beforeCanonical))
                        Log.d("EditCardDebug", "정규화 후: '${markdown}'")
                        debugMarkdownNormalization(beforeCanonical, "저장 시")
                        viewModel.updateSummary(markdown)

                        initialMarkdown = markdown
                        currentMarkdown = markdown

                        // 저장 요청
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
                    // 상단바 하단 구분선
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

        // ── FAB: 간소화된 위치 계산 ──
        floatingActionButton = {
            AnimatedVisibility(
                visible =  !showAiBar,
                modifier = Modifier
                    .imePadding()
                    .zIndex(30f)
                    .padding(bottom = if (editorFocused && keyboardVisible && !showAiBar) 68.dp else 45.dp),
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

            // 본문: 기본 패딩만 적용
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(bottom = contentBottomInset),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 시스템 기본 선택 툴바 숨김
                Surface( color = Color.White ) {
                    NoSelectionToolbar {
                        NoSelectionToolbar {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)                // 좌우 여백
                                    .onGloballyPositioned { editorBounds = it.boundsInParent() } // 패딩 포함한 영역을 에디터로 간주
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
                                        .bringIntoViewRequester(editorBringIntoView)
                                        .onGloballyPositioned { coords ->
                                            // ✅ 디버깅 5: 실제 렌더링 크기 확인
                                            Log.d("EditCardDebug", "에디터 렌더링 크기: ${coords.size}")
                                            Log.d("EditCardDebug", "에디터 표시 텍스트: '${rtState.annotatedString.text}'")
                                            Log.d("EditCardDebug", "에디터 표시 길이: ${rtState.annotatedString.text.length}")
                                        }
                                        .onFocusChanged { fs ->
                                            editorFocused = fs.isFocused
                                            if (fs.isFocused) {
                                                // AI 바는 닫고, 에디터가 보이도록 스크롤
                                                viewModel.toggleAiBar(false)
                                                // English: Scroll the editor into view on focus
                                                scope.launch {
                                                    withFrameNanos { /* wait one frame for IME insets */ }
                                                    editorBringIntoView.bringIntoView()
                                                }
                                            }
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
                    showAiBar -> 300.dp  // aiBarHeight + 여유
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
                    onClose = { if (!aiLoading) viewModel.toggleAiBar(false) },
                    onSubmit = {
                        val md = canonicalizeMarkdown(stripShimForServer(rtState.toMarkdown()))
                        viewModel.updateSummary(md)
                        viewModel.requestAiEdit()
                    },
                    showAiBar = showAiBar,
                    canUndo = canUndo,
                    onUndo = { if (!aiLoading) viewModel.undoAiEdit() },
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
