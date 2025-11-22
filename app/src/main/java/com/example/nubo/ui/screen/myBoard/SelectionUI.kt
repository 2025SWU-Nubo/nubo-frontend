package com.example.nubo.ui.screen.myBoard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.nubo.R
import com.example.nubo.data.model.InvitationDto
import com.example.nubo.data.model.MemberDto
import com.example.nubo.domain.model.InviteUser
import com.example.nubo.model.navigation.BottomNavItem.Companion.items
import com.example.nubo.ui.component.noRippleClickable
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.AppTextStyles.b1_bold_18
import com.example.nubo.ui.theme.AppTextStyles.b1_semibold_18
import com.example.nubo.ui.theme.AppTextStyles.b2_bold_16
import com.example.nubo.ui.theme.AppTextStyles.b2_semibold_16
import com.example.nubo.ui.theme.AppTextStyles.b3_medium_14
import com.example.nubo.ui.theme.AppTextStyles.b3_regular_14
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey20
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.GreyMain100
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.Purple50
import com.example.nubo.ui.theme.Purple700
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.ui.theme.RedError
import androidx.compose.material3.OutlinedTextField as OutlinedTextField1


// BoardAction을 이 파일로 이동하여 공용으로 사용
enum class BoardAction { COPY, MOVE }

// 어떤 바텀 시트가 보이는지 관리하기 위한 enum 추가
enum class BottomSheetType {
    NONE,           // 아무것도 안 보임
    SELECTION,      // 기존 선택 모드
    BOARD_SETTINGS, // 보드 설정 (삭제, 설정)
    BOARD_EDIT, // 보드 설정 전체 화면
    SECTION_SETTINGS,// 섹션 설정 (이름 변경)
    INVITE, // 초대 화면
    BOARD_MEMBERS // 참여자 목록 확인
}

// 슬롯 기반으로 변경된 바텀 바
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
    onMoveClick: () -> Unit,
    onCancelClick: () -> Unit
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
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // 닫기(X) 아이콘 버튼
            IconButton(
                onClick = onCancelClick,
                modifier = Modifier.size(48.dp) // 충분한 터치 영역 확보
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close), // ic_close.xml 아이콘 사용
                    contentDescription = "선택 취소"
                )
            }

            Text(
                text = title,
                style = b1_semibold_18,
                modifier = Modifier.weight(1f), // 남은 공간을 모두 차지
                textAlign = TextAlign.Center
            )
            // Spacer를 왼쪽에 두어 제목을 중앙으로
            Spacer(modifier = Modifier.width(48.dp))
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

// 보드 옵션
@Composable
fun BoardSettingsContent(
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit,
    selectedCardCount: Int,
    selectedBoardCount: Int
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp, // 입체 효과
        color = Color.White
    ) {
        val title = when {
            selectedBoardCount > 0 -> "${selectedBoardCount}개의 보드 선택됨"
            selectedCardCount > 0 -> "${selectedCardCount}개의 카드 선택됨"
            else -> "항목 선택"
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.padding(start = 4.dp, top = 10.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "닫기"
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            val isEnabled = selectedBoardCount > 0 || selectedCardCount > 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp, start = 20.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = b1_semibold_18
                )
                Spacer(modifier = Modifier.weight(1f))
                OptionButton(
                    text = "삭제",
                    iconRes = R.drawable.ic_board_delete,
                    onClick = onDeleteClick,
                    enabled = isEnabled
                )
            }
        }
    }
}

