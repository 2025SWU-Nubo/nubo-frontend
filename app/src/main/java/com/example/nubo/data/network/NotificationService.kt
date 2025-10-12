package com.example.nubo.data.network

import com.example.nubo.data.model.DeleteDeviceTokenRequest
import com.example.nubo.data.model.NotificationDto
import com.example.nubo.data.model.NotificationListResponse
import com.example.nubo.data.model.RegisterDeviceTokenRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationService {
    // 내 알림 목록을 조회함 (서버가 최근 7일, 최신순 정렬을 기본으로 적용함)
    @GET("/api/notification")
    suspend fun getNotifications(): List<NotificationDto>

    // 특정 알림을 읽음 처리함 (단일)
    @PATCH("/api/notification/{notificationId}/read")
    suspend fun markRead(
        @Path("notificationId") id: String
    ): Unit

    // 모든 알림을 읽음 처리함 (전체)
    @PATCH("/api/notification/read-all")
    suspend fun markAllRead(): Unit

    // FCM 푸시를 받을 디바이스 토큰을 등록하거나 갱신함
    @POST("/api/push/device-token")
    suspend fun registerDeviceToken(
        @Body body: RegisterDeviceTokenRequest
    ): Unit

    // 등록된 디바이스 토큰을 삭제함 (로그아웃 또는 기기 제거)
    // 서버 명세가 DELETE 본문 허용이면 @HTTP(hasBody=true)로 본문을 보냄
    @HTTP(method = "DELETE", path = "/api/push/device-token", hasBody = true)
    suspend fun deleteDeviceToken(
        @Body body: DeleteDeviceTokenRequest
    ): Unit

    @POST("/api/board/invitation/{invitationId}/accept")
    suspend fun acceptInvitation(
        @Header("Authorization") bearer: String,
        @Path("invitationId") invitationId: Int,
        @Header("Accept") accept: String = "application/json",
    ): retrofit2.Response<Unit>

    @POST("/api/board/invitation/{invitationId}/reject")
    suspend fun rejectInvitation(
        @Header("Authorization") bearer: String,
        @Path("invitationId") invitationId: Int,
        @Header("Accept") accept: String = "application/json",
    ): retrofit2.Response<Unit>
}
