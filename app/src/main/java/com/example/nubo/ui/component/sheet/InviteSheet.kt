package com.example.nubo.ui.component.sheet

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.PurpleMain500
import com.example.nubo.ui.component.sheet.InviteUiState
import androidx.compose.foundation.lazy.items
import com.example.nubo.ui.theme.Grey200
import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.example.nubo.domain.model.InviteUser
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.Purple700

@Composable
fun InviteSheet (
    onClose: () -> Unit,
    onBack:()-> Unit,
    onInvite: (String) -> Unit,
    onComplete: (List<String>, List<InviteUser>) -> Unit,
    resetSignal: Int = 0,
    initialSelected: List<String> = emptyList(),
    useTopPadding: Boolean = false
){
    val viewModel: InviteViewModel = hiltViewModel()

    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val selectedUsers by viewModel.selectedUsers.collectAsState()

    val hasSelection = selected.isNotEmpty()

    // 최대 시트 높이를 화면 높이의 75퍼로 제한
    val configuration = LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp * 0.8f).dp   // 0.7f ~ 0.8f 사이에서 취향대로 조절


    Column(
        modifier = Modifier
            .background(color = Color.White)
            .fillMaxWidth()
            .heightIn(max = maxSheetHeight)
            // 조건부 상단 패딩 적용
            .then(if (useTopPadding) Modifier.statusBarsPadding() else Modifier)
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
                onClick = onBack,
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
                text = "참여자 초대",
                style = AppTextStyles.b1_semibold_18,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(20.dp))

        //참여자 검색
        Column(
            horizontalAlignment = Alignment.Start,
        ) {
            Text("이메일", style = AppTextStyles.b2_medium_16, modifier = Modifier.padding(start=3.dp))
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                singleLine = true,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(40.dp),      // pill
                textStyle = TextStyle(fontSize = 14.sp, lineHeight = 18.sp),
                leadingIcon = { Icon(painter = painterResource(R.drawable.ic_search), contentDescription = "검색") },
                placeholder = { Text("이메일로 검색",style = AppTextStyles.b3_regular_14 ,color = Grey200) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleMain500,
                    unfocusedBorderColor = Grey50,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Grey10,
                    cursorColor = Purple700
                )
            )
        }

        Spacer(Modifier.height(22.dp))

        //결과 스크롤 영역
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 360.dp)    // 원하는 고정 높이
        ) {
            when(val s = uiState){
                InviteUiState.Idle -> {
                    if (selectedUsers.isNotEmpty()) {
                        Column {
                            Text(text = "현재까지 초대 요청한 사용자", style = AppTextStyles.label_medium_12, color = Grey200)
                            Spacer(Modifier.height(6.dp))
                            InvitedList(
                                users = selectedUsers,
                                selectedEmails = selected,
                                onToggle = { user -> viewModel.toggleSelect(user) }
                            )
                        }
                    } else {
                        EmptyHint("검색된 사용자는 이곳에 표시됩니다.")
                    }
                }
                InviteUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ){
                        CircularProgressIndicator(strokeWidth = 3.dp)
                    }
                }
                is InviteUiState.Error -> {
                    EmptyHint("검색 중 오류가 발생했어요.\n잠시 뒤 다시 시도해주세요.")
                }
                is InviteUiState.Success -> {
                    if (s.users.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ){
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.error_face),
                                    contentDescription = "사용자 조회 실패",
                                    )
                                Text(text = "일치하는 사용자가 없습니다.", style = AppTextStyles.b2_bold_16)
                                Text(text = "이메일 정보가 정확한지 확인해주세요.", style = AppTextStyles.b3_regular_14)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(s.users, key = { it.id }) { user ->
                                UserRow(
                                    user = user,
                                    invited = user.email in selected,
                                    onInviteToggle = { viewModel.toggleSelect(user) }
                                )
                            }
                            item { Spacer(Modifier.height(8.dp)) }
                        }
                    }
                }
            }
        }

        //완료하기 버튼
        Button(modifier = Modifier.fillMaxWidth().height(50.dp),
            onClick ={
                val count = selected.size
                if (count > 0) {
                    // 이메일 리스트
                    val emails = selected.toList()
                    // 전체 유저 정보 (프리뷰용)
                    val users = selectedUsers // viewModel.selectedUsers.collectAsState() 로 받아온 값

                    onComplete(emails, users)
                }
        } ,
            shape = RoundedCornerShape(8.dp),
            enabled = hasSelection,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hasSelection) PurpleMain500 else Grey50,
                contentColor = if (hasSelection) Color.White else Grey500,
                disabledContainerColor = Grey50,
                disabledContentColor = Grey500
            )) {
            val countText = if (hasSelection) "(${selected.size})" else ""

            Text(text = "완료하기", style = AppTextStyles.b1_bold_18)
        }
        Spacer(Modifier.height(25.dp))
    }

}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AppTextStyles.b3_regular_14,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun UserRow(
    user: InviteUser,
    invited: Boolean,
    onInviteToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아바타(플레이스홀더)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8E7FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(user.nickname.take(1), fontSize = 12.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(user.nickname, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                user.email,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        OutlinedButton(
            onClick = onInviteToggle,
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
            Text(if (invited) "초대됨" else "초대")
        }
    }
}

@Composable
private fun InvitedList(
    users: List<InviteUser>,
    selectedEmails: Set<String>,
    onToggle: (InviteUser) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(users, key = { it.id }) { user ->
            UserRow(
                user = user,
                invited = user.email in selectedEmails,
                onInviteToggle = { onToggle(user) }
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun InvitedEmailRow(
    email: String,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle with first letter
        val initial = email.firstOrNull()?.uppercaseChar()?.toString() ?: ""

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8E7FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = email,
                style = AppTextStyles.b3_medium_14,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        OutlinedButton(
            onClick = onToggle,
            shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = PurpleMain500,
                contentColor = Color.White
            ),
            border = null,
            modifier = Modifier.height(32.dp)
        ) {
            Text("초대됨")
        }
    }
}
