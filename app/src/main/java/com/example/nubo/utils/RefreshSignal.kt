package com.example.nubo.utils

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import kotlinx.coroutines.flow.StateFlow

const val REFRESH_TICK_KEY = "refresh_tick" // 공통 키(라우트별 SavedStateHandle 공간에 저장됨)

/**
 * 대상 라우트의 SavedStateHandle에 항상 "새로운 Long 값"을 넣어
 * 이벤트 병합(같은 값 무시) 문제를 방지
 */
fun NavController.postRefreshTick(targetRoute: String) {
    runCatching {
        val tick = System.currentTimeMillis()
        getBackStackEntry(targetRoute).savedStateHandle[REFRESH_TICK_KEY] = tick
    }.onFailure {
        // 대상 라우트가 백스택에 없을 수 있으므로 안전 처리(경고 로그만)
        android.util.Log.w("RefreshSignal", "BackStackEntry not found: $targetRoute", it)
    }
}

/**
 * SavedStateHandle에서 tick(StateFlow<Long>)을 꺼내는 확장 함수
 * 기본값 0L로 시작하며, 0이 아닌 값이 들어오면 새 이벤트로 취급
 */
fun SavedStateHandle.refreshTicks(): StateFlow<Long> =
    getStateFlow(REFRESH_TICK_KEY, 0L)
