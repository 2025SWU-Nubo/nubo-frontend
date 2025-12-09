package com.example.nubo.ui.screen.myBoard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.nubo.R
import com.example.nubo.ui.component.noRippleClickable
import com.example.nubo.ui.theme.AppTextStyles.b1_bold_18
import com.example.nubo.ui.theme.AppTextStyles.b1_semibold_18
import com.example.nubo.ui.theme.AppTextStyles.b2_bold_16
import com.example.nubo.ui.theme.AppTextStyles.b2_semibold_16
import com.example.nubo.ui.theme.AppTextStyles.b3_medium_14
import com.example.nubo.ui.theme.AppTextStyles.b3_regular_14
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey20
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.ui.theme.RedError

/** 섹션 및 카드 항목 선택과 관련하여 수행되는 기능들에 대한 ui 파일
 * 섹션 및 카드 항목 선택 - 삭제, 복제, 이동 */

// 선택 모드 바텀바 슬롯
@Composable
fun SelectionBottomBar(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    showBoardSelector: Boolean,
    boardSelectorContent: @Composable () -> Unit,
    actionsContent: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .animateContentSize(), // 내용물 크기 변경 시 애니메이션
            color = Color.White
        ) {
            if (showBoardSelector) {
                boardSelectorContent()
            } else {
                actionsContent()
            }
        }
    }
}

// 선택모드 바텀바 내부 콘텐츠
@Composable
fun ActionsContent(
    selectedSectionCount: Int,
    selectedCardCount: Int,
    onDeleteClick: () -> Unit,
    onCopyClick: () -> Unit,
    onMoveClick: () -> Unit,
    onCancelClick: () -> Unit,
    onBack: () -> Unit,
    showBackButton: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 24.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val title = when {
            selectedSectionCount > 0 && selectedCardCount > 0 -> "${selectedSectionCount}개의 섹션과 ${selectedCardCount}개의 카드 선택됨"
            selectedSectionCount > 0 -> "${selectedSectionCount}개의 섹션 선택됨"
            selectedCardCount > 0 -> "${selectedCardCount}개의 카드 선택됨"
            else -> "항목 선택"
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // --- 조건부 뒤로가기 버튼 ---
            if (showBackButton) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "뒤로가기"
                    )
                }
            } else {
                // 뒤로가기 버튼이 없을 때는 균형 맞추기 위해 동일한 크기의 Spacer
                Spacer(modifier = Modifier.width(48.dp))
            }

            // 타이틀 (정중앙)
            Text(
                text = title,
                style = b1_semibold_18,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(48.dp))

            /*// --- 조건부 닫기 버튼 ---
            if (!showBackButton) {
                // 닫기(X) 아이콘 버튼 — 오른쪽으로 이동
                IconButton(
                    onClick = onCancelClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "닫기"
                    )
                }
            }else {
                // 닫기 버튼이 없을 때는 균형 맞추기 위해 동일한 크기의 Spacer
                Spacer(modifier = Modifier.width(48.dp))
            }*/
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 50.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val isEnabled = selectedSectionCount > 0 || selectedCardCount > 0
            SelectionButton(
                modifier = Modifier.weight(1f),
                text = "삭제",
                iconRes = R.drawable.ic_board_delete,
                enabled = isEnabled,
                onClick = onDeleteClick
            )
            SelectionButton(
                modifier = Modifier.weight(1f),
                text = "복제",
                iconRes = R.drawable.ic_board_copy,
                enabled = isEnabled,
                onClick = onCopyClick
            )
            SelectionButton(
                modifier = Modifier.weight(1f),
                text = "이동",
                iconRes = R.drawable.ic_board_move,
                enabled = isEnabled,
                onClick = onMoveClick
            )
        }

    }
}


// 선택 모드 액션 버튼
@Composable
private fun SelectionButton(
    modifier: Modifier = Modifier,
    text: String,
    iconRes: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (enabled) Purple100 else Grey20
    val contentColor = if (enabled) PurpleMain500 else GreyMain300

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(40.dp),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = backgroundColor,
            disabledContentColor = contentColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            // Row가 버튼의 전체 너비를 차지하도록 하여 중앙 정렬
            modifier = Modifier.fillMaxWidth(),
            // Row 내부의 아이템들을 가로(수평) 방향으로 중앙에 배치
            horizontalArrangement = Arrangement.Center,
            // Row 내부의 아이템들을 세로(수직) 방향으로 중앙에 배치
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = text,
            )
            // 아이콘과 텍스트 사이에 가로 간격
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = b2_semibold_16
            )
        }
    }
}

