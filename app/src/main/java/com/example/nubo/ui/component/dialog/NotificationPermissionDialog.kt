package com.example.nubo.ui.component.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.PurpleMain500


@Composable
fun NotificationPermissionDialog(
    visible: Boolean,
    onAllow: () -> Unit,
    onLater: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 300.dp, max = 400.dp)
                    .padding(horizontal = 24.dp, vertical = 22.dp)
            ) {
                // 제목
                Text(
                    text = "알림 권한 설정",
                    style = AppTextStyles.b1_bold_18,
                    color = Color.Black
                )

                Spacer(Modifier.height(12.dp))

                // 질문 문장
                Text(
                    text = "카드 업로드 완료 알림을 켤까요?",
                    style = AppTextStyles.b2_medium_16,
                    color = Color.Black
                )

                Spacer(Modifier.height(6.dp))

                // 보조 설명
                Text(
                    text = "알림을 켜면 업로드가 끝났을 때 바로 알려드려요.",
                    style = AppTextStyles.b3_medium_14,
                    color = com.example.nubo.ui.theme.GreyMain300
                )

                Spacer(Modifier.height(18.dp))

                // 버튼들 (우측 정렬)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    // 나중에 (회색 필드형)
                    Button(
                        onClick = onLater,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.example.nubo.ui.theme.Grey10,
                            contentColor = com.example.nubo.ui.theme.GreyMain100
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("나중에", style = AppTextStyles.b3_medium_14)
                    }

                    Spacer(Modifier.width(8.dp))

                    // 알림 켜기 (보라 메인)
                    Button(
                        onClick = onAllow,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurpleMain500,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("알림 켜기", style = AppTextStyles.b3_medium_14)
                    }
                }
            }
        }
    }
}


