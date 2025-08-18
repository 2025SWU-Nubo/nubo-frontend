package com.example.nubo.ui.component.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.PurpleMain500

@Composable
fun InviteSheet (
    onClose: () -> Unit,
    onInvite: (String) -> Unit
){
    var email by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .background(color = Color.White)
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 20.dp,end=20.dp, top = 0.dp, bottom = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "참여자 초대",
                style = AppTextStyles.b2_semibold_16
            )
        }

        Spacer(Modifier.height(22.dp))

        //보드 이름
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(horizontal = 15.dp)
        ) {
            Text("이메일", style = AppTextStyles.b2_medium_16)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = {email = it},
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



    }

}
