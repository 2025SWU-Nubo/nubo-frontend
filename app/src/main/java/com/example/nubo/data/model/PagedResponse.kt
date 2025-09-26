// PagedResponse.kt
package com.example.nubo.data.model

// Generic page wrapper
data class PagedResponse<T>(
    val content: List<T>,
    val pageable: Pageable?,
    val last: Boolean,
    val totalElements: Int,
    val totalPages: Int,
    val size: Int,
    val number: Int,
    val sort: Sort?,
    val first: Boolean,
    val numberOfElements: Int,
    val empty: Boolean
) {
    data class Pageable(
        val pageNumber: Int,
        val pageSize: Int,
        val sort: Sort?,
        val offset: Long,
        val paged: Boolean,
        val unpaged: Boolean
    )
    data class Sort(
        val empty: Boolean,
        val sorted: Boolean,
        val unsorted: Boolean
    )
}

