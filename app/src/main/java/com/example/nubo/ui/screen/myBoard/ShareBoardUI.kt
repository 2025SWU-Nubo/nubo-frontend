package com.example.nubo.ui.screen.myBoard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
// --- 프로젝트 테마 / 리소스 ---
import com.example.nubo.ui.theme.*
import com.example.nubo.R
// --- 모델 ---
import com.example.nubo.data.model.InvitationDto
import com.example.nubo.data.model.MemberDto
import com.example.nubo.domain.model.InviteUser
import com.example.nubo.ui.theme.AppTextStyles.b1_semibold_18
import com.example.nubo.ui.theme.AppTextStyles.b2_semibold_16
import com.example.nubo.ui.theme.AppTextStyles.b3_medium_14
import com.example.nubo.ui.theme.AppTextStyles.b3_regular_14

/** 공유보드 관련하여 수행되는 기능들에 대한 ui 파일
* 참여자 목록, 참여자 초대 등 */

// 보드 설정 수정 화면 UI
@Composable
fun BoardEditSheet(
    source: String?,
    currentName: String,
    isCurrentlyShared: Boolean,
    draftIsShared: Boolean,
    onDismiss: () -> Unit,
    currentMembers: List<InviteUser>,
    onInviteClick: (String, Boolean) -> Unit,
    onMembersClick: () -> Unit,
    modifier: Modifier,
    onConfirm: (newName: String, isShared: Boolean) -> Unit
) {
    //보드 소스 확인
    val isEditable = source == "USER"

    // --- 입력값 및 유효성 검사 상태 ---
    var name by rememberSaveable { mutableStateOf(currentName) }
    var isShared by rememberSaveable { mutableStateOf(draftIsShared) }
    var isNameTouched by rememberSaveable { mutableStateOf(false) }

    // 부모 화면에서 임시 저장된 값을 다시 넣어줄 때, 상태를 동기화하기 위해 필요
    LaunchedEffect(currentName, draftIsShared) {
        name = currentName
        isShared = draftIsShared // <- 여기서 저장해둔 값(true)으로 UI를 복구합니다.
    }

    val trimmedName = name.trim()
    val isNameValid = trimmedName.length >= 2
    val showError = isNameTouched && !isNameValid
    // -----------------

    // 오직 "서버 상태"가 개인 보드(!isCurrentlyShared)일 때만 변경 가능.
    // (사용자가 잠시 공유 버튼을 눌렀다고 해서 이 값이 false가 되면 안 됨)
    val canChangeType = isEditable && !isCurrentlyShared

    Surface(
        modifier = Modifier
            .imePadding()
            .fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp, // 입체 효과
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding() // 하단 네비게이션 바 패딩
                .padding(start = 16.dp, end = 16.dp, top = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 헤더: 닫기(좌) + 타이틀(중) + 완료(우) ---
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // 왼쪽 닫기 버튼
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-18).dp)
                        .size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "닫기"
                    )
                }

                // 가운데 타이틀
                Text(text = "보드 설정", style = b1_semibold_18)

                // 오른쪽 완료 버튼
                Button(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 18.dp),
                    onClick = {
                        isNameTouched = true
                        if (isNameValid) {
                            onConfirm(trimmedName, isShared) // 최종 선택된 값 전송
                        }
                    },
                    enabled = isNameValid, // 유효성 검사
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        contentColor = PurpleMain500,
                        disabledContentColor = GreyMain100
                    ),
                ) {
                    Text(text = "완료", style = b1_semibold_18)
                }
            }
            Spacer(Modifier.height(28.dp))

            // --- 보드 이름 입력 ---
            Column(horizontalAlignment = Alignment.Start) {
                Text("보드 이름", style = b2_semibold_16)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (!isNameTouched) isNameTouched = true
                    },
                    enabled = isEditable,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(40.dp),
                    isError = showError,
                    // CreateBoardSheet와 동일한 스타일 적용
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleMain500,
                        unfocusedBorderColor = Grey50,
                        errorBorderColor = RedError,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Grey10,
                        disabledBorderColor = Grey50, // 비활성화 시 테두리 색
                        disabledTextColor = GreyMain300,   // 비활성화 시 텍스트 색
                        disabledContainerColor = Grey10
                    ),
                    placeholder = { Text("보드 이름", style = b3_regular_14, color = Grey200) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (isNameValid) onConfirm(trimmedName, isShared)
                    })
                )

                // --- 유효성 및 안내 메시지 ---
                Box(modifier = Modifier.padding(top = 8.dp)) {
                    if (!isEditable) { // AI 보드일 때 안내 문구
                        Text(
                            text = "AI 보드는 이름 변경이 불가합니다.",
                            style = b3_regular_14,
                            color = Grey500, // 안내 문구 색상
                        )
                    } else if (showError) { // USER 보드이고, 유효성 검사 실패 시
                        Text(
                            text = "보드 이름을 2자 이상 입력해주세요.",
                            style = b3_regular_14,
                            color = RedError,
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))

            // --- 보드 유형 선택 ---
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("보드 유형", style = b2_semibold_16)
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. [개인 보드 버튼]
                    // 표시 조건: 현재 공유된 상태가 아닐 때만 보임
                    // (이미 공유된 보드는 '개인' 칩을 아예 숨김)
                    if (!isCurrentlyShared) {
                        SegButton(
                            text = "개인",
                            // 변경 불가능한 경우(AI 보드 등)에는 강제로 'false'를 주어 회색으로 표시
                            selected = if (canChangeType) !isShared else false,
                            onClick = {
                                if (canChangeType) isShared = false
                            },
                            iconRes = R.drawable.unselected_private
                        )
                    }

                    // 2. [공유 보드 버튼]
                    // 표시 조건: AI 보드가 아니거나(USER 소스), 이미 공유된 상태일 때 보임
                    // (AI 보드는 '공유' 칩을 아예 숨김)
                    if (isEditable || isCurrentlyShared) {
                        SegButton(
                            text = "공유",
                            // 변경 불가능한 경우(이미 공유된 보드)에는 강제로 'false'를 주어 회색으로 표시
                            selected = if (canChangeType) isShared else false,
                            onClick = {
                                if (canChangeType) isShared = true
                            },
                            iconRes = R.drawable.unselected_share
                        )
                    }
                }

                // 안내 문구 처리 (Spacer 추가하여 버튼과 간격 확보)
                if (isShared && !isCurrentlyShared) {
                    // 3. 개인 보드인데 사용자가 '공유' 버튼을 선택한 경우 (버튼 활성화됨, 경고 문구)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "* 공유보드로 전환 시 다시 개인보드로 변경이 불가합니다.",
                        style = b3_regular_14,
                        color = RedError
                    )
                }
                if(!isCurrentlyShared && !isShared ){
                    if(!isEditable ){
                        Spacer(Modifier.height(8.dp))
                        Text("* 회원님만 이 보드를 볼 수 있습니다.", style = AppTextStyles.b3_regular_14, color = Grey500)
                    }
                    else{
                        Spacer(Modifier.height(8.dp))
                        Text("* 회원님만 이 보드를 볼 수 있습니다.", style = AppTextStyles.b3_regular_14, color = Purple700)}
                }
            }

            // --- 참여자 초대 및 목록 (공유 보드일 때만 애니메이션으로 등장) ---
            AnimatedVisibility(
                visible = isShared,
                enter = slideInVertically(initialOffsetY = { -it / 2 }) + androidx.compose.animation.fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it / 2 }) + androidx.compose.animation.fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // 참여자 목록 확인
                    Row(
                        modifier = Modifier
                            .clickable { onMembersClick() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("참여자 목록", style = b2_semibold_16)
                        Icon(painter = painterResource(R.drawable.arrow_right), contentDescription = "더보기")
                    }

                    // 초대하기 버튼
                    Row(
                        modifier = Modifier
                            .padding(top = 28.dp)
                            .clickable {
                                onInviteClick(name, isShared)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("참여자 초대하기", style = b2_semibold_16)
                        Icon(painter = painterResource(R.drawable.arrow_right), contentDescription = "더보기")
                    }

                    // 참여자 목록 표시
                    if (currentMembers.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentMembers, key = { it.id }) { user ->
                                InvitePreviewChip(user = user)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // 하단 여백 (내용물에 따라 높이 유동적)
            Spacer(Modifier.height(20.dp))
        }
    }
}

