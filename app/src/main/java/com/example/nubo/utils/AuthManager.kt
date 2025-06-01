package com.example.nubo.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.nubo.data.model.UserInfo
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.gson.Gson
import androidx.core.content.edit

object AuthManager {
    private const val PREF_NAME = "AuthPrefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_INFO = "user_info"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Save AccessToken
     */
    fun saveAccessToken(context: Context, token: String) {
        getPreferences(context).edit() { putString(KEY_ACCESS_TOKEN, token) }
    }

    /**
     * Load AccessToken
     */
    fun getAccessToken(context: Context): String? {
        return getPreferences(context).getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Save User Info
     */
    fun saveUserInfo(context: Context, userInfo: UserInfo) {
        val json = Gson().toJson(userInfo)
        getPreferences(context).edit() { putString(KEY_USER_INFO, json) }
    }

    /**
     * Load User Info
     */
    fun getUserInfo(context: Context): UserInfo? {
        val json = getPreferences(context).getString(KEY_USER_INFO, null)
        return if (json != null) {
            Gson().fromJson(json, UserInfo::class.java)
        } else {
            null
        }
    }

    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(context: Context): Boolean {
        return getAccessToken(context) != null
    }

    /**
     * Logout
     */
    fun logout(context: Context) {
        getPreferences(context).edit() { clear() }
    }
}