@Composable
fun SectionSettingsContent(
    currentName: String,
    isCurrentlyShared: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (newName: String, isShared: Boolean) -> Unit,
    modifier: Modifier

) {
    // --- 입력값 및 유효성 검사 상태 ---
    var name by rememberSaveable { mutableStateOf(currentName) }
    var isShared by rememberSaveable { mutableStateOf(isCurrentlyShared) }
    var isNameTouched by rememberSaveable { mutableStateOf(false) }


    val trimmedName = name.trim()
    val isNameValid = trimmedName.length >= 2
    val showError = isNameTouched && !isNameValid
    // -----------------

    Surface(
        modifier = Modifier
            .imePadding()
            .fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp, // 입체 효과
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .background(color = Color.White)
                .height(300.dp)
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 헤더: 뒤로가기 + 타이틀 ---
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-18).dp)
                        .size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "닫기"
                    )
                }
                Text(text = "섹션 설정", style = b1_semibold_18) // 타이틀 변경
            }
            Spacer(Modifier.height(28.dp))

            // --- 보드 이름 입력 ---
            Column(horizontalAlignment = Alignment.Start) {
                Text("섹션 이름", style = b2_semibold_16)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField1(
                    value = name,
                    onValueChange = {
                        name = it
                        if (!isNameTouched) isNameTouched = true
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(40.dp),
                    isError = showError,
                    // CreateBoardSheet와 동일한 스타일 적용
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleMain500,
                        unfocusedBorderColor = Grey50,
                        errorBorderColor = RedError,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Grey10,
                        disabledBorderColor = Grey50, // 비활성화 시 테두리 색
                        disabledTextColor = GreyMain300,   // 비활성화 시 텍스트 색
                        disabledContainerColor = Grey10
                    ),
                    placeholder = { Text("섹션 이름", style = b3_regular_14, color = Grey200) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (isNameValid) onConfirm(trimmedName, isShared)
                    })
                )

                // --- 유효성 및 안내 메시지 ---
                Box(modifier = Modifier.height(24.dp)) {
                    if (showError) {  //유효성 검사 실패 시
                        Text(
                            text = "섹션 이름을 2자 이상 입력해주세요.",
                            style = b3_regular_14,
                            color = RedError,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                Spacer(modifier = Modifier.weight(1f)) // 버튼을 하단에 고정

                // --- 변경하기 버튼 ---
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    onClick = {
                        isNameTouched = true
                        if (isNameValid) {
                            onConfirm(trimmedName, isShared)
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    enabled = isNameValid, // 유효성 검사 결과에 따라 활성화
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleMain500,
                        disabledContainerColor = Grey200, // 비활성화 색상
                        contentColor = Color.White, // 활성화 시 텍스트 색상
                        disabledContentColor = Color.White // 비활성화 시 텍스트 색상
                    )
                ) {
                    Text(text = "변경하기", style = b1_bold_18) // 버튼 텍스트 변경
                }
                Spacer(Modifier.height(25.dp))
            }
        }
    }
}

// 보드/섹션 옵션 바텀바 버튼
@Composable
private fun OptionButton(
    text: String,
    iconRes: Int,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val backgroundColor = if (enabled) Purple100 else Grey20
    val contentColor = if (enabled) PurpleMain500 else GreyMain300

    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.height(40.dp), // 고정 높이
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = backgroundColor,
            disabledContentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 12.dp) // 내부 여백
    ) {
        Row(
            // fillMaxWidth가 없어서 내용물 크기에 맞게 조절됩니다.
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = text,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = b2_semibold_16
            )
        }
    }
}


// 섹션 및 카드 삭제 다이얼로그
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
                .background(Color.Black.copy(alpha = 0.5f))
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

