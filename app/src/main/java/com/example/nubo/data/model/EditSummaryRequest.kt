package com.example.nubo.data.model

import com.example.nubo.data.dto.HighlightDto

data class EditSummaryRequest(
    val summary: String,
    val highlights: List<HighlightDto>? = null
)
