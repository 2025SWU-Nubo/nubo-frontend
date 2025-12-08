package com.example.nubo.ui.screen.myBoard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField as OutlinedTextField1
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.nubo.R
import com.example.nubo.ui.component.noRippleClickable
import com.example.nubo.ui.theme.AppTextStyles.b1_semibold_18
import com.example.nubo.ui.theme.AppTextStyles.b2_semibold_16
import com.example.nubo.ui.theme.AppTextStyles.b3_regular_14
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Grey20
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.ui.theme.RedError


/** 보드 선택 시 수행되는 기능들에 대한 파일
* 보드 삭제, 보드 이름 변경 등*/

// 보드 선택 바텀바 - 삭제 기능
// 나의 카드 전체 탭에서 함께 사용
@Composable
fun BoardSelectionContent(
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit,
    selectedCardCount: Int,
    selectedBoardCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
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
                .padding(top=24.dp)
        ) {
          /*  IconButton(
                onClick = onDismiss,
                modifier = Modifier.padding(start = 4.dp, top = 10.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "닫기"
                )
            }*/
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

// 보드 바텀바 버튼
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

// 보드 전체 삭제 확인 다이얼로그
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

// --- 새로 만들 보드 이름 변경 UI ---
@Composable
fun BoardRename(
    currentName: String,
    isCurrentlyShared: Boolean,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    // --- 입력값 및 유효성 검사 상태 ---
    var name by rememberSaveable { mutableStateOf(currentName) }
    var isShared by rememberSaveable { mutableStateOf(isCurrentlyShared) }
    var isNameTouched by rememberSaveable { mutableStateOf(false) }

    val trimmedName = name.trim()
    val isNameValid = trimmedName.length >= 2
    val showError = isNameTouched && !isNameValid

    Surface(
        modifier = Modifier
            .imePadding()
            .fillMaxWidth(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .height(300.dp)
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 헤더: 닫기 + 타이틀 ---
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // 왼쪽 뒤로가기 버튼
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "뒤로가기",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(22.dp)
                        .noRippleClickable { onBack() }
                )
                Text(text = "보드 이름 변경", style = b1_semibold_18)
                /*// 오른쪽 닫기 버튼
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "닫기",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(24.dp)
                        .noRippleClickable { onDismiss() }
                )*/
            }

            Spacer(Modifier.height(28.dp))

            // --- 보드 이름 입력 영역 ---
            Column(horizontalAlignment = Alignment.Start) {
                Text("보드 이름", style = b2_semibold_16)
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleMain500,
                        unfocusedBorderColor = Grey50,
                        errorBorderColor = RedError,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Grey10,
                        disabledBorderColor = Grey50,
                        disabledTextColor = GreyMain300,
                        disabledContainerColor = Grey10
                    ),
                    placeholder = {
                        Text("보드 이름", style = b3_regular_14, color = Grey200)
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (isNameValid) onConfirm(trimmedName, isShared)
                    })
                )

                // --- 유효성 & 안내 메시지 ---
                Box(modifier = Modifier.height(24.dp)) {
                    if (showError) {
                        Text(
                            text = "보드 이름을 2자 이상 입력해주세요.",
                            style = b3_regular_14,
                            color = RedError,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))

                // --- 변경하기 버튼 ---
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    onClick = {
                        isNameTouched = true
                        if (isNameValid) {
                            onConfirm(trimmedName, isShared)
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    enabled = isNameValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleMain500,
                        disabledContainerColor = Grey200,
                        contentColor = Color.White,
                        disabledContentColor = Color.White
                    )
                ) {
                    Text(text = "변경하기", style = b1_semibold_18)
                }

                Spacer(Modifier.height(25.dp))
            }
        }
    }
}

