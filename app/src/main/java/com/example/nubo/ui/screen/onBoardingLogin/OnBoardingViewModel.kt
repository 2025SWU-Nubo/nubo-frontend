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
import androidx.core.content.edit
import com.google.gson.Gson

data class UiToast(
    val message: String,
    val type: AppToastType = AppToastType.NORMAL,    // import com.example.components.toast.AppToastType
    val layout: AppToastLayout = AppToastLayout.TitleOnly,
    val durationMillis: Int = 2000,
    @androidx.annotation.DrawableRes val iconRes: Int? = null
)



/**
 * 온보딩/로그인 단계의 토큰 수명주기 중심 ViewModel
 * - 로그인 성공 직후: FCM 토큰을 서버에 업서트(등록/갱신)
 * - 토큰 만료/계정 전환/로그아웃: 서버 매핑 삭제 + 로컬 FCM 토큰 삭제(deleteToken)
 * - 앱 진입 시: ensurePushTokenRegistered()로 재시도 보장
 */
@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository,
    private var notificationRepository: NotificationRepository
) : AndroidViewModel(application) {
    companion object{
        private const val FORCE_SHOW_INTEREST = true
    }

    @SuppressLint("StaticFieldLeak")
    private val context: Context = application.applicationContext

    // 새 사용자 여부를 저장할 변수 추가
    private var _pendingIsNewUser: Boolean = false


    // FCM 토큰 캐시/등록 상태 저장용
    private val prefs = context.getSharedPreferences("push_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(OnBoardingUiState())
    val uiState: StateFlow<OnBoardingUiState> get() = _uiState

    private val _toastEvents = MutableSharedFlow<UiToast>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val toastEvents: SharedFlow<UiToast> = _toastEvents.asSharedFlow()

    // 알림 권한 요청이 필요한지 상태 관리
    private var pendingAccessToken: String? = null
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
            // ✅ AccessToken 이 존재 → 서버에 유효성 검사
            validateTokenWithServer()
        } else {
            // ❌ AccessToken 없음 → 로그인 버튼 표시
            showLoginButton()
        }
    }

    /** 서버 AccessToken 유효성 검사 → 통과 시 푸시 토큰 업서트, 실패 시 로그아웃 처리 */
    private fun validateTokenWithServer() {
        viewModelScope.launch {
            try {
                val call = authRepository.checkToken()
                if (call == null) {
                    // 토큰이 없으면 로그인 화면 표시
                    showLoginButton(); return@launch
                }

                call.enqueue(object : Callback<TokenValidationResponse> {
                    override fun onResponse(
                        call: Call<TokenValidationResponse>,
                        response: Response<TokenValidationResponse>
                    ) {
                        _uiState.value = _uiState.value.copy(isLoading = false)

                        Log.d("TokenCheck", "code=${response.code()}")
                        Log.d("TokenCheck", "body=${Gson().toJson(response.body())}")
                        Log.d("TokenCheck", "error=${response.errorBody()?.string()}")

                        if (response.isSuccessful) {
                            val result = response.body()
                            if (result?.valid == true) {
                                // 토큰 유효 → 바로 메인으로 이동
                                registerPushAfterLogin()
                                navigateToMain()
                            } else {
                                // 토큰 만료 → 로그인 필요
                                handleTokenExpired()
                            }
                        } else if (response.code() in listOf(401, 403)) {
                            handleTokenExpired()
                        } else {
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

    private fun showLoginButton() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            showLoginButton = true
        )
    }

    /** 권한 체크 후 메인으로 이동(필요 시 권한 다이얼로그를 띄워도 됨) */
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

    /** 로그인 API 호출 → 성공 시 사용자/토큰 저장 → 푸시 토큰 업서트 트리거 */
    private fun sendAuthCodeToServer(authCode: String, onLoginComplete: (Intent) -> Unit) {
        // 계정 전환 대비: 서버의 기존 디바이스-토큰 매핑 제거
        viewModelScope.launch {
            runCatching { notificationRepository.deleteRegisteredTokenIfAny() }
        }

        _uiState.value = _uiState.value.copy(isLoading = true)
        val request = LoginRequest(authCode)

        authRepository.loginWithGoogle(request)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    _uiState.value = _uiState.value.copy(isLoading = false)

                    Log.d("LoginResp", "code=${response.code()}")                    // HTTP status
                    Log.d("LoginResp", "body=${Gson().toJson(response.body())}")     // 성공 바디(JSON)
                    Log.d("LoginResp", "error=${response.errorBody()?.string()}")    // 실패 바디(raw)


                    if (!response.isSuccessful) {
                        Log.e("OAuth", "Response error: ${response.errorBody()?.string()}"); return
                    }
                    val loginRes = response.body() ?: return
                    val existingUser = authRepository.getUserInfo()
                    val newUser = loginRes.user
                    pendingAccessToken = loginRes.accessToken

                    // isNewUser 저장
                    _uiState.value = _uiState.value.copy(isNewUser = loginRes.isNewUser)

                    if (existingUser != null && existingUser.id != newUser.id) {
                        _uiState.value = _uiState.value.copy(existingUser = existingUser, loginResponseUser = newUser)
                    } else {
                        saveUserAndNavigate(newUser, loginRes.accessToken, onLoginComplete)
                        pendingAccessToken = null
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    Log.e("Server", "Network error: ${t.localizedMessage}")
                }
            })
    }



    /** 로그인 성공 직후: FCM 토큰을 서버에 업서트(등록/갱신) */
    @SuppressLint("HardwareIds")
    private fun registerPushAfterLogin() {
        FirebaseMessaging.getInstance().isAutoInitEnabled = true

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { fcmToken ->
                if (fcmToken.isNullOrBlank()) {
                    Log.w("FCM_REG", "getToken OK but empty")
                    return@addOnSuccessListener
                }
                Log.d("FCM_REG", "getToken OK (${fcmToken}...)")

                // 최신 토큰 로컬 캐시(중복 apply 금지: KTX가 자동 적용)
                prefs.edit() { putString("latest_fcm_token", fcmToken)}

                // 서버 업서트(force=true로 반드시 갱신)
                viewModelScope.launch { notificationRepository.registerDeviceTokenIfNeeded(fcmToken, force = true) }

            }
            .addOnFailureListener { e ->
                // FCM이 바로 토큰 못 줄 때 캐시로 보완
                val cached = prefs.getString("latest_fcm_token", null)
                Log.w("FCM_REG", "getToken FAIL ${e.message}; cached=${cached}...")
                if (!cached.isNullOrBlank()) {
                    viewModelScope.launch {
                        notificationRepository.registerDeviceTokenIfNeeded(cached, force = true)
                    }
                }
            }
    }

    /** 토큰 만료/401/403 등 AccessToken 무효 → 로그아웃 처리 + 디바이스 토큰 정리 */
    private fun handleTokenExpired() {
        Log.i("Auth", "Token expired, clearing stored data")
        authRepository.clearAuthData()

        viewModelScope.launch {
            // 서버 매핑 삭제
            runCatching { notificationRepository.deleteRegisteredTokenIfAny() }

            // 로컬 FCM 토큰 삭제 → 다음 진입 시 새 토큰 발급
            runCatching { FirebaseMessaging.getInstance().deleteToken() }
        }

        toast("로그인이 만료되었습니다. 다시 로그인해주세요.", type = AppToastType.NEGATIVE)
        showLoginButton()
    }

    /** 계정 전환 시: 서버/로컬 모두 토큰 정리 후 새 계정으로 저장 및 이동 */
    fun confirmAccountSwitch(onLoginComplete: (Intent) -> Unit) {
        val newUser = _uiState.value.loginResponseUser ?: return
        val accessTokenFromLogin = pendingAccessToken

        if (accessTokenFromLogin.isNullOrEmpty()) {
            val fallback = authRepository.getAccessToken() ?: return
            saveUserAndNavigate(newUser, fallback, onLoginComplete)
        } else {
            saveUserAndNavigate(newUser, accessTokenFromLogin, onLoginComplete)
            pendingAccessToken = null
        }

        viewModelScope.launch {
            // 서버 삭제 및 로컬 FCM 삭제 병행
            runCatching { notificationRepository.deleteRegisteredTokenIfAny() }
            runCatching { FirebaseMessaging.getInstance().deleteToken() }
        }
    }

    /** 앱 진입 시 토큰 업서트 보장(재시도 지점) */
    fun ensurePushTokenRegistered(force: Boolean = false) {
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNullOrBlank()) {
                    android.util.Log.w("FCM_REG", "ensure: getToken empty")
                } else {
                    android.util.Log.d("FCM_REG", "ensure: getToken (${token.take(12)}...)")
//                    viewModelScope.launch { registerTokenIfNeeded(token) }
                    viewModelScope.launch { notificationRepository.registerDeviceTokenIfNeeded(token, force = force) }

                }
            }
            .addOnFailureListener { e ->
                val cached = prefs.getString("latest_fcm_token", null)
                android.util.Log.w("FCM_REG", "ensure: getToken FAIL ${e.message}; cached=${cached?.take(12)}...")
                if (!cached.isNullOrBlank()) {
//                    viewModelScope.launch { registerTokenIfNeeded(cached) }
                    viewModelScope.launch { notificationRepository.registerDeviceTokenIfNeeded(cached, force = force) }

                }
            }
    }

    /** 서버 측 “이미 등록됨” 플래그를 초기화(다음 업서트 강제 트리거 목적) */
    private fun clearRegisteredFlag() {
        prefs.edit {
            remove("registered_fcm_token")
            remove("registered_at_epoch")
        }
        Log.d("FCM_REG", "clear flag: registered_fcm_token removed")
    }

    // 로그인 완료 콜백을 임시 저장
    private var _pendingLoginComplete: ((Intent) -> Unit)? = null

    // 로그인 후 알림 권한 처리 완료
    fun onLoginNotificationPermissionHandled() {
        _shouldRequestNotificationPermission.value = false
        _pendingLoginComplete?.let { complete ->
            complete(Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                // 새 사용자면 관심사 설정 플래그 추가
                if (_pendingIsNewUser) {
                    putExtra("EXTRA_NEEDS_INTEREST", true)
                }
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

    fun getGoogleSignInIntentWrapper(): Intent = googleSignInClient.signInIntent

    private fun saveUserAndNavigate(
        user: UserInfo,
        token: String,
        onLoginComplete: (Intent) -> Unit
    ) {
        authRepository.saveAccessToken(token)
        authRepository.saveUserId(user.id)
        authRepository.saveUserInfo(user)

        // '이미 등록됨' 스킵 플래그 제거해서 다음 호출이 무조건 시도되게
        getApplication<Application>()
            .getSharedPreferences("push_prefs", Context.MODE_PRIVATE)
            .edit() {
                remove("registered_fcm_token")
            }

        // 로그인 성공시 푸시 토큰 등록 시도 (이제 스킵 안 됨)
        registerPushAfterLogin()

        // UiState에서 isNewUser 가져오기
        val realIsNewUser = _uiState.value.isNewUser
        val isNewUser = if (FORCE_SHOW_INTEREST) true else realIsNewUser

        // 이하 기존 로직 그대로
        if (NotificationPermissionHelper.shouldRequestNotificationPermission(context)) {
            _shouldRequestNotificationPermission.value = true
            _pendingLoginComplete = onLoginComplete
            _pendingIsNewUser = isNewUser
        } else {
            // 권한 요청이 필요없을 때도 isNewUser 체크
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                if (isNewUser) putExtra("EXTRA_NEEDS_INTEREST", true)
            }
            onLoginComplete(intent)
        }
    }
}
