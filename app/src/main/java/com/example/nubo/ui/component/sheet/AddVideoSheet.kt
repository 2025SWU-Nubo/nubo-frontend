package com.example.nubo.ui.component.sheet

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey0
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.PurpleMain500
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.example.nubo.ui.screen.add.SheetTopToast
import com.example.nubo.ui.theme.Grey20
import com.example.nubo.ui.theme.PinkError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.components.toast.AppToastType
import com.example.nubo.ui.screen.cardupload.CardUploadViewModel
import com.example.nubo.ui.theme.AppTextStyles.b2_bold_16
import com.example.nubo.ui.theme.GreyMain100
import com.example.nubo.data.service.CardUploadService

@Composable
fun AddVideoSheet(
    onClose: () -> Unit,
    viewModel: AddVideoViewModel = hiltViewModel(),          // ViewModel with video validation
    cardUploadViewModel: CardUploadViewModel = hiltViewModel(), // Existing upload ViewModel
    showToast: (String, AppToastType, Int) -> Unit = { _, _, _ -> }
) {
    // Page state
    var page by remember { mutableStateOf(SheetPage.SAVE_VIDEO) }

    // Text input state using TextFieldState (for custom contentPadding)
    val textState = remember { TextFieldState() }

    // Selected board/section IDs
    var checkedIds by rememberSaveable { mutableStateOf(setOf<String>()) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showError by rememberSaveable { mutableStateOf(false) }   // Error highlight flag
    val shake = remember { Animatable(0f) }                       // Shake animation

    // Toast visibility flags
    var toastVisible by rememberSaveable { mutableStateOf(false) }          // Invalid link toast
    var networkErrorToastVisible by rememberSaveable { mutableStateOf(false) } // Network error toast

    // Board toast (AI auto classification guide)
    var boardToastVisible by rememberSaveable { mutableStateOf(false) }
    var boardToastShown by rememberSaveable { mutableStateOf(false) }       // Show only once

    // Validation state
    val validateState by viewModel.state.collectAsStateWithLifecycle(
        AddVideoViewModel.ValidateState.Idle
    )
    // Boards tree state
    val boardsState by viewModel.boards.collectAsStateWithLifecycle(
        AddVideoViewModel.BoardsState.Idle
    )

    // Shake on error
    LaunchedEffect(showError) {
        if (showError) {
            shake.snapTo(0f)
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 420
                    -8f at 60 using LinearEasing
                    8f at 120 using LinearEasing
                    -6f at 180 using LinearEasing
                    6f at 240 using LinearEasing
                    -3f at 300 using LinearEasing
                    3f at 360 using LinearEasing
                    0f at 420
                }
            )
        }
    }

    // Trigger boards loading when entering PICK_BOARD page
    LaunchedEffect(page) {
        if (page == SheetPage.PICK_BOARD && !boardToastShown) {
            boardToastVisible = true
            boardToastShown = true
            viewModel.loadBoards()
        }
    }

    // React to validation state changes
    LaunchedEffect(validateState) {
        when (validateState) {
            is AddVideoViewModel.ValidateState.Success -> {
                // Valid URL → go to board picker
                page = SheetPage.PICK_BOARD
                showError = false
                toastVisible = false
            }

            AddVideoViewModel.ValidateState.Invalid -> {
                // Invalid URL → shake + invalid link toast
                showError = true
                toastVisible = true
                scope.launch {
                    delay(3000)
                    showError = false
                    clearText(textState)
                }
            }

            is AddVideoViewModel.ValidateState.Error -> {
                // Network/server error → shake + network error toast
                showError = true
                networkErrorToastVisible = true
            }

            AddVideoViewModel.ValidateState.Loading,
            AddVideoViewModel.ValidateState.Idle -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 0.dp)
        ) {
            IconButton(
                onClick = {
                    // Reset all sheet state
                    page = SheetPage.SAVE_VIDEO
                    checkedIds = emptySet()
                    boardToastShown = false
                    boardToastVisible = false
                    toastVisible = false
                    networkErrorToastVisible = false
                    clearText(textState)
                    viewModel.resetForNewSession()
                    onClose()
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "닫기",
                    tint = Grey500
                )
            }

            Text(
                text = when (page) {
                    SheetPage.SAVE_VIDEO -> "영상 추가하기"
                    SheetPage.PICK_BOARD -> "보드 선택"
                },
                style = AppTextStyles.b1_semibold_18,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp)
        ) {
            when (page) {
                SheetPage.SAVE_VIDEO -> {
                    val currentText = textState.text.toString()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // URL input field
                        OutlinedTextField(
                            state = textState,
                            lineLimits = TextFieldLineLimits.SingleLine,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .offset(x = shake.value.dp),
                            shape = RoundedCornerShape(28.dp),
                            textStyle = AppTextStyles.b3_medium_14,
                            placeholder = {
                                Text(
                                    text = "링크를 입력하세요",
                                    style = AppTextStyles.b3_medium_14,
                                    color = Grey200
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (showError) Grey50 else Grey50,
                                unfocusedBorderColor = if (showError) Grey50 else Grey50,
                                focusedContainerColor = if (showError) PinkError else Color.White,
                                unfocusedContainerColor = if (showError) PinkError else Grey10
                            ),
                            // Custom inner paddings
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = 10.dp,
                                end = 16.dp,
                                bottom = 10.dp
                            )
                        )

                        // Reset error highlight when user edits text
                        LaunchedEffect(textState.text) {
                            if (showError) showError = false
                        }

                        // "추가" button
                        Button(
                            onClick = {
                                viewModel.validate(currentText.trim())
                            },
                            enabled = currentText.isNotBlank() &&
                                validateState !is AddVideoViewModel.ValidateState.Loading,
                            modifier = Modifier.height(42.dp),
                            shape = RoundedCornerShape(28.dp),
                            border = if (currentText.isNotBlank()) null else BorderStroke(1.dp, Grey50),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurpleMain500,
                                contentColor = Grey0,
                                disabledContainerColor = Grey10,
                                disabledContentColor = Grey1000
                            )
                        ) {
                            if (validateState is AddVideoViewModel.ValidateState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Grey0
                                )
                            } else {
                                Text("추가", style = AppTextStyles.b3_medium_14)
                            }
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                }

                SheetPage.PICK_BOARD -> {
                    val fixedHeight = 340.dp

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(fixedHeight)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            when (boardsState) {
                                AddVideoViewModel.BoardsState.Idle,
                                AddVideoViewModel.BoardsState.Loading -> {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .fillMaxWidth()
                                    )
                                }

                                is AddVideoViewModel.BoardsState.Error -> {
                                    Text(
                                        text = "보드 목록을 불러오지 못했어요.\n 다시 시도해주세요.",
                                        style = AppTextStyles.b3_medium_14,
                                        color = Grey500,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(16.dp)
                                    )
                                }

                                is AddVideoViewModel.BoardsState.Loaded -> {
                                    val tree =
                                        (boardsState as AddVideoViewModel.BoardsState.Loaded).boards

                                    if (tree.isEmpty()) {
                                        EmptyBoardsState(
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            contentPadding = PaddingValues(vertical = 8.dp)
                                        ) {
                                            items(tree, key = { it.id }) { node ->
                                                BoardNodeItem(
                                                    node = node.toUi(),
                                                    level = 0,
                                                    isChecked = { id -> checkedIds.contains(id) },
                                                    onCheckedChange = { id, isOn ->
                                                        checkedIds = if (isOn) checkedIds + id else checkedIds - id
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val urlToUpload =
                                    viewModel.rememberedUrl ?: textState.text.toString().trim()

                                val selectedIds: List<Long> = checkedIds
                                    .mapNotNull { it.toLongOrNull() }
                                    .distinct()

                                val rawToken = viewModel.getAccessTokenOrNull()
                                if (rawToken.isNullOrEmpty()) {
                                    showToast("로그인이 필요해요", AppToastType.NEGATIVE, 2000)
                                    return@Button
                                }

                                CardUploadService.startService(
                                    context = context,
                                    videoUrl = urlToUpload,
                                    boardId = selectedIds.firstOrNull()
                                )

                                page = SheetPage.SAVE_VIDEO
                                checkedIds = emptySet()
                                boardToastShown = false
                                boardToastVisible = false
                                toastVisible = false
                                networkErrorToastVisible = false
                                clearText(textState)
                                viewModel.resetForNewSession()
                                onClose()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurpleMain500,
                                contentColor = Grey0
                            )
                        ) {
                            Text("추가하기", style = AppTextStyles.b1_bold_18)
                        }
                    }
                }
            }
        }
    }

    // Invalid link toast
    if (toastVisible) {
        val annotatedTitle = buildAnnotatedString {
            append("잘못된 링크!")
        }
        SheetTopToast(
            title = annotatedTitle,
            message = "유효하지 않은 링크입니다.\n영상 URL 확인 후 다시 입력해주세요.",
            visible = toastVisible,
            onDismiss = { toastVisible = false },
            durationMillis = 3000L,
            bottomOffset = 220.dp
        )
    }

    // Network error toast
    if (networkErrorToastVisible) {
        val annotatedTitle = buildAnnotatedString { append("네트워크 오류") }
        SheetTopToast(
            title = annotatedTitle,
            message = "네트워크 오류로 잠시 후 다시 시도해주세요.",
            visible = networkErrorToastVisible,
            onDismiss = { networkErrorToastVisible = false },
            durationMillis = 3000L,
            bottomOffset = 220.dp
        )
    }

    // Board guide toast
    if (boardToastVisible) {
        val fixedHeight = 340.dp
        val offsetFromBottom = fixedHeight + 150.dp

        val annotatedTitle = buildAnnotatedString {
            append("선택하지 않아도 ")
            withStyle(SpanStyle(color = PurpleMain500)) {
                append("AI가 자동 분류")
            }
            append("해줘요.")
        }

        SheetTopToast(
            title = annotatedTitle,
            message = "AI가 영상 내용을 자동으로 분석해\n적절한 보드 안에 저장해요.",
            visible = boardToastVisible,
            onDismiss = { boardToastVisible = false },
            durationMillis = 180_000L,
            bottomOffset = offsetFromBottom
        )
    }

    // Reset when sheet is disposed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetForNewSession()
        }
    }
}

