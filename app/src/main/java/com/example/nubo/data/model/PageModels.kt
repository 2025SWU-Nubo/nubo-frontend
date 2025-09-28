package com.example.nubo.data.model


// 페이지 상태
data class PageState(
    val page: Int,
    val size: Int,
    val totalPages: Int,
    val totalElements: Int,
    val isFirst: Boolean,
    val isLast: Boolean,
    val numberOfElements: Int,
    val sorted: Boolean
)

// 도메인 리스트 + 페이지 상태
data class PagedResult<T>(
    val items: List<T>,
    val pageState: PageState
)
