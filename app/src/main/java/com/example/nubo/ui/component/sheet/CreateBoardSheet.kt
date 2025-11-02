package com.example.nubo.ui.component.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.GreyMain100
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.R
import com.example.nubo.ui.theme.Purple50
import com.example.nubo.ui.theme.Purple700
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.PinkError
import com.example.nubo.ui.theme.RedError


@Composable
fun CreateBoardSheet(
    onClose: () -> Unit,
    onBack: () -> Unit,
    onInviteClick: () -> Unit,
    onCreate: (name : String, inShared: Boolean) -> Unit,
    name: String,
    isShared: Boolean,
    onNameChange: (String) -> Unit,
    onSharedChange: (Boolean) -> Unit,
    isLoading: Boolean,
    nameError: String?,
    onSubmit: (String) -> Unit
){

    // 사용자가 타이핑 중일 때 커서/선택 상태를 보존하기 위한 로컬 상태
    // 단, "텍스트 값" 자체는 반드시 VM과 동기화한다
    var touched by rememberSaveable { mutableStateOf(false) }
    var nameValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = name)) // VM 값으로 초기화
    }

    LaunchedEffect(name) {
        if (nameValue.text != name && nameValue.composition == null) {
            nameValue = nameValue.copy(text = name, selection = nameValue.selection)
        }
    }

    val nameTrim = nameValue.text.trim()
    val localValid = nameTrim.isNotEmpty()
    val showLocalError = touched && !localValid
    val hasRemoteError = nameError != null
    val isError = showLocalError || hasRemoteError

    val errorMessage = when {
        hasRemoteError -> nameError                         // 서버/VM에서 내려준 에러(예: 중복)
        showLocalError -> "보드 이름을 입력해주세요."           // 로컬 공란/길이 등
        else -> null
    }

    Column(
        modifier = Modifier
            .background(color = Color.White)
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        //헤더
        Box(
            modifier = Modifier
                .fillMaxWidth(),            // Material 권장 최소 터치 영역
            contentAlignment = Alignment.Center
        ) {
            // 왼쪽 뒤로가기
            IconButton(
                onClick = {
                    onNameChange(nameValue.text)
                    onBack },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-18).dp)
                    .size(48.dp)           // touch target 확보
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "뒤로가기",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // 가운데 타이틀
            Text(
                text = "보드 만들기",
                style = AppTextStyles.b1_semibold_18,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(28.dp))

        //보드 이름
        Column(
            horizontalAlignment = Alignment.Start,
        ) {
            Text("보드 이름", style = AppTextStyles.b2_semibold_16)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = nameValue,
                onValueChange = {v ->
                    nameValue = v
                    if(!touched) touched = true
                    onNameChange(v.text) // 입력할 때마다 VM으로 즉시 반영
                                },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),                     // 높이 고정
                shape = RoundedCornerShape(40.dp),      // 보더 고정
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 16.sp
                ),
                isError = isError,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleMain500,
                    unfocusedBorderColor = Grey50,
                    errorBorderColor = RedError,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Grey10,
                ),
                placeholder = { Text("보드 이름", style = AppTextStyles.b3_regular_14, color = Grey200) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        touched = true
                        if (nameTrim.isNotEmpty() && !isLoading) onSubmit(nameTrim)
                    }
                )
            )

            Spacer(Modifier.height(8.dp))

            // 에러 메시지 (필드 하단)
            errorMessage?.let{msg ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(150)) + expandVertically(tween(150)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                ) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = msg,          // !! 제거
                        style = AppTextStyles.b3_regular_14,
                        color = RedError
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        //보드 유형
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()                 // 가로 꽉 채우기
        ) {
            Text("보드 유형", style = AppTextStyles.b2_semibold_16)
            Spacer(Modifier.height(8.dp))
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ){
                SegButton(
                    text ="개인 보드",
                    selected = !isShared,
                    onClick = {
                        if (isShared) {
                            onSharedChange(false)
                        }
                    },
                    iconRes = R.drawable.unselected_private
                )

                SegButton(
                    text ="공유 보드",
                    selected = isShared,
                    onClick = { if (!isShared) {
                        onSharedChange(true)
                    } },
                    iconRes = R.drawable.unselected_share
                )
            }

            Spacer(Modifier.height(8.dp))
            if(!isShared){Text("* 회원님만 이 보드를 볼 수 있습니다.", style = AppTextStyles.b3_regular_14, color = Purple700)}
        }

        Spacer(Modifier.height(24.dp))

        AnimatedVisibility(
            visible = isShared,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 300) // fadeIn 속도 0.5초
            ) + expandVertically(
                animationSpec = tween(durationMillis = 300) // expand 속도 0.5초
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 300) // fadeOut 속도 0.3초
            ) + shrinkVertically(
                animationSpec = tween(durationMillis = 300) // shrink 속도 0.3초
            )
        ) {
            //참여자 초대(공유 보드 클릭 시,)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row( // 텍스트, 아이콘 묶기
                    modifier = Modifier.clickable {
                        onNameChange(nameValue.text)
                        onInviteClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "참여자 초대하기",
                        style = AppTextStyles.b2_semibold_16,
                    )
                    Icon(
                        painter = painterResource(R.drawable.arrow_right),
                        contentDescription = "더보기",
                    )
                }

                Spacer(Modifier.height(5.dp))
                Text("이후 생성한 보드에서 참여자를 추가할 수 있습니다.",  style = AppTextStyles.b3_regular_14, color =Grey500)
                Spacer(Modifier.height(16.dp))

            }
        }

        //추가하기 버튼
        Button(modifier = Modifier.fillMaxWidth().height(50.dp),
            onClick = {
            touched = true
            if (localValid && !isLoading) {
                onSubmit(nameTrim)
            }},
            shape = RoundedCornerShape(8.dp),
            enabled = localValid && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (localValid && !isLoading ) PurpleMain500 else Grey50
            )) {
            Text(text = "추가하기", style = AppTextStyles.b1_bold_18)
        }

        Spacer(Modifier.height(25.dp))
    }
}

//보드 유형 선택 버튼 커스텀
@Composable
private fun SegButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    iconRes:Int,
) {
    val primary = PurpleMain500
    val outline = GreyMain100
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(40.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected)
                Purple50
            else
                Grey10,
            contentColor = if (selected) primary else MaterialTheme.colorScheme.onSurface
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
        Icon(
            painter = painterResource(id=iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if(selected) PurpleMain500 else Grey500
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = AppTextStyles.b3_medium_14
        )
    }
}