// Helper to clear TextFieldState text
private fun clearText(state: TextFieldState) {
    state.edit {
        if (length > 0) {
            delete(0, length)
        }
    }
}

private enum class SheetPage { SAVE_VIDEO, PICK_BOARD }

// ------------------------- Board selection components -------------------------

private data class BoardNode(
    val id: String,
    val title: String,
    val children: List<BoardNode> = emptyList(),
    val selectable: Boolean = true
)

private fun UiBoardNode.toUi(): BoardNode {
    return BoardNode(
        id = id.toString(),
        title = title,
        children = children.map { it.toUi() }
    )
}

@Composable
private fun BoardNodeItem(
    node: BoardNode,
    level: Int,
    isChecked: (String) -> Boolean,
    onCheckedChange: (String, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val hasChildren = node.children.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val arrowSize = Modifier.size(24.dp)

            if (hasChildren) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_down),
                    contentDescription = if (expanded) "접기" else "펼치기",
                    modifier = arrowSize
                        .clickable { expanded = !expanded }
                        .graphicsLayer { rotationZ = if (expanded) 0f else 180f },
                    tint = Color.Unspecified
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_down),
                    contentDescription = null,
                    modifier = arrowSize
                        .alpha(0f)
                        .clearAndSetSemantics { },
                    tint = Color.Unspecified
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = node.title,
                style = AppTextStyles.b3_medium_14,
                color = Grey1000,
                modifier = Modifier.weight(1f)
            )

            val checkedParent = isChecked(node.id)
            val parentIconRes =
                if (checkedParent) R.drawable.ic_add_fill_checkbox
                else R.drawable.ic_add_blank_check_box

            Icon(
                painter = painterResource(id = parentIconRes),
                contentDescription = if (checkedParent) "선택됨" else "선택",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onCheckedChange(node.id, !checkedParent) },
                tint = Color.Unspecified
            )

            Spacer(Modifier.width(32.dp))
        }

        if (hasChildren && expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(Grey20)
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        val y = size.height - strokeWidth / 2
                        drawLine(
                            color = GreyMain100,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeWidth
                        )
                    }
            ) {
                node.children.forEach { child ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 32.dp,
                                end = 16.dp,
                                top = 14.dp,
                                bottom = 14.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = child.title,
                            style = AppTextStyles.b3_medium_14,
                            color = Grey1000,
                            modifier = Modifier.weight(1f)
                        )

                        val checkedChild = isChecked(child.id)
                        val iconRes =
                            if (checkedChild) R.drawable.ic_add_fill_checkbox
                            else R.drawable.ic_add_blank_check_box

                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = if (checkedChild) "선택됨" else "선택",
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onCheckedChange(child.id, !checkedChild) },
                            tint = Color.Unspecified
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyBoardsState(
    modifier: Modifier = Modifier,
    title: String = "보드가 아직 없어요",
    subtitle: String = "먼저 보드를 만들거나\nAI 자동 분류를 사용해보세요!"
) {
    val iconRes = R.drawable.error_face

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = "이모지",
            tint = Color.Unspecified
        )

        Text(
            text = title,
            style = b2_bold_16,
            color = Grey1000,
            textAlign = TextAlign.Center
        )

        Text(
            text = subtitle,
            style = AppTextStyles.b3_regular_14,
            color = Grey1000,
            textAlign = TextAlign.Center
        )
    }
}
