package com.example.nubo.data.model

import com.google.gson.annotations.SerializedName

data class BoardMembersResponse(
    @SerializedName("boardId")
    val boardId: Long,
    @SerializedName("members")
    val members: List<MemberDto>,
    @SerializedName("invitations")
    val invitations: List<InvitationDto>
)

data class MemberDto(
    @SerializedName("userId")
    val userId: Long,
    @SerializedName("nickname")
    val nickname: String,
    @SerializedName("role")
    val role: String // "OWNER", "ADMIN", "MEMBER"
)

data class InvitationDto(
    @SerializedName("invitationId")
    val invitationId: Long,
    @SerializedName("email")
    val email: String,
    @SerializedName("nickname")
    val nickname: String,
    @SerializedName("status")
    val status: String // "PENDING"
)
