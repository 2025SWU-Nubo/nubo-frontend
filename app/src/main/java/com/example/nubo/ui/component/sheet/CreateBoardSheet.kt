package com.example.nubo.ui.component.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.GreyMain100
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.PurpleMain500

@Composable
fun CreateBoardSheet(
    onClose: () -> Unit,
    onInviteClick: () -> Unit,
    onCreate: (name : String, inShared: Boolean) -> Unit
){
    var name by rememberSaveable { mutableStateOf("") }
    var isShared by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .background(color = Color.White)
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 20.dp,end=20.dp, top = 0.dp, bottom = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //헤더
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically)
        {
            Spacer(Modifier.width(70.dp))
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "보드 만들기",
                style = AppTextStyles.b2_semibold_16
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                shape = RoundedCornerShape(10.dp),
                onClick = onInviteClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurpleMain500,
                    contentColor = Color.White
                )) {
                Text(text = "생성", style = AppTextStyles.label_semibold_14)
            }
        }
        Spacer(Modifier.height(22.dp))

        //보드 이름
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(horizontal = 15.dp)
        ) {
            Text("보드 이름", style = AppTextStyles.b2_medium_16)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = {name = it},
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleMain500,
                    unfocusedBorderColor = Grey50,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Grey10,
                )
            )
        }

        Spacer(Modifier.height(22.dp))

        //보드 유형
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()                 // ✅ 가로 꽉 채우기
                .padding(horizontal = 15.dp),
        ) {
            Text("보드 유형", style = AppTextStyles.b2_medium_16)
            Spacer(Modifier.height(8.dp))
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ){
                SegButton(
                    text ="개인 보드",
                    selected = !isShared,
                    onClick = {isShared = false}
                )

                SegButton(
                    text ="공유 보드",
                    selected = isShared,
                    onClick = {isShared = true}
                )
            }
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
                    .padding(horizontal = 15.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text("참여자 초대(선택)", style = AppTextStyles.b2_medium_16)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("사용자", style = AppTextStyles.b2_medium_16)
                    }

                    OutlinedButton(
                        onClick = onInviteClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(MaterialTheme.colorScheme.outline)
                        )
                    ) {
                        Text("추가")
                    }
                }
            }

        }



    }
}

//보드 유형 선택 버튼 커스텀
@Composable
private fun SegButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val primary = PurpleMain500
    val outline = GreyMain100
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.surface // white-like
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
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = text,
            style = AppTextStyles.b3_medium_14
        )
    }
}