//보드 유형 선택 버튼 커스텀
@Composable
private fun SegButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    iconRes: Int,
) {
    val primary = PurpleMain500
    val outline = GreyMain100

    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(40.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Purple50 else Grey10,
            contentColor = if (selected) primary else MaterialTheme.colorScheme.onSurface,
            // 비활성화 상태일 때의 색상 (기존 색상에서 alpha만 적용하여 연하게)
            disabledContainerColor = (if (selected) Purple50 else Grey10),
            disabledContentColor = (if (selected) primary else Grey500)
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
        Spacer(Modifier.width(3.dp))
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = (if (selected) PurpleMain500 else Grey500)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = AppTextStyles.b3_medium_14
        )
        Spacer(Modifier.width(3.dp))
    }
}

// 추가한 유저 확인 칩 UI
@Composable
private fun InvitePreviewChip(user: InviteUser) {
    // use first character of nickname as fallback
    val initial = user.nickname.firstOrNull()?.uppercaseChar()?.toString()
        ?: user.email.firstOrNull()?.uppercaseChar()?.toString()
        ?: ""

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Purple50),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = b3_medium_14, // AppTextStyles 대신 import된 스타일 사용
                color = com.example.nubo.ui.theme.Purple700
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = user.nickname,
            style = com.example.nubo.ui.theme.AppTextStyles.label_medium_12,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = Grey500
        )
    }
}

