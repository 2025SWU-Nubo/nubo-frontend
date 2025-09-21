package com.example.nubo.data.model

data class RecentBoardResponse(
    val boardId: Int,                // 서버 Board ID
    val boardName: String,
    val videoThumbnailUrl : String?,
){
    val hasThumbnail: Boolean get() = !videoThumbnailUrl.isNullOrBlank()
}