// --- 보드 전체 삭제할 때 사용하는 확인 다이얼로그 ---
@Composable
fun BoardDeleteConfirmationDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
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
                    // 요청하신 텍스트로 변경
                    Text(
                        text = "삭제를 클릭하면 보드 내 모든 콘텐츠가 삭제됩니다.",
                        style = b3_regular_14, // 폰트 스타일 변경
                        color = Grey500, // 텍스트 색상 변경
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 22.dp) // 패딩 조정
                    )
                    Divider(color = Grey50)
                    Text(
                        text = "삭제",
                        style = b1_semibold_18,
                        color = RedError, // 빨간색 텍스트
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
                        style = b1_semibold_18.copy(color = PurpleMain500), // 파란색 텍스트
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

// 실행 취소 Snackbar 모서리 변경
@Composable
fun UndoSnackbar(
    message: String,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .navigationBarsPadding()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(50)) // 그림자 추가
            .clip(RoundedCornerShape(50)), // 둥근 모서리로 클리핑
        contentAlignment = Alignment.Center,// 내부 Row를 중앙에 배치

    ) {
        // 배경 이미지
        Image(
            painter = painterResource(id = R.drawable.toast_bg), // toast_bg.png 사용
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(59.dp), // 이미지의 적절한 높이 (내부 콘텐츠에 맞춰 조절)
            contentScale = ContentScale.FillBounds // 이미지 크기를 Box에 맞게 조절
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp), // 좌우 내부 여백 추가
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = b2_semibold_16,
                color = Color.Black
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onUndo) {
                Text(
                    text = "실행 취소",
                    style = b2_semibold_16,
                    color = GreyMain300
                )
            }
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

// AddVideoSheet.kt의 완성된 UI 코드로 교체
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

