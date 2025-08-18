package com.example.nubo.ui.component.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey20
import com.example.nubo.ui.theme.Grey500

@Composable
fun AddMenuSheet(
    onClose: () -> Unit,
    onVideoClick: () -> Unit,
    onBoardClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(color = Color.White)
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 10.dp,end=10.dp, top = 0.dp, bottom = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: 닫기 버튼 + 타이틀
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "추가 생성하기",
                style = AppTextStyles.b2_semibold_16
            )
        }

        Spacer(Modifier.height(12.dp))

        // Content: 작은 버튼 2개
        Row(
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SheetOption(
                icon = Icons.Outlined.Download,
                text = "영상",
                onClick = onVideoClick
            )
            SheetOption(
                icon = Icons.Outlined.Folder,
                text = "보드",
                onClick = onBoardClick
            )
        }
    }
}

@Composable
private fun SheetOption(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            color = Grey20,
            tonalElevation = 1.dp,
            modifier = Modifier.size(72.dp) // 버튼 크기 고정 (작게)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Grey500
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text,
            style = AppTextStyles.b3_medium_14
        )
    }
}
