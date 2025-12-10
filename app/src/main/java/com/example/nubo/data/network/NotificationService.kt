package com.example.nubo.data.network

import com.example.nubo.data.dto.DeleteDeviceTokenRequest
import com.example.nubo.data.dto.NotificationDto
import com.example.nubo.data.dto.RegisterDeviceTokenRequest
import com.example.nubo.data.model.UnreadNotificationExistsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationService {

    // 최근 7일 알림 조회
    @GET("/api/notification")
    suspend fun getNotifications(): List<NotificationDto>

    // 특정 알림 읽음 처리
    @PATCH("/api/notification/{notificationId}/read")
    suspend fun markRead(
        @Path("notificationId") id: String
    )

    // 전체 읽음 처리
    @PATCH("/api/notification/read-all")
    suspend fun markAllRead()

    // FCM 디바이스 토큰 등록/갱신
    @POST("/api/push/device-token")
    suspend fun registerDeviceToken(
        @Body body: RegisterDeviceTokenRequest
    ): retrofit2.Response<Unit>

    // FCM 디바이스 토큰 삭제
    @HTTP(method = "DELETE", path = "/api/push/device-token", hasBody = true)
    suspend fun deleteDeviceToken(
        @Body body: DeleteDeviceTokenRequest
    ): retrofit2.Response<Unit>

    // 초대 수락
    @POST("/api/board/invitation/{invitationId}/accept")
    suspend fun acceptInvitation(
        @Path("invitationId") invitationId: Int
    ): retrofit2.Response<Unit>

    // 초대 거절
    @POST("/api/board/invitation/{invitationId}/reject")
    suspend fun rejectInvitation(
        @Path("invitationId") invitationId: Int
    ): retrofit2.Response<Unit>

    // 읽지 않은 알림 존재 여부 조회
    @GET("/api/notification/unread-exists")
    suspend fun unreadNotifications(
        @Header("Accept") accept: String = "application/json"
    ): Response<UnreadNotificationExistsResponse>
}
