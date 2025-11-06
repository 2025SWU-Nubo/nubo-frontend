package com.example.nubo.ui.screen.interest

import com.example.nubo.R

/** Map boardId/boardName to local drawable */
object InterestAssets {

    // 1) Exact mapping by server boardId (preferred: stable & fast)
    private val idToDrawable = mapOf(
        59L to R.drawable.interest_education,             // 교육
        60L to R.drawable.interest_tech,                  // 테크 & 프로그래밍
        61L to R.drawable.interest_business,              // 비즈니스 & 생산성
        62L to R.drawable.interest_travel,                // 뷰티 & 패션
        63L to R.drawable.interest_diet,               // 요리 & 라이프스타일
        64L to R.drawable.interest_health,                // 운동 & 건강
        65L to R.drawable.interest_travel,                // 여행 & 브이로그
        66L to R.drawable.interest_game,                  // 게임
        67L to R.drawable.interest_hobby,                 // 취미 & 공예
        68L to R.drawable.interest_music,                 // 음악
        69L to R.drawable.interest_art,                   // 예술 & 디자인
        70L to R.drawable.interest_entertainment,         // 엔터테인먼트(코미디/TV/쇼)
        71L to R.drawable.basic_profile_image                   // 기타
    )

    // 2) Fallback by boardName keyword (in case IDs change)
    private fun nameToDrawable(name: String): Int {
        val n = name.lowercase()
        return when {
            "교육" in n || "educ" in n -> R.drawable.interest_education
            "테크" in n || "프로그래밍" in n || "tech" in n -> R.drawable.interest_tech
            "비즈니스" in n || "생산성" in n || "biz" in n -> R.drawable.interest_business
            "뷰티" in n || "패션" in n -> R.drawable.interest_fashion
            "요리" in n || "라이프스타일" in n -> R.drawable.interest_diet
            "운동" in n || "건강" in n -> R.drawable.interest_health
            "여행" in n || "브이로그" in n || "vlog" in n -> R.drawable.interest_travel
            "게임" in n || "game" in n -> R.drawable.interest_game
            "취미" in n || "공예" in n -> R.drawable.interest_hobby
            "음악" in n || "music" in n -> R.drawable.interest_music
            "예술" in n || "디자인" in n || "art" in n || "design" in n -> R.drawable.interest_art
            "엔터테인먼트" in n || "코미디" in n || "tv" in n || "쇼" in n -> R.drawable.interest_entertainment
            else -> R.drawable.basic_profile_image
        }
    }

    /** Get drawable for a board */
    fun of(boardId: Long, boardName: String): Int {
        // Prefer ID mapping; fallback to name mapping
        return idToDrawable[boardId] ?: nameToDrawable(boardName)
    }
}
