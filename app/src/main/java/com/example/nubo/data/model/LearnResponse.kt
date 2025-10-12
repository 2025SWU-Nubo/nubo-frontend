package com.example.nubo.data.model

import com.google.gson.annotations.SerializedName

// /api/stat/dashboard 응답 전체를 담는 데이터 클래스
data class DashboardResponse(
    @SerializedName("weeklyVideoCounts")
    val weeklyVideoCounts: List<WeeklyVideoCount>,

    @SerializedName("todayVideoCount")
    val todayVideoCount: Int,

    @SerializedName("todayWaterDrops")
    val todayWaterDrops: Int,

    @SerializedName("stage")
    val stage: Int,

    @SerializedName("growthRate")
    val growthRate: Int,

    @SerializedName("berryCount")
    val berryCount: Int
)

// 주간 비디오 카운트
data class WeeklyVideoCount(
    @SerializedName("date")
    val date: String, // "2025-09-21" 형식

    @SerializedName("count")
    val count: Int
)
