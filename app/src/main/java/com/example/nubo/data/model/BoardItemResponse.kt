package com.example.nubo.data.model

import okio.Source

data class BoardItemResponse(
    val id:Long,
    val name: String,
    val boardType:String,
    val source: String,
    val shared: Boolean,
    val favorite: Boolean
)