// 섹션 및 카드 삭제 시 확인 다이얼로그
@Composable
fun DeleteConfirmationDialog(
    visible: Boolean,
    selectedCardCount: Int,
    selectedSectionCount: Int,
    onDismiss: () -> Unit,
    onDelete: () -> Unit  //  삭제
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .navigationBarsPadding()
            ) {
                // 메인 다이얼로그 (삭제)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(14.dp)) // 모서리를 14.dp로 변경
                        .background(Color.White)
                        .clickable( // 배경 클릭 전파 방지
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val title = when {
                        selectedSectionCount > 0 && selectedCardCount > 0 -> "삭제를 클릭하면 선택한 모든 콘텐츠가 삭제됩니다.\n(❗️섹션 내부 카드 포함)"
                        selectedSectionCount > 0 -> "삭제를 클릭하면 섹션 내 모든 콘텐츠가 삭제됩니다."
                        else -> "삭제를 클릭하면 선택한 모든 카드가 삭제됩니다."
                    }
                    Text(
                        text = title,
                        style = b3_regular_14, // 폰트 스타일 변경
                        color = Grey500, // 텍스트 색상 변경
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 16.dp) // 패딩 조정
                    )
                    Divider(color = Grey50)
                    Text(
                        text = "삭제",
                        style = b1_semibold_18,
                        color = RedError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .noRippleClickable { onDelete() }
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 취소 버튼
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "취소",
                        style = b1_semibold_18,
                        color = PurpleMain500,
                        modifier = Modifier
                            .fillMaxWidth()
                            .noRippleClickable { onDismiss() }
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/* --- 복제 및 이동 시 보드 목록 화면 UI --- */
// 보드 선택 UI 관련 헬퍼 함수들
private data class BoardNode(
    val id: String,
    val title: String,
    val children: List<BoardNode> = emptyList(),
)

private fun UiBoardNode.toUi(): BoardNode {
    return BoardNode(
        id = id.toString(),
        title = title,
        children = children.map { it.toUi() }
    )
}

// 보드 선택 바텀바 콘텐츠
@Composable
fun BoardSelectionSheetContent(
    action: BoardAction,
    boardsState: BoardDetailViewModel.BoardsState,
    onBack: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "뒤로가기"
                )
            }
            Text(
                text = "보드 선택",
                style = b1_semibold_18,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                // weight(1f) 대신 heightIn을 사용하여 최대 높이를 340.dp로 제한
                // 내용이 340.dp보다 적으면 그만큼만 차지하고, 많아지면 340.dp 내에서 스크롤
                .heightIn(max = 340.dp)
        ) {
            when (boardsState) {
                is BoardDetailViewModel.BoardsState.Loaded -> {
                    val tree = boardsState.boards.map { it.toUi() }
                    if (tree.isEmpty()) {
                        EmptyBoardsState(modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) { // LazyColumn은 주어진 공간 안에서 스크롤됩니다.
                            items(tree, key = { it.id }) { node ->
                                BoardNodeItem(
                                    node = node,
                                    level = 0,
                                    isSelected = { id -> selectedId == id },
                                    onItemSelect = { id ->
                                        selectedId = if (selectedId == id) null else id
                                    }
                                )
                            }
                        }
                    }
                }

                BoardDetailViewModel.BoardsState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                else -> {}
            }
        }
        Button(
            onClick = { onConfirm(selectedId) },
            enabled = selectedId != null, // 하나라도 선택해야 버튼 활성화
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PurpleMain500,
                contentColor = Color.White,
                disabledContainerColor = Grey200, // 비활성화 시 색상
                disabledContentColor = Color.White
            )
        ) {
            val buttonText = if (action == BoardAction.COPY) "붙여넣기" else "이동하기"
            Text(text = buttonText, style = b1_bold_18, color = Color.White)
        }
    }
}

// 보드 선택 노드 아이템
@Composable
private fun BoardNodeItem(
    node: BoardNode,
    level: Int,
    isSelected: (String) -> Boolean,
    onItemSelect: (String) -> Unit // 파라미터 변경
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
                .clickable { onItemSelect(node.id) } // 행 전체를 클릭 가능하게 변경
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_down),
                contentDescription = if (expanded) "접기" else "펼치기",
                modifier = Modifier
                    .size(24.dp)
                    .alpha(if (hasChildren) 1f else 0f)
                    .clickable(enabled = hasChildren) { expanded = !expanded }
                    .graphicsLayer { rotationZ = if (expanded) 0f else -90f },
                tint = Color.Unspecified
            )
            Spacer(Modifier.width(8.dp))
            Text(text = node.title, style = b3_medium_14, color = Grey1000, modifier = Modifier.weight(1f))

            // [수정] UI는 그대로, clickable 로직만 변경
            val isCurrentlySelected = isSelected(node.id)
            Icon(
                painter = painterResource(id = if (isCurrentlySelected) R.drawable.ic_add_fill_checkbox else R.drawable.ic_add_blank_check_box),
                contentDescription = if (isCurrentlySelected) "선택됨" else "선택",
                modifier = Modifier.size(24.dp), // clickable은 부모 Row로 이동했으므로 제거
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
                    .drawBehind { /* ... (구분선 로직은 동일) */ }
            ) {
                node.children.forEach { child ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemSelect(child.id) } // 행 전체를 클릭 가능하게 변경
                            .padding(start = 32.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = child.title,
                            style = b3_medium_14,
                            color = Grey1000,
                            modifier = Modifier.weight(1f)
                        )
                        val isChildSelected = isSelected(child.id)
                        Icon(
                            painter = painterResource(id = if (isChildSelected) R.drawable.ic_add_fill_checkbox else R.drawable.ic_add_blank_check_box),
                            contentDescription = if (isChildSelected) "선택됨" else "선택",
                            modifier = Modifier.size(24.dp), // clickable은 부모 Row로 이동했으므로 제거
                            tint = Color.Unspecified
                        )
                    }
                }
            }
        }
    }
}

// 빈 보드일 때 UI
@Composable
fun EmptyBoardsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.error_face),
            contentDescription = "이모지",
            tint = Color.Unspecified
        )
        Text(text = "보드가 아직 없어요", style = b2_bold_16, color = Grey1000, textAlign = TextAlign.Center)
        Text(
            text = "먼저 보드를 만들거나\nAI 자동 분류를 사용해보세요!",
            style = b3_regular_14,
            color = Grey1000,
            textAlign = TextAlign.Center
        )
    }
}