//----------보드 설정-----------
// 보드 설정 수정 화면 UI
@Composable
fun BoardEditSheet(
    source: String?,
    currentName: String,
    isCurrentlyShared: Boolean,
    draftIsShared: Boolean,
    onDismiss: () -> Unit,
    currentMembers: List<InviteUser>,
    onInviteClick: (String, Boolean) -> Unit,
    onMembersClick: () -> Unit,
    modifier: Modifier,
    onConfirm: (newName: String, isShared: Boolean) -> Unit
) {
    //보드 소스 확인
    val isEditable = source == "USER"

    // --- 입력값 및 유효성 검사 상태 ---
    var name by rememberSaveable { mutableStateOf(currentName) }
    var isShared by rememberSaveable { mutableStateOf(draftIsShared) }
    var isNameTouched by rememberSaveable { mutableStateOf(false) }

    // 부모 화면에서 임시 저장된 값을 다시 넣어줄 때, 상태를 동기화하기 위해 필요
    LaunchedEffect(currentName, draftIsShared) {
        name = currentName
        isShared = draftIsShared // <- 여기서 저장해둔 값(true)으로 UI를 복구합니다.
    }

    val trimmedName = name.trim()
    val isNameValid = trimmedName.length >= 2
    val showError = isNameTouched && !isNameValid
    // -----------------

    // 오직 "서버 상태"가 개인 보드(!isCurrentlyShared)일 때만 변경 가능.
    // (사용자가 잠시 공유 버튼을 눌렀다고 해서 이 값이 false가 되면 안 됨)
    val canChangeType = isEditable && !isCurrentlyShared

    Surface(
        modifier = Modifier
            .imePadding()
            .fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp, // 입체 효과
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding() // 하단 네비게이션 바 패딩
                .padding(start = 16.dp, end = 16.dp, top = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 헤더: 닫기(좌) + 타이틀(중) + 완료(우) ---
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // 왼쪽 닫기 버튼
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-18).dp)
                        .size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "닫기"
                    )
                }

                // 가운데 타이틀
                Text(text = "보드 설정", style = b1_semibold_18)

                // 오른쪽 완료 버튼
                Button(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 18.dp),
                    onClick = {
                        isNameTouched = true
                        if (isNameValid) {
                            onConfirm(trimmedName, isShared) // 최종 선택된 값 전송
                        }
                    },
                    enabled = isNameValid, // 유효성 검사
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PurpleMain500,
                        disabledContentColor = GreyMain100
                    ),
                    contentPadding = PaddingValues(0.dp) // 패딩 제거하여 텍스트 위치 조정
                ) {
                    Text(text = "완료", style = b1_semibold_18)
                }
            }
            Spacer(Modifier.height(28.dp))

            // --- 보드 이름 입력 ---
            Column(horizontalAlignment = Alignment.Start) {
                Text("보드 이름", style = b2_semibold_16)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField1(
                    value = name,
                    onValueChange = {
                        name = it
                        if (!isNameTouched) isNameTouched = true
                    },
                    enabled = isEditable,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(40.dp),
                    isError = showError,
                    // CreateBoardSheet와 동일한 스타일 적용
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleMain500,
                        unfocusedBorderColor = Grey50,
                        errorBorderColor = RedError,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Grey10,
                        disabledBorderColor = Grey50, // 비활성화 시 테두리 색
                        disabledTextColor = GreyMain300,   // 비활성화 시 텍스트 색
                        disabledContainerColor = Grey10
                    ),
                    placeholder = { Text("보드 이름", style = b3_regular_14, color = Grey200) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (isNameValid) onConfirm(trimmedName, isShared)
                    })
                )

                // --- 유효성 및 안내 메시지 ---
                Box(modifier = Modifier.padding(top = 8.dp)) {
                    if (!isEditable) { // AI 보드일 때 안내 문구
                        Text(
                            text = "AI 보드는 이름 변경이 불가합니다.",
                            style = b3_regular_14,
                            color = Grey500, // 안내 문구 색상
                        )
                    } else if (showError) { // USER 보드이고, 유효성 검사 실패 시
                        Text(
                            text = "보드 이름을 2자 이상 입력해주세요.",
                            style = b3_regular_14,
                            color = RedError,
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))

            // --- 보드 유형 선택 ---
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("보드 유형", style = b2_semibold_16)
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. [개인 보드 버튼]
                    // 표시 조건: 현재 공유된 상태가 아닐 때만 보임
                    // (이미 공유된 보드는 '개인' 칩을 아예 숨김)
                    if (!isCurrentlyShared) {
                        SegButton(
                            text = "개인",
                            // 변경 불가능한 경우(AI 보드 등)에는 강제로 'false'를 주어 회색으로 표시
                            selected = if (canChangeType) !isShared else false,
                            onClick = {
                                if (canChangeType) isShared = false
                            },
                            iconRes = R.drawable.unselected_private
                        )
                    }

                    // 2. [공유 보드 버튼]
                    // 표시 조건: AI 보드가 아니거나(USER 소스), 이미 공유된 상태일 때 보임
                    // (AI 보드는 '공유' 칩을 아예 숨김)
                    if (isEditable || isCurrentlyShared) {
                        SegButton(
                            text = "공유",
                            // 변경 불가능한 경우(이미 공유된 보드)에는 강제로 'false'를 주어 회색으로 표시
                            selected = if (canChangeType) isShared else false,
                            onClick = {
                                if (canChangeType) isShared = true
                            },
                            iconRes = R.drawable.unselected_share
                        )
                    }
                }

                // 안내 문구 처리 (Spacer 추가하여 버튼과 간격 확보)
                if (isShared && !isCurrentlyShared) {
                    // 3. 개인 보드인데 사용자가 '공유' 버튼을 선택한 경우 (버튼 활성화됨, 경고 문구)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "* 공유보드로 전환 시 다시 개인보드로 변경이 불가합니다.",
                        style = b3_regular_14,
                        color = RedError
                    )
                }
                if(!isCurrentlyShared && !isShared ){
                    if(!isEditable ){
                        Spacer(Modifier.height(8.dp))
                        Text("* 회원님만 이 보드를 볼 수 있습니다.", style = AppTextStyles.b3_regular_14, color = Grey500)
                    }
                    else{
                    Spacer(Modifier.height(8.dp))
                    Text("* 회원님만 이 보드를 볼 수 있습니다.", style = AppTextStyles.b3_regular_14, color = Purple700)}
                }
            }

            // --- 참여자 초대 및 목록 (공유 보드일 때만 애니메이션으로 등장) ---
            AnimatedVisibility(
                visible = isShared,
                enter = slideInVertically(initialOffsetY = { -it / 2 }) + androidx.compose.animation.fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it / 2 }) + androidx.compose.animation.fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // 참여자 목록 확인
                    Row(
                        modifier = Modifier
                            .clickable { onMembersClick() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("참여자 목록", style = b2_semibold_16)
                        Icon(painter = painterResource(R.drawable.arrow_right), contentDescription = "더보기")
                    }

                    // 초대하기 버튼
                    Row(
                        modifier = Modifier
                            .padding(top = 28.dp)
                            .clickable {
                                onInviteClick(name, isShared)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("참여자 초대하기", style = b2_semibold_16)
                        Icon(painter = painterResource(R.drawable.arrow_right), contentDescription = "더보기")
                    }

                    // 참여자 목록 표시
                    if (currentMembers.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentMembers, key = { it.id }) { user ->
                                InvitePreviewChip(user = user)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // 하단 여백 (내용물에 따라 높이 유동적)
            Spacer(Modifier.height(20.dp))
        }
    }
}

