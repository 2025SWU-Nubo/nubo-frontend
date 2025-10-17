package com.example.nubo.ui.screen.onBoardingLogin

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.components.toast.AppToastLayout
import com.example.components.toast.AppToastType
import com.example.nubo.MainActivity
import com.example.nubo.R
import com.example.nubo.data.model.LoginRequest
import com.example.nubo.data.model.LoginResponse
import com.example.nubo.data.model.TokenValidationResponse
import com.example.nubo.data.model.UserInfo
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.NotificationRepository
import com.example.nubo.data.repository.NotificationRepository.RegisterOutcome
import com.example.nubo.utils.NotificationPermissionHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

data class UiToast(
    val message: String,
    val type: AppToastType = AppToastType.NORMAL,    // import com.example.components.toast.AppToastType
    val layout: AppToastLayout = AppToastLayout.TitleOnly,
    val durationMillis: Int = 2000,
    @androidx.annotation.DrawableRes val iconRes: Int? = null
)

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository,
    private var notificationRepository: NotificationRepository
) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context: Context = application.applicationContext

    private val prefs = context.getSharedPreferences("push_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(OnBoardingUiState())
    val uiState: StateFlow<OnBoardingUiState> get() = _uiState

    private val _toastEvents = MutableSharedFlow<UiToast>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val toastEvents: SharedFlow<UiToast> = _toastEvents.asSharedFlow()


    private suspend fun emitToast(toast: UiToast) {
        _toastEvents.emit(toast)
    }

    // 알림 권한 요청이 필요한지 상태 관리
    private val _shouldRequestNotificationPermission = MutableStateFlow(false)
    val shouldRequestNotificationPermission: StateFlow<Boolean> get() = _shouldRequestNotificationPermission


    private lateinit var googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient

    init {
        initializeGoogleAuth()
    }

    fun toast(
        message: String,
        type: AppToastType = AppToastType.NORMAL,
        duration: Int = 2000
    ) {
        if (!_toastEvents.tryEmit(UiToast(message, type = type, durationMillis = duration))) {
            viewModelScope.launch { _toastEvents.emit(UiToast(message, type = type, durationMillis = duration)) }
        }
    }

    /** 공통: 레포로 토큰 등록 시도 (짧은 로그) */
    private suspend fun registerTokenIfNeeded(token: String) {
        when (val r = notificationRepository.registerDeviceTokenIfNeeded(token)) {
            is RegisterOutcome.Success -> android.util.Log.d("FCM_REG", "VM: server register OK (${token.take(12)}...)")
            is RegisterOutcome.SkippedAlready -> android.util.Log.d("FCM_REG", "VM: skip (already)")
            is RegisterOutcome.SkippedBlank -> android.util.Log.w("FCM_REG", "VM: skip (blank)")
            is RegisterOutcome.Failure -> android.util.Log.w("FCM_REG", "VM: server register FAIL ${r.error.message}")
        }
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
                                // 토큰이 유효할 경우
                                if (tokenResponse.valid) {
                                    clearRegisteredFlag()
                                    registerPushAfterLogin()
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
        authRepository.clearAuthData()
        // 커스텀 토스트로 발행 (NEGATIVE)
        toast("로그인이 만료되었습니다. 다시 로그인해주세요.", type = AppToastType.NEGATIVE)
        showLoginButton()
    }

    private fun showLoginButton() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            showLoginButton = true
        )
    }

    // 권한 체크 후 이동
    private fun checkNotificationPermissionAndNavigate() {
//        if (NotificationPermissionHelper.shouldRequestNotificationPermission(context)) {
//            _shouldRequestNotificationPermission.value = true
//        } else {
//            navigateToMain()
//        }

        navigateToMain()
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

    @SuppressLint("HardwareIds")
    private fun registerPushAfterLogin() {
        FirebaseMessaging.getInstance().isAutoInitEnabled = true

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { fcmToken ->
                if (fcmToken.isNullOrBlank()) {
                    android.util.Log.w("FCM_REG", "getToken OK but empty")
                    return@addOnSuccessListener
                }
                android.util.Log.d("FCM_REG", "getToken OK (${fcmToken.take(12)}...)")
                viewModelScope.launch { registerTokenIfNeeded(fcmToken) }
            }
            .addOnFailureListener { e ->
                // ⬇ FCM이 바로 토큰 못 줄 때 캐시로 보완
                val cached = prefs.getString("latest_fcm_token", null)
                android.util.Log.w("FCM_REG", "getToken FAIL ${e.message}; cached=${cached?.take(12)}...")
                if (!cached.isNullOrBlank()) {
                    viewModelScope.launch { registerTokenIfNeeded(cached) }
                }
            }
    }

    fun ensurePushTokenRegistered() {
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNullOrBlank()) {
                    android.util.Log.w("FCM_REG", "ensure: getToken empty")
                } else {
                    android.util.Log.d("FCM_REG", "ensure: getToken (${token.take(12)}...)")
                    viewModelScope.launch { registerTokenIfNeeded(token) }
                }
            }
            .addOnFailureListener { e ->
                val cached = prefs.getString("latest_fcm_token", null)
                android.util.Log.w("FCM_REG", "ensure: getToken FAIL ${e.message}; cached=${cached?.take(12)}...")
                if (!cached.isNullOrBlank()) {
                    viewModelScope.launch { registerTokenIfNeeded(cached) }
                }
            }
    }

    private fun clearRegisteredFlag() {
        prefs.edit().remove("registered_fcm_token").apply()
        android.util.Log.d("FCM_REG", "clear flag: registered_fcm_token removed")
    }


    private fun saveUserAndNavigate(
        user: UserInfo,
        token: String,
        onLoginComplete: (Intent) -> Unit
    ) {
        authRepository.saveAccessToken(token)
        authRepository.saveUserId(user.id)
        authRepository.saveUserInfo(user)

        // ✅ '이미 등록됨' 스킵 플래그 제거해서 다음 호출이 무조건 시도되게
        getApplication<Application>()
            .getSharedPreferences("push_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove("registered_fcm_token")
            .apply()

        // 로그인 성공시 푸시 토큰 등록 시도 (이제 스킵 안 됨)
        registerPushAfterLogin()

        // 이하 기존 로직 그대로
        if (NotificationPermissionHelper.shouldRequestNotificationPermission(context)) {
            _shouldRequestNotificationPermission.value = true
            _pendingLoginComplete = onLoginComplete
        } else {
            onLoginComplete(Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
        }
    }



//    @SuppressLint("HardwareIds")
//    private fun registerPushAfterLogin() {
//        FirebaseMessaging.getInstance().isAutoInitEnabled = true
//
//        FirebaseMessaging.getInstance().token
//            .addOnSuccessListener { fcmToken ->
//                if (fcmToken.isNullOrBlank()) {
//                    Log.w("FCM", "getToken success but empty")
//                    return@addOnSuccessListener
//                }
//                Log.d("FCM", "token = $fcmToken")
//
//                viewModelScope.launch {
//                    runCatching {
//                        notificationRepository.registerDeviceTokenIfNeeded(fcmToken)
//                    }.onSuccess {
//                        Log.d("FCM", "registerDeviceTokenIfNeeded success")
//                    }.onFailure {
//                        Log.e("FCM", "registerDeviceTokenIfNeeded failed", it)
//                    }
//                }
//            }
//            .addOnFailureListener { e ->
//                Log.w("FCM", "getToken failed", e)
//            }
//    }

//    fun ensurePushTokenRegistered() {
//        FirebaseMessaging.getInstance().isAutoInitEnabled = true
//        FirebaseMessaging.getInstance().token
//            .addOnSuccessListener { token ->
//                if (!token.isNullOrBlank()) {
//                    viewModelScope.launch {
//                        runCatching { notificationRepository.registerDeviceTokenIfNeeded(token) }
//                    }
//                }
//            }
//            .addOnFailureListener { e -> Log.w("FCM", "getToken failed", e) }
//    }


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
