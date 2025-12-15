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
import com.example.nubo.ui.component.toast.GlobalToastBus
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
    onSavedWithToast: (String) -> Unit,
    viewModel: EditCardViewModel = hiltViewModel()
) {
    // Rich text editor state
    val rtState = rememberRichTextState()

    // Flag for auto numbered list handling
    var isHandlingAutoList by remember { mutableStateOf(false) }

    // ViewModel state
    val uiState by viewModel.uiState.collectAsState()
    val showAiBar by viewModel.showAiBar.collectAsState()
    val aiQuery by viewModel.aiQuery.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val toast by viewModel.toast.collectAsState()
//    val toastHost = LocalAppToastHostState.current
    val canUndo by viewModel.canUndoAiEdit.collectAsState()
    val uiEventFlow = viewModel.uiEvent

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    // Initial markdown from server (used as "original" for change detection)
    var initialMarkdown by rememberSaveable { mutableStateOf<String?>(null) }

    // Track whether user edited before server data arrived
    var hasUserEdited by rememberSaveable { mutableStateOf(false) }

    // Suppress VM sync when editor content is replaced programmatically
    var suppressVmSync by remember { mutableStateOf(false) }

    // Handle one-time UI events
    LaunchedEffect(Unit) {
        uiEventFlow.collect { event ->
            when (event) {
                is EditCardUiEvent.ApplyAiEdit -> {
                    suppressVmSync = true
                    val currentSelection = rtState.selection

                    val normalized = standardizeMarkdown(event.markdown)
                    rtState.setMarkdown(normalized)

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

    // Focus / bounds
    var editorFocused by remember { mutableStateOf(false) }
    var editorBounds by remember { mutableStateOf<Rect?>(null) }
    var toolbarBounds by remember { mutableStateOf<Rect?>(null) }

    val editorFocusRequester = remember { FocusRequester() }

    // Keyboard visibility
    val keyboardVisible by rememberKeyboardVisible()

    val contentBottomInset = when {
        showAiBar -> 300.dp
        editorFocused && keyboardVisible && !showAiBar -> 72.dp + 12.dp
        else -> 100.dp
    }

    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    // Toast bottom padding (현재는 직접 사용하지 않지만 향후 확장용)
    val aiBarHeight = 84.dp
    val mdBarHeight = 64.dp
    val toastBottomPadding = when {
        showAiBar -> aiBarHeight + 16.dp
        editorFocused && keyboardVisible && !showAiBar -> mdBarHeight + 16.dp
        else -> 16.dp
    }

    // Whether current content is different from original
    val hasUnsavedChanges by remember(uiState, initialMarkdown) {
        derivedStateOf {
            val ready = uiState as? EditCardUiState.Ready ?: return@derivedStateOf false
            val init = initialMarkdown ?: return@derivedStateOf false
            standardizeMarkdown(ready.summary) != init
        }
    }

    // Leave confirmation dialog state
    var showLeaveConfirm by rememberSaveable { mutableStateOf(false) }

    // Initialize editor with server data (only when we have not set initialMarkdown yet)
    LaunchedEffect(uiState) {
        val ready = uiState as? EditCardUiState.Ready ?: return@LaunchedEffect
        val normalized = standardizeMarkdown(ready.summary)

        // Only set original value once
        if (initialMarkdown == null) {
            initialMarkdown = normalized

            // If user has not edited yet, we can safely set editor content from server
            if (!hasUserEdited && rtState.toMarkdown() != normalized) {
                suppressVmSync = true
                rtState.setMarkdown(normalized)
                suppressVmSync = false
            }
        }
    }


    LaunchedEffect(rtState) {
        snapshotFlow { rtState.annotatedString.text }   // ← 텍스트 변경을 기준으로 감지
            .collectLatest {
                // 매번 현재 markdown 다시 계산
                val normalized = standardizeMarkdown(rtState.toMarkdown())

                // 사용자가 뭔가 입력했다는 플래그
                if (normalized.isNotEmpty()) {
                    hasUserEdited = true
                }

                if (!suppressVmSync) {
                    (uiState as? EditCardUiState.Ready)?.let { state ->
                        if (state.summary != normalized) {
                            viewModel.updateSummary(normalized)
                        }
                    }
                }
            }
    }


    // Auto numbered list feature
    LaunchedEffect(rtState) {
        snapshotFlow { rtState.annotatedString.text to rtState.selection }
            .collect { (text, selection) ->
                if (isHandlingAutoList) return@collect

                val cursor = selection.end
                if (cursor <= 0 || cursor > text.length) return@collect

                if (text[cursor - 1] != '\n') return@collect

                val prevLineEnd = cursor - 1
                val prevLineStart = text.lastIndexOf('\n', prevLineEnd - 1).let {
                    if (it == -1) 0 else it + 1
                }
                val prevLine = text.substring(prevLineStart, prevLineEnd)

                val match = Regex("^(\\s*)(\\d+)\\.\\s+(.+)$").find(prevLine) ?: return@collect

                val indent = match.groupValues[1]
                val number = match.groupValues[2].toIntOrNull() ?: return@collect
                val nextNumber = number + 1

                val currLineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
                val currLine = text.substring(cursor, currLineEnd)
                if (currLine.isNotEmpty()) return@collect

                val insertText = "$indent$nextNumber. "

                val newText = buildString {
                    append(text.substring(0, cursor))
                    append(insertText)
                    append(text.substring(cursor))
                }

                isHandlingAutoList = true
                suppressVmSync = true

                rtState.setMarkdown(newText)

                val newCursor = cursor + insertText.length
                rtState.selection = TextRange(newCursor)

                suppressVmSync = false
                isHandlingAutoList = false
            }
    }

    // Hide AI bar when keyboard disappears
    LaunchedEffect(keyboardVisible) {
        if (!keyboardVisible && showAiBar) {
            viewModel.toggleAiBar(false)
        }
    }

    LaunchedEffect(toast) {
        val msg = toast ?: return@LaunchedEffect

        // 전역 토스트 버스로 이벤트 전송  딜레이 없음
        GlobalToastBus.showMessage(
            message = msg,
            type = AppToastType.POSITIVE,
            durationMillis = 2000,
            preDelayMillis = 400
        )

        viewModel.consumeToast()
    }


    // Handle system back button
    BackHandler(enabled = !aiLoading) {
        focusManager.clearFocus(force = true)
        if (hasUnsavedChanges) {
            showLeaveConfirm = true
        } else {
            onBack()
        }
    }

    // Leave confirmation dialog
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
                            if (hasUnsavedChanges) {
                                showLeaveConfirm = true
                            } else {
                                onBack()
                            }
                        },
                        enabled = !aiLoading
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (aiLoading) return@TextButton

                            Log.d("EditCardDebug", "━━━ 저장 시작 ━━━")
                            val markdown = standardizeMarkdown(rtState.toMarkdown())
                            Log.d("EditCardDebug", "정규화 후: '$markdown'")

                            // Sync current editor content to ViewModel
                            viewModel.updateSummary(markdown)

                            // Clear focus
                            focusManager.clearFocus(force = true)

                            // Save to server
                            viewModel.save(
                                onSuccess = {
                                    // After successful save, update initialMarkdown
                                    val ready = viewModel.uiState.value as? EditCardUiState.Ready
                                    val canonical = standardizeMarkdown(ready?.summary.orEmpty())
                                    initialMarkdown = canonical

                                    onSavedWithToast("요약 노트 수정이 완료되었어요.")
                                }
                            )
                        },
                        enabled = !aiLoading
                    ) {
                        Text(
                            text = "완료",
                            style = AppTextStyles.b1_bold_18,
                            color = PurpleMain500
                        )
                    }
                },
                modifier = Modifier.drawBehind {
                    val y = size.height
                    drawLine(
                        color = Grey50,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(editorBounds, toolbarBounds) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        val p = down.position
                        val inEditor = editorBounds?.contains(p) == true
                        val inToolbar = toolbarBounds?.contains(p) == true
                        if (inToolbar) return@awaitEachGesture
                        if (!inEditor) focusManager.clearFocus(force = true)
                    }
                }
        ) {
            // Main content
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
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(editorFocusRequester)
                                        .onGloballyPositioned { coords ->
                                            Log.d(
                                                "EditCardDebug",
                                                "에디터 렌더링 크기: ${coords.size}"
                                            )
                                            Log.d(
                                                "EditCardDebug",
                                                "에디터 표시 텍스트: '${rtState.annotatedString.text}'"
                                            )
                                            Log.d(
                                                "EditCardDebug",
                                                "에디터 표시 길이: ${rtState.annotatedString.text.length}"
                                            )
                                        }
                                        .onFocusChanged { fs ->
                                            editorFocused = fs.isFocused
                                            if (fs.isFocused) {
                                                viewModel.toggleAiBar(false)
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // AI loading overlay
            val aiOverlayBottomPadding by animateDpAsState(
                targetValue = if (showAiBar) 100.dp else 0.dp,
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
                Text(
                    "AI가 요약 노트를 다듬고 있어요",
                    style = AppTextStyles.b2_medium_16,
                    color = Grey700
                )
            }

            // Markdown toolbar
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

            // AI prompt bar
            AnimatedVisibility(
                visible = showAiBar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .navigationBarsPadding()
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

            // Floating AI button
            AnimatedVisibility(
                visible = !showAiBar,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .imePadding()
                    .navigationBarsPadding()
                    .zIndex(30f)
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
