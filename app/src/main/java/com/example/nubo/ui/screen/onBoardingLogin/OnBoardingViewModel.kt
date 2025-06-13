package com.example.nubo.ui.screen.onBoardingLogin

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.MainActivity
import com.example.nubo.R
import com.example.nubo.data.model.LoginRequest
import com.example.nubo.data.model.LoginResponse
import com.example.nubo.data.model.TokenValidationResponse
import com.example.nubo.data.model.UserInfo
import com.example.nubo.data.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository
) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context: Context = application.applicationContext

    private val _uiState = MutableStateFlow(OnBoardingUiState())
    val uiState: StateFlow<OnBoardingUiState> get() = _uiState

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> get() = _toastMessage


    private lateinit var googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient

    init {
        initializeGoogleAuth()
    }

    private fun initializeGoogleAuth() {
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestServerAuthCode(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun onStartButtonClicked() {
        _uiState.value = _uiState.value.copy(
            logoShrinked = true,
            isLoading = true
        )
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        if (authRepository.isLoggedIn()) {
            validateTokenWithServer()
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                showLoginButton = true
            )
        }
    }

    // 서버에 토큰 유효성 검증 요청
    private fun validateTokenWithServer() {
        viewModelScope.launch {
            try {
                val tokenCheckCall = authRepository.checkToken()
                if (tokenCheckCall == null) {
                    // 토큰이 없으면 로그인 화면 표시
                    showLoginButton()
                    return@launch
                }

                tokenCheckCall.enqueue(object : Callback<TokenValidationResponse> {
                    override fun onResponse(call: Call<TokenValidationResponse>, response: Response<TokenValidationResponse>) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        if (response.isSuccessful) {
                            response.body()?.let { tokenResponse ->
                                if (tokenResponse.valid) {
                                    // 토큰이 유효하면 메인으로 이동
                                    navigateToMain()
                                } else {
                                    // 토큰이 유효하지 않으면 로그아웃 처리
                                    handleTokenExpired()
                                }
                            } ?: run {
                                // 응답 body가 null이면 로그인 화면 표시
                                showLoginButton()
                            }
                        } else if (response.code() == 401 || response.code() == 403) {
                            // 토큰이 만료되었으면 로그아웃 처리
                            handleTokenExpired()
                        } else {
                            // 기타 에러는 로그인 화면 표시
                            showLoginButton()
                        }
                    }

                    override fun onFailure(call: Call<TokenValidationResponse>, t: Throwable) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        Log.e("TokenValidation", "Network error: ${t.localizedMessage}")
                        // 네트워크 에러시에는 로그인 화면을 보여줌
                        showLoginButton()
                    }
                })
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                Log.e("TokenValidation", "Error validating token", e)
                showLoginButton()
            }
        }
    }


    // 토큰 만료 처리
    private fun handleTokenExpired() {
        Log.i("Auth", "Token expired, clearing stored data")
        authRepository.clearAuthData() // 모든 인증 데이터 삭제
        _toastMessage.value = "로그인이 만료되었습니다. 다시 로그인해주세요."
        showLoginButton()
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    private fun showLoginButton() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            showLoginButton = true
        )
    }


    fun handleSignInResult(task: Task<GoogleSignInAccount>, onLoginComplete: (Intent) -> Unit) {
        viewModelScope.launch {
            try {
                val account = task.getResult(ApiException::class.java)
                val authCode = account.serverAuthCode
                authCode?.let {
                    sendAuthCodeToServer(it, onLoginComplete)
                }
            } catch (e: ApiException) {
                Log.w("GoogleSignIn", "Google sign in failed", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }


    private fun sendAuthCodeToServer(authCode: String, onLoginComplete: (Intent) -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val request = LoginRequest(authCode)

        authRepository.loginWithGoogle(request)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    if (response.isSuccessful) {
                        response.body()?.let { loginResponse ->
                            val existingUser = authRepository.getUserInfo()
                            val newUser = loginResponse.user

                            if (existingUser != null && existingUser.id != newUser.id) {
                                _uiState.value = _uiState.value.copy(
                                    existingUser = existingUser,
                                    loginResponseUser = newUser
                                )
                            } else {
                                saveUserAndNavigate(newUser, loginResponse.accessToken, onLoginComplete)
                            }
                        }
                    } else {
                        Log.e("OAuth", "Response error: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    Log.e("Server", "Network error: ${t.localizedMessage}")
                }
            })
    }

    fun confirmAccountSwitch(onLoginComplete: (Intent) -> Unit) {
        val newUser = _uiState.value.loginResponseUser ?: return
        val accessToken = authRepository.getAccessToken() ?: return
        saveUserAndNavigate(newUser, accessToken, onLoginComplete)
    }

    private fun saveUserAndNavigate(
        user: UserInfo,
        token: String,
        onLoginComplete: (Intent) -> Unit
    ) {
        authRepository.saveAccessToken(token)
        authRepository.saveUserId(user.id)
        authRepository.saveUserInfo(user)
        onLoginComplete(Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }

    private fun navigateToMain() {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
    }
}
