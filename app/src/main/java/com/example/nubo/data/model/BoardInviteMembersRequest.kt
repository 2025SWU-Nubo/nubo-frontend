package com.example.nubo.data.model

// 보드 설정에서 공유 보드 참여자 초대 요청
data class InviteMembersRequest(
    val emails: List<String>
)

// 참여자 초대 모델
data class InvitationMemberDto(
    val invitationId: Long,
    val email: String,
    val nickname: String?,
    val status: String
)
