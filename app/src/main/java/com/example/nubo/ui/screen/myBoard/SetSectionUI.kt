package com.example.nubo.ui.screen.myBoard

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
// --- Keyboard 옵션 ---
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
// --- 프로젝트 리소스 / 테마 ---
import com.example.nubo.R
import com.example.nubo.ui.component.noRippleClickable
import com.example.nubo.ui.theme.*
import com.example.nubo.ui.theme.AppTextStyles.b1_semibold_18
import com.example.nubo.ui.theme.AppTextStyles.b2_semibold_16
import com.example.nubo.ui.theme.AppTextStyles.b3_regular_14
import androidx.compose.material3.OutlinedTextField as OutlinedTextField1

/** 섹션 관련하여 수행되는 기능들에 대한 ui 파일
* 섹션 추가, 섹션 이름 변경 */

//섹션 이름 변경
@Composable
fun SectionRename(
    modifier: Modifier,
    currentName: String,
    isCurrentlyShared: Boolean,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(currentName) }
    var isShared by rememberSaveable { mutableStateOf(isCurrentlyShared) }
    var isNameTouched by rememberSaveable { mutableStateOf(false) }

    val trimmedName = name.trim()
    val isNameValid = trimmedName.length >= 2
    val showError = isNameTouched && !isNameValid

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .background(color = Color.White)
                .height(300.dp)
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- 헤더 ---
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // 왼쪽 뒤로가기 버튼
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back), // ← 뒤로가기 아이콘
                    contentDescription = "뒤로가기",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(22.dp)
                        .noRippleClickable { onBack() }
                )
                Text(text = "섹션 이름 변경", style = b1_semibold_18)
              /*  // 오른쪽 닫기 버튼
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

            // --- 이름 입력 ---
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleMain500,
                        unfocusedBorderColor = Grey50,
                        errorBorderColor = RedError,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Grey10
                    ),
                    placeholder = {
                        Text("섹션 이름", style = b3_regular_14, color = Grey200)
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (isNameValid) onConfirm(trimmedName, isShared)
                    })
                )

                // --- 오류 메시지 ---
                Box(modifier = Modifier.height(24.dp)) {
                    if (showError) {
                        Text(
                            text = "섹션 이름을 2자 이상 입력해주세요.",
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
                        if (isNameValid) onConfirm(trimmedName, isShared)
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

//섹션 추가
@Composable
fun AddSection(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onBack: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var isNameTouched by rememberSaveable { mutableStateOf(false) }

    val trimmedName = name.trim()
    // 생성 모드 활성화 조건
    val confirmEnabled = trimmedName.length >= 2
    // 에러 표시 조건
    val showError = isNameTouched && name.isNotBlank() && trimmedName.length < 2

    Surface(
        modifier = Modifier
            .imePadding()
            .fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .background(color = Color.White)
                .height(300.dp)
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- 헤더 ---
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
                Text(text = "섹션 추가", style = b1_semibold_18)
               /* // 오른쪽 닫기 버튼
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

            // --- 이름 입력 ---
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleMain500,
                        unfocusedBorderColor = Grey50,
                        errorBorderColor = RedError,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Grey10
                    ),
                    placeholder = {
                        Text("섹션 이름", style = b3_regular_14, color = Grey200)
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (confirmEnabled) onConfirm(trimmedName)
                    })
                )

                // --- 유효성 메시지 ---
                Box(modifier = Modifier.height(24.dp)) {
                    if (showError) {
                        Text(
                            text = "섹션 이름을 2자 이상 입력해주세요.",
                            style = b3_regular_14,
                            color = RedError,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // --- 생성 버튼 ---
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    onClick = {
                        isNameTouched = true
                        if (confirmEnabled) onConfirm(trimmedName)
                    },
                    shape = RoundedCornerShape(8.dp),
                    enabled = confirmEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleMain500,
                        disabledContainerColor = Grey200,
                        contentColor = Color.White,
                        disabledContentColor = Color.White
                    )
                ) {
                    Text(text = "추가하기", style = b1_semibold_18)
                }

                Spacer(Modifier.height(25.dp))
            }
        }
    }
}
