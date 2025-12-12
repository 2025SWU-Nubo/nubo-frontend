package com.example.nubo.ui.screen.myBoard

import androidx.compose.runtime.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// --- Keyboard 옵션 ---
import androidx.compose.ui.text.font.FontWeight
// --- 프로젝트 테마 / 리소스 ---
import com.example.nubo.ui.theme.*
import com.example.nubo.R
// --- 모델 ---
import com.example.nubo.data.model.InvitationDto
import com.example.nubo.data.model.MemberDto
import com.example.nubo.ui.component.noRippleClickable
import com.example.nubo.ui.theme.AppTextStyles.b2_medium_16
import com.example.nubo.ui.theme.AppTextStyles.b2_semibold_16


/** 공유보드 관련하여 수행되는 기능들에 대한 ui 파일
 * 참여자 목록, 참여자 초대 등 */

// 참여자 목록 확인
@Composable
fun BoardMembersSheet(
    activeMembers: List<MemberDto>,     // 참여 중인 사용자
    pendingMembers: List<InvitationDto>, // 대기 중인 사용자
    isOwner: Boolean,
    onBack: () -> Unit,
    onInviteClick: () -> Unit,
    onCancelInvite: (Long) -> Unit // 초대 취소 클릭 시 (invitationId 전달)
) {
    Surface(
        modifier = Modifier
            .imePadding()
            .fillMaxWidth(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .background(color = Color.White)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 20.dp, end = 20.dp,top = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 1. 헤더
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

                Text(
                    text = "참여자 목록",
                    style = AppTextStyles.b1_semibold_18,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp) // 아이템 간 간격은 개별 처리
            ) {
                if (isOwner && pendingMembers.isNotEmpty()) { // owner 체크 추가
                    item {
                        Text(
                            text = "대기 중인 사용자",
                            style = b2_semibold_16
                        )
                        Spacer(Modifier.height(12.dp)) // 텍스트와 목록 사이 여백
                    }

                    items(pendingMembers, key = { "invitation_${it.invitationId}" }) { invitation ->
                        PendingMemberRow(
                            invitation = invitation,
                            isOwner = isOwner,
                            onCancelClick = { onCancelInvite(invitation.invitationId) }
                        )
                        Spacer(Modifier.height(8.dp)) // 아이템 간 간격
                    }

                    // 영역 사이 구분선 (대기 중인 사용자가 있을 때만 표시)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Grey10)   // 원하는 배경색
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }

                // --- 참여 중인 사용자 영역 ---
                item {
                    Text(
                        text = "참여 중인 사용자",
                        style = b2_semibold_16
                    )
                    Spacer(Modifier.height(12.dp)) // 텍스트와 목록 사이 여백
                }

                items(activeMembers, key = { "member_${it.userId}" }) { member ->
                    ActiveMemberRow(member = member)
                    Spacer(Modifier.height(8.dp))
                }

                // 오너일때만 참여자 초대 버튼 활성화
                if (isOwner) {
                    item {
                        Spacer(Modifier.height(8.dp))

                        AddMemberRow(
                            onClick = { onInviteClick() }  // ← 클릭하면 참여자 초대 시트로 이동시키는 흐름
                        )

                        Spacer(Modifier.height(20.dp))
                    }
                }

                // 하단 여백
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

// 참여자 초대 버튼
@Composable
private fun AddMemberRow(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)  // ActiveMemberRow와 동일 높이
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아이콘 동그라미 배경
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Grey30), // 요청한 색상
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_board_user_add),
                contentDescription = "참여자 추가",
                tint = Grey700,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = "참여자 초대",
            style = b2_medium_16,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.width(8.dp))

        Icon(
            painter = painterResource(R.drawable.arrow_right),
            contentDescription = null,
            tint = Grey500,
            )
    }
}

// 대기 중인 사용자 Row
@Composable
private fun PendingMemberRow(
    invitation: InvitationDto,
    isOwner: Boolean,
    onCancelClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아바타 (이니셜)
        val initial = invitation.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: ""
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8E7FF)), // 연한 보라색 배경 (임의 지정, 테마에 맞게 수정 가능)
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                fontSize = 14.sp,
                color = PurpleMain500,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(12.dp))

        // 이름 및 이메일
        Column(Modifier.weight(1f)) {
            Text(
                text = invitation.nickname,
                style = b2_medium_16,
                color = MaterialTheme.colorScheme.onSurface
            )
            /*Text(
                text = invitation.email,
                style = AppTextStyles.b3_regular_14,
                color = Grey200 // 혹은 Grey500
            )*/
        }

        // 초대 취소 버튼 (요청하신 스타일 적용)
        // invited 상태는 '대기 중'이므로 항상 true라고 가정하고 UI 표시
        val invited = true
        // owner=true 일 때만 초대 취소 버튼 표시
        if (isOwner) {
            OutlinedButton(
                onClick = onCancelClick,
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
                Text(
                    text = "초대 취소",
                    style = AppTextStyles.b3_medium_14.copy(fontSize = 12.sp) // 버튼 텍스트 크기 조정
                )
            }
        }
    }
}

// 참여 중인 사용자 Row
@Composable
private fun ActiveMemberRow(
    member: MemberDto
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아바타
        val initial = member.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: ""
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8E7FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                fontSize = 14.sp,
                color = PurpleMain500,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(12.dp))

        // 이름
        Column(Modifier.weight(1f)) {
            Text(
                text = member.nickname,
                style = b2_medium_16,
                color = MaterialTheme.colorScheme.onSurface
            )
            // API 응답에 이메일이 없다면 생략하거나 추가 로직 필요.
            // 현재 DTO에는 이메일이 없으므로 닉네임만 표시합니다.
        }

        // Owner 칩 (ROLE이 OWNER일 때만 표시)
        if (member.role == "OWNER") {
            OutlinedButton(
                onClick = {}, // 클릭 불가
                enabled = false, // 비활성화하여 클릭 막음 (하지만 색상은 유지해야 함)
                shape = RoundedCornerShape(15.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Purple50,
                    disabledContainerColor = Purple50, // 비활성화 시에도 배경색 유지
                    contentColor = PurpleMain500,
                    disabledContentColor = PurpleMain500 // 비활성화 시에도 텍스트색 유지
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(PurpleMain500),
                    width = 1.dp
                ),
                modifier = Modifier.height(32.dp) // 칩 높이 조금 작게
            ) {
                Text(
                    text = "OWNER",
                    style = AppTextStyles.label_medium_12,
                )
            }
        }
    }
}

