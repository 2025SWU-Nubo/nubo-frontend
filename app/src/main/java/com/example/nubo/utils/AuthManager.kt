package com.example.nubo.utils

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn

object AuthManager {
    fun isUserLoggedIn(context: Context): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null
    }
}
