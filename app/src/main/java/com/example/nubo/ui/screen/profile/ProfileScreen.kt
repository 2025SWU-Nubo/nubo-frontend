package com.example.nubo.ui.screen.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey0
import com.example.nubo.ui.theme.Grey5

@Composable
fun ProfileScreen(
    nickname: String = "닉네임",
    email: String = "aabcd@gmail.com",
    // Callbacks for actions
    onEditNickname: () -> Unit = {},
    onMyInfo: () -> Unit = {},
    onNotification: () -> Unit = {},
    onHelp: () -> Unit = {},
    onPrivacy: () -> Unit = {},
) {
    Scaffold(
    ) { inner ->
        // 스크롤 콘텐츠
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
        ) {
            // ===== Header: 프로필, 닉네임, 이메일 =====
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 프로필 사진
                Box(
                    modifier = Modifier
                        .size(85.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(" ", modifier = Modifier)
                }

                Spacer(Modifier.height(25.dp))

                // 닉네임 + 수정
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = nickname,
                        style = AppTextStyles.b2_semibold_16,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp)) // small horizontal gap
                    Icon(
                        painter = painterResource(id = R.drawable.ic_profile_pencil),
                        contentDescription = "닉네임 수정",
                        modifier = Modifier
                            .size(16.dp) // smaller icon size
                            .clickable(onClick = onEditNickname)
                    )
                }

                // 닉네임과 이메일 사이 Spacer
                Spacer(Modifier.height(5.dp))

                // 이메일
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 이메일 뱃지 (현재는 구글만 제공)
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_profile_google),
                            contentDescription = "구글 뱃지",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = email,
                        style = AppTextStyles.b2_semibold_16,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ===== Section: Settings =====
            Text(
                text = "설정",
                style = AppTextStyles.b2_medium_16,
                color = MaterialTheme.colorScheme.secondary,
                modifier =Modifier
                .padding(top = 45.dp, bottom = 20.dp, start = 20.dp,)
            )

            // Settings list
            SettingsItem(title = "내 정보", onClick = onMyInfo)
            SettingsItem(title = "알림 설정", onClick = onNotification)
            SettingsItem(title = "도움말", onClick = onHelp)
            SettingsItem(title = "개인정보 처리방침", onClick = onPrivacy)

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    onClick: () -> Unit
) {
    // Single row item with chevron
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // row click
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = AppTextStyles.b2_medium_16,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewProfileScreen() {
    MaterialTheme {
        ProfileScreen()
    }
}
