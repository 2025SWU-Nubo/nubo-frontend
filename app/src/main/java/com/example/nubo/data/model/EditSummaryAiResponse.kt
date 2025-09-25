package com.example.nubo.data.model

import com.example.nubo.data.dto.HighlightDto

data class EditSummaryAiResponse(
    val cardId: Int,
    val summary: String,
    val highlights: List<HighlightDto>,
    val updatedAt: String
)
