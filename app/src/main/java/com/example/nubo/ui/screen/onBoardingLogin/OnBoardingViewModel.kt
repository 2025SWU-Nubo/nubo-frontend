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
import com.example.nubo.utils.NotificationPermissionHelper
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

    // 알림 권한 요청이 필요한지 상태 관리
    private val _shouldRequestNotificationPermission = MutableStateFlow(false)
    val shouldRequestNotificationPermission: StateFlow<Boolean> get() = _shouldRequestNotificationPermission


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
        _uiState.value = _uiState.value.copy(isLoading = true)
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        if (authRepository.isLoggedIn()) {
            validateTokenWithServer()
        } else {
            // 로그인되지 않은 경우에만 로고 축소 및 로그인 버튼 표시
            _uiState.value = _uiState.value.copy(
                logoShrinked = true,
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
                                    // 토큰이 유효하면 알림 권한 확인 후 메인으로 이동
                                    checkNotificationPermissionAndNavigate()
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

    // 알림 권한 확인 후 메인 화면으로 이동
    private fun checkNotificationPermissionAndNavigate() {
        if (NotificationPermissionHelper.shouldRequestNotificationPermission(context)) {
            // 알림 권한 요청이 필요한 경우
            _shouldRequestNotificationPermission.value = true
        } else {
            // 알림 권한이 이미 있거나 필요하지 않은 경우 바로 메인으로 이동
            navigateToMain()
        }
    }

    // 알림 권한 요청 후 메인 화면으로 이동
//    fun onNotificationPermissionHandled() {
//        _shouldRequestNotificationPermission.value = false
//        navigateToMain()
//    }

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

        // 로그인 성공 후 알림 권한 확인
        if (NotificationPermissionHelper.shouldRequestNotificationPermission(context)) {
            _shouldRequestNotificationPermission.value = true
            // onLoginComplete는 알림 권한 처리 후 호출됨
            _pendingLoginComplete = onLoginComplete
        } else {
            // 알림 권한이 이미 있거나 필요하지 않은 경우 바로 메인으로 이동
            onLoginComplete(Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
        }
    }

    // 로그인 완료 콜백을 임시 저장
    private var _pendingLoginComplete: ((Intent) -> Unit)? = null

    // 로그인 후 알림 권한 처리 완료
    fun onLoginNotificationPermissionHandled() {
        _shouldRequestNotificationPermission.value = false
        _pendingLoginComplete?.let { complete ->
            complete(Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            _pendingLoginComplete = null
            return
        }

        // Otherwise we came from token-validated path
        navigateToMain()
    }

    private fun navigateToMain() {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
    }
}
