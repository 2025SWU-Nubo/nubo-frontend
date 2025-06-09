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
            navigateToMain()
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                showLoginButton = true
            )
        }
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