// 참여자 목록 확인
@Composable
fun BoardMembersSheet(
    activeMembers: List<MemberDto>,     // 참여 중인 사용자
    pendingMembers: List<InvitationDto>, // 대기 중인 사용자
    isOwner: Boolean,
    onBack: () -> Unit,
    onCancelInvite: (Long) -> Unit // 초대 취소 클릭 시 (invitationId 전달)
) {
    Surface(
        modifier = Modifier
            .imePadding()
            .fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp, // 입체 효과
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .background(color = Color.White)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, top = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 1. 헤더
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-18).dp)
                        .size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "뒤로가기",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "참여자 목록",
                    style = AppTextStyles.b1_semibold_18,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 2. 헤더로부터 32dp 아래부터 목록 시작
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
                                .background(Grey50)   // 원하는 배경색
                        )
                        Spacer(Modifier.height(16.dp))
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

                // 하단 여백
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
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
            Text(text = initial, fontSize = 14.sp, color = PurpleMain500, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.width(12.dp))

        // 이름 및 이메일
        Column(Modifier.weight(1f)) {
            Text(
                text = invitation.nickname,
                style = b2_semibold_16,
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
            Text(text = initial, fontSize = 14.sp, color = PurpleMain500, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.width(12.dp))

        // 이름 및 이메일 (이메일은 MemberDto에 없으면 닉네임만 표시하거나, API 응답에 추가 필요)
        Column(Modifier.weight(1f)) {
            Text(
                text = member.nickname,
                style = AppTextStyles.b2_semibold_16,
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
                modifier = Modifier.height(26.dp) // 칩 높이 조금 작게
            ) {
                Text(
                    text = "OWNER",
                    style = AppTextStyles.label_medium_12,
                )
            }
        }
    }
}