//보드 유형 선택 버튼 커스텀
@Composable
private fun SegButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    iconRes: Int,
) {
    val primary = PurpleMain500
    val outline = GreyMain100

    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(40.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Purple50 else Grey10,
            contentColor = if (selected) primary else MaterialTheme.colorScheme.onSurface,
            // 비활성화 상태일 때의 색상 (기존 색상에서 alpha만 적용하여 연하게)
            disabledContainerColor = (if (selected) Purple50 else Grey10),
            disabledContentColor = (if (selected) primary else Grey500)
        ),
        border = if (selected)
            ButtonDefaults.outlinedButtonBorder.copy(
                brush = SolidColor(primary),
                width = 1.5.dp
            )
        else
            ButtonDefaults.outlinedButtonBorder.copy(
                brush = SolidColor(outline),
                width = 1.dp
            ),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.height(45.dp)
    ) {
        Spacer(Modifier.width(3.dp))
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = (if (selected) PurpleMain500 else Grey500)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = AppTextStyles.b3_medium_14
        )
        Spacer(Modifier.width(3.dp))
    }
}

// 추가한 유저 확인 칩 UI
@Composable
private fun InvitePreviewChip(user: InviteUser) {
    // use first character of nickname as fallback
    val initial = user.nickname.firstOrNull()?.uppercaseChar()?.toString()
        ?: user.email.firstOrNull()?.uppercaseChar()?.toString()
        ?: ""

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Purple50),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = b3_medium_14, // AppTextStyles 대신 import된 스타일 사용
                color = com.example.nubo.ui.theme.Purple700
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = user.nickname,
            style = com.example.nubo.ui.theme.AppTextStyles.label_medium_12,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = Grey500
        )
    }
}

