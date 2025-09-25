package com.example.nubo.ui.screen.profile

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.GreyMain100

@Composable
fun EditNameScreen(
    initial: String,                 // 초기 이름
    onBack: () -> Unit,              // 뒤로가기
    onDone: (String) -> Unit,         // 완료(서버 전송은 추후 onDone 내부에서 처리)
    viewModel: ProfileViewModel = hiltViewModel()
) {
    // ----- 간격 규격 -----
    val H_PADDING = 42.dp            // 좌/우 42
    val LABEL_TOP = 16.dp            // 라벨과 입력 사이 16
    val DIV_GAP = 8.dp               // 인디케이터 주변 8

    // ----- 상태 -----
    var text by rememberSaveable { mutableStateOf(initial) }
    var triedSubmit by rememberSaveable { mutableStateOf(false) }

    val trimmed = text.trim()
    val changed = trimmed != initial
    val hasAnyInput = trimmed.isNotEmpty()
    val tooShort = trimmed.length < 2
    // 규칙: 입력/삭제 등 변화가 있으면 보라(활성) → 단, 완료를 눌렀는데 2자 미만이면 즉시 비활성+문구 노출
    val enabled = changed && hasAnyInput && !(triedSubmit && tooShort)

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    //텍스트 필드 부분
    var isFocused by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val indicatorFocused = GreyMain100
    val indicatorUnfocused = MaterialTheme.colorScheme.outline

    val context = LocalContext.current

    // 진입 시 자동 포커스
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            // InformationScreen 헤더와 동일한 레이아웃/패딩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 15.dp)
            ) {
                // 뒤로가기 버튼
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .clickable(onClick = onBack)
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .clickable(onClick = onBack)
                            .align(Alignment.CenterStart),
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "뒤로",
                        tint = Grey1000
                    )
                }

                Text(
                    text = "내 정보",
                    style = AppTextStyles.subtitle_semibold_20,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Center)
                )

                // 오른쪽 "완료"
                Text(
                    text = "완료",
                    style = AppTextStyles.b2_semibold_16,
                    color = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable(enabled = enabled) {
                            if (tooShort) {
                                triedSubmit = true
                            } else {
                                keyboard?.hide()
                                // --- 서버 호출 ---
                                viewModel.updateName(trimmed) { ok, msg ->
                                    if (ok) {
                                        // 상위에 알려주고, 이전 화면으로
                                        onDone(trimmed)
                                    } else {
                                        // 실패: 토스트로 사용자에게 안내
                                        Toast.makeText(
                                            context,
                                            msg ?: "닉네임 변경에 실패했습니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                )
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = H_PADDING)     // 좌우 42dp
        ) {
            // 타이틀과 내용 사이 세로 42
            Spacer(Modifier.height(42.dp))

            // ----- 라벨 -----
            Text(
                text = "이름",
                style = AppTextStyles.b2_regular_16,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(Modifier.height(LABEL_TOP))

            // ----- 입력 필드 -----
            BasicTextField(
                value = text,
                onValueChange = { it ->
                    if (it.length <= 10) {   // 최소 2자 이상 최대 21자 이하
                        text = it
                        if (triedSubmit && it.trim().length >= 2) triedSubmit = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    // Draw underline indicator manually (same spacing/width as TextField)
                    .drawBehind {
                        val stroke = 1.dp.toPx()
                        drawLine(
                            color = if (isFocused) indicatorFocused else indicatorUnfocused,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = stroke
                        )
                    },
                singleLine = true,
                // Font for the input text
                textStyle = AppTextStyles.subtitle_semibold_20.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (trimmed.length < 2) {
                            triedSubmit = true
                        } else if (enabled) {
                            keyboard?.hide()
                            onDone(trimmed)
                        }
                    }
                ),
                // Remove inner padding completely & place trailing icon manually
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingValues(horizontal = 0.dp, vertical = 8.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Text area (no horizontal padding)
                        Box(Modifier.weight(1f)) {
                            innerTextField()
                        }
                        // Trailing clear icon (same as before)
                        if (text.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { text = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_profile_delete),
                                    contentDescription = "지우기",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            )

            // 인디케이터 와 문구 여백
            Spacer(Modifier.height(DIV_GAP))

            //  문구 노출 조건: (완료 시도 후 2자 미만) 또는 (변경 중이고 2자 미만)
            val showError = (triedSubmit && tooShort) || (changed && tooShort)

            if (showError) {
                Text(
                    text = "이름을 2자 이상 10자 이하로 입력해주세요.",
                    style = AppTextStyles.b3_regular_14,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)   // 인디케이터 아래 8dp
                )
            }
        }
    }
}
