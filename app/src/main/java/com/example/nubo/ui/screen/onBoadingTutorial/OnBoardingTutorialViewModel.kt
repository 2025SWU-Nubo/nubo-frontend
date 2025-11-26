package com.example.nubo.ui.screen.onBoadingTutorial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnBoardingTutorialViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // Called when user finishes or skips tutorial
    fun markTutorialCompletedRemote() {
        viewModelScope.launch {
            runCatching { userRepository.markTutorialCompleted() }
                .onSuccess {
                    //  서버 업데이트 성공 시 로컬 플래그도 true 로 반영
                    authRepository.setTutorialCompleted(true)
                }
                .onFailure { e ->
                    android.util.Log.e("Tutorial", "failed to mark complete", e)
                }
        }
    }

    // 튜토리얼 마지막 "시작하기" 버튼에서 호출
    fun onTutorialFinished() {
        // 1  keep existing interest flag
        val interest = authRepository.getInterestCompleted() ?: false

        // 2  update local onboarding flags
        authRepository.saveOnboardingFlags(
            interestCompleted = interest,
            tutorialCompleted = true
        )
        // or you could use  authRepository.setTutorialCompleted(true)

        // 3  fire remote update
        markTutorialCompletedRemote()
    }
}