// 참여자 목록 확인
@Composable
fun BoardMembersSheet(
    activeMembers: List<MemberDto>,     // 참여 중인 사용자
    pendingMembers: List<InvitationDto>, // 대기 중인 사용자
    isOwner: Boolean,
    onBack: () -> Unit,
    onCancelInvite: (Long) -> Unit // 초대 취소 클릭 시 (invitationId 전달)
) {
    Surface(
        modifier = Modifier
            .imePadding()
            .fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp, // 입체 효과
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .background(color = Color.White)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, top = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 1. 헤더
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-18).dp)
                        .size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "뒤로가기",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "참여자 목록",
                    style = AppTextStyles.b1_semibold_18,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 2. 헤더로부터 32dp 아래부터 목록 시작
            Spacer(Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp) // 아이템 간 간격은 개별 처리
            ) {
                if (isOwner && pendingMembers.isNotEmpty()) { // owner 체크 추가
                    item {
                        Text(
                            text = "대기 중인 사용자",
                            style = b2_semibold_16
                        )
                        Spacer(Modifier.height(12.dp)) // 텍스트와 목록 사이 여백
                    }

                    items(pendingMembers, key = { "invitation_${it.invitationId}" }) { invitation ->
                        PendingMemberRow(
                            invitation = invitation,
                            isOwner = isOwner,
                            onCancelClick = { onCancelInvite(invitation.invitationId) }
                        )
                        Spacer(Modifier.height(8.dp)) // 아이템 간 간격
                    }

                    // 영역 사이 구분선 (대기 중인 사용자가 있을 때만 표시)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Grey50)   // 원하는 배경색
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // --- 참여 중인 사용자 영역 ---
                item {
                    Text(
                        text = "참여 중인 사용자",
                        style = b2_semibold_16
                    )
                    Spacer(Modifier.height(12.dp)) // 텍스트와 목록 사이 여백
                }

                items(activeMembers, key = { "member_${it.userId}" }) { member ->
                    ActiveMemberRow(member = member)
                    Spacer(Modifier.height(8.dp))
                }

                // 하단 여백
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

// 대기 중인 사용자 Row
@Composable
private fun PendingMemberRow(
    invitation: InvitationDto,
    isOwner: Boolean,
    onCancelClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아바타 (이니셜)
        val initial = invitation.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: ""
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8E7FF)), // 연한 보라색 배경 (임의 지정, 테마에 맞게 수정 가능)
            contentAlignment = Alignment.Center
        ) {
            Text(text = initial, fontSize = 14.sp, color = PurpleMain500, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.width(12.dp))

        // 이름 및 이메일
        Column(Modifier.weight(1f)) {
            Text(
                text = invitation.nickname,
                style = b2_semibold_16,
                color = MaterialTheme.colorScheme.onSurface
            )
            /*Text(
                text = invitation.email,
                style = AppTextStyles.b3_regular_14,
                color = Grey200 // 혹은 Grey500
            )*/
        }

        // 초대 취소 버튼 (요청하신 스타일 적용)
        // invited 상태는 '대기 중'이므로 항상 true라고 가정하고 UI 표시
        val invited = true
        // owner=true 일 때만 초대 취소 버튼 표시
        if (isOwner) {
            OutlinedButton(
                onClick = onCancelClick,
                shape = RoundedCornerShape(15.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp),
                colors = if (invited) {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = PurpleMain500,
                        contentColor = Color.White
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                },
                border = if (invited) null else ButtonDefaults.outlinedButtonBorder,
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "초대 취소",
                    style = AppTextStyles.b3_medium_14.copy(fontSize = 12.sp) // 버튼 텍스트 크기 조정
                )
            }
        }
    }
}

// 참여 중인 사용자 Row
@Composable
private fun ActiveMemberRow(
    member: MemberDto
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아바타
        val initial = member.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: ""
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8E7FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initial, fontSize = 14.sp, color = PurpleMain500, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.width(12.dp))

        // 이름 및 이메일 (이메일은 MemberDto에 없으면 닉네임만 표시하거나, API 응답에 추가 필요)
        Column(Modifier.weight(1f)) {
            Text(
                text = member.nickname,
                style = AppTextStyles.b2_semibold_16,
                color = MaterialTheme.colorScheme.onSurface
            )
            // API 응답에 이메일이 없다면 생략하거나 추가 로직 필요.
            // 현재 DTO에는 이메일이 없으므로 닉네임만 표시합니다.
        }

        // Owner 칩 (ROLE이 OWNER일 때만 표시)
        if (member.role == "OWNER") {
            OutlinedButton(
                onClick = {}, // 클릭 불가
                enabled = false, // 비활성화하여 클릭 막음 (하지만 색상은 유지해야 함)
                shape = RoundedCornerShape(15.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Purple50,
                    disabledContainerColor = Purple50, // 비활성화 시에도 배경색 유지
                    contentColor = PurpleMain500,
                    disabledContentColor = PurpleMain500 // 비활성화 시에도 텍스트색 유지
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(PurpleMain500),
                    width = 1.dp
                ),
                modifier = Modifier.height(26.dp) // 칩 높이 조금 작게
            ) {
                Text(
                    text = "OWNER",
                    style = AppTextStyles.label_medium_12,
                )
            }
        }
    }
}
