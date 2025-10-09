package com.example.nubo.ui.screen.myBoard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles.b1_bold_18
import com.example.nubo.ui.theme.AppTextStyles.b1_semibold_18
import com.example.nubo.ui.theme.AppTextStyles.b2_bold_16
import com.example.nubo.ui.theme.AppTextStyles.b2_semibold_16
import com.example.nubo.ui.theme.AppTextStyles.b3_medium_14
import com.example.nubo.ui.theme.AppTextStyles.b3_regular_14
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey20
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.GreyMain100
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.PurpleMain500


// [추가] BoardAction을 이 파일로 이동하여 공용으로 사용
enum class BoardAction { COPY, MOVE }

// 슬롯 기반으로 변경된 바텀 바
@Composable
fun SelectionBottomBar(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    showBoardSelector: Boolean,
    boardSelectorContent: @Composable () -> Unit,
    actionsContent: @Composable () -> Unit
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
                .animateContentSize(), // 내용물 크기 변경 시 애니메이션
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            shadowElevation = 8.dp,
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

// 기본 액션 버튼 UI (삭제, 복제, 이동)
@Composable
fun ActionsContent(
    selectedSectionCount: Int,
    selectedCardCount: Int,
    onDeleteClick: () -> Unit,
    onCopyClick: () -> Unit,
    onMoveClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val title = when {
            selectedSectionCount > 0 && selectedCardCount > 0 -> "${selectedSectionCount}개의 섹션과 ${selectedCardCount}개의 카드 선택됨"
            selectedSectionCount > 0 -> "${selectedSectionCount}개의 섹션 선택됨"
            selectedCardCount > 0 -> "${selectedCardCount}개의 카드 선택됨"
            else -> "항목 선택"
        }
        Text(text = title, style = b1_semibold_18)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 50.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val isEnabled = selectedSectionCount > 0 || selectedCardCount > 0
            SelectionButton(modifier = Modifier.weight(1f), text = "삭제", iconRes = R.drawable.ic_board_delete, enabled = isEnabled, onClick = onDeleteClick)
            SelectionButton(modifier = Modifier.weight(1f), text = "복제", iconRes = R.drawable.ic_board_copy, enabled = isEnabled, onClick = onCopyClick)
            SelectionButton(modifier = Modifier.weight(1f), text = "이동", iconRes = R.drawable.ic_board_move, enabled = isEnabled, onClick = onMoveClick)
        }
    }
}


// 선택 모드 바텀시트 버튼
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
        modifier = modifier.height(40.dp),
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

// --- 보드 선택 UI 관련 헬퍼 함수들 ---

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

// BoardNodeItem 호출부 AddVideoSheet와 동일
@Composable
fun BoardSelectionSheetContent(
    action: BoardAction,
    boardsState: BoardDetailViewModel.BoardsState,
    onBack: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(painterResource(id = R.drawable.ic_arrow_back), contentDescription = "뒤로가기") }
            Text(text = "보드 선택", style = b1_semibold_18, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
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
                BoardDetailViewModel.BoardsState.Loading -> { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) }
                else -> {}
            }
        }
        Button(
            onClick = { onConfirm(selectedId) },
            enabled = selectedId != null, // 하나라도 선택해야 버튼 활성화
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
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


// AddVideoSheet.kt의 완성된 UI 코드로 교체
@Composable
private fun BoardNodeItem(
    node: BoardNode,
    level: Int,
    isSelected: (String) -> Boolean,
    onItemSelect: (String) -> Unit // 파라미터 변경
) {
    var expanded by remember { mutableStateOf(true) }
    val hasChildren = node.children.isNotEmpty()

    Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
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
                        Text(text = child.title, style = b3_medium_14, color = Grey1000, modifier = Modifier.weight(1f))
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

// AddVideoSheet.kt의 완성된 UI 코드로 교체
@Composable
fun EmptyBoardsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(painter = painterResource(id = R.drawable.error_face), contentDescription = "이모지", tint = Color.Unspecified)
        Text(text = "보드가 아직 없어요", style = b2_bold_16, color = Grey1000, textAlign = TextAlign.Center)
        Text(text = "먼저 보드를 만들거나\nAI 자동 분류를 사용해보세요!", style = b3_regular_14, color = Grey1000, textAlign = TextAlign.Center)
    }
}
