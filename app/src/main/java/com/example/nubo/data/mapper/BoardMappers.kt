package com.example.nubo.data.mapper

import com.example.nubo.data.model.BoardListItemResponse
import com.example.nubo.domain.model.BoardSummary

// 목록 DTO → 도메인 변환
fun BoardListItemResponse.toDomain() = BoardSummary(
    id = id,
    name = name,
    source = source,
    sectionCount = sectionCount,
    cardCount = cardCount,
    updatedAt = updatedAt,
    thumbnail = videoThumbnailUrl,
    shared = shared,
    favorite = favorite
)
