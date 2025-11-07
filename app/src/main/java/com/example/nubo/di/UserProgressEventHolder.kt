package com.example.nubo.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 전역에서 발생하는 사용자 주요 이벤트를 보관하는 홀더.
 * (예: 레벨업, 누베리 획득 등)
 *
 * CardDetailViewModel에서 이벤트를 post하고
 * LearnViewModel에서 이벤트를 observe(collect)
 */
@Singleton
class UserProgressEventHolder @Inject constructor() {

    // --- 레벨업 이벤트 (가장 마지막 스테이지 저장) ---
    private val _pendingLevelUp = MutableStateFlow<Int?>(null)
    val pendingLevelUp = _pendingLevelUp.asStateFlow()

    fun postLevelUpEvent(stage: Int) {
        _pendingLevelUp.value = stage
    }

    fun consumeLevelUpEvent() {
        _pendingLevelUp.value = null
    }

    // --- 누베리 획득 이벤트 (보여줄 게 있는지 여부만 저장) ---
    private val _pendingBerryGained = MutableStateFlow<Boolean>(false)
    val pendingBerryGained = _pendingBerryGained.asStateFlow()

    fun postBerryGainedEvent() {
        // 여러 번 획득해도 한 번만 true로 설정됨
        _pendingBerryGained.value = true
    }

    fun consumeBerryGainedEvent() {
        _pendingBerryGained.value = false
    }
}
