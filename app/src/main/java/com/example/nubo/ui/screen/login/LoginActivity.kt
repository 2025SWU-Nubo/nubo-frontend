package com.example.nubo.ui.screen.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.nubo.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.nubo.R
import androidx.compose.ui.res.painterResource
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey700
import com.example.nubo.ui.theme.PurpleMain500
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment


class LoginActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // GoogleSignInClient 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            LoginScreen {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }
    }
}

//@Composable
//fun LoginScreen(onGoogleLoginClick: () -> Unit) {
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(32.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center,
//            modifier = Modifier.align(Alignment.Center)
//        ) {
//            // 메인 로고 텍스트
//            Image(
//                painter = painterResource(id = R.drawable.nubo_logo),
//                contentDescription = "Nubo 로고",
//                modifier = Modifier
//                    .width(110.dp)
//                    .height(60.dp),
//                contentScale = ContentScale.Fit
//            )
//            Text("간편하게 로그인하고", style = AppTextStyles.title_regular_24)
//            Text("다양한 서비스를 이용하세요.", style = AppTextStyles.title_bold_24)
//        }
//
//        // Google 로그인 버튼 스타일을 맞춘 카드 형태
//        Button(
//            onClick = onGoogleLoginClick,
//            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
//            shape = RoundedCornerShape(12.dp),
//            modifier = Modifier
//                .fillMaxWidth()
//                .border(
//                    width = 1.dp,
//                    color = Color.Gray, // 회색 테두리
//                    shape = RoundedCornerShape(12.dp)
//                )
//                .padding(2.dp)
//        ) {
//            Icon(
//                painter = painterResource(id = R.drawable.btn_google_logo),
//                contentDescription = "Google Logo",
//                tint = Color.White // 원래 색상 유지
//            )
//            Spacer(modifier = Modifier.width(8.dp))
//            Text("Google로 로그인하기", color = Color.Black)
//        }
//    }
//}

@Composable
fun LoginScreen(onGoogleLoginClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp,30.dp,30.dp,bottom = 75.dp)
    ) {
        // 중앙 정렬: 로고 + 텍스트 묶음
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Image(
                painter = painterResource(id = R.drawable.nubo_logo),
                contentDescription = "Nubo 로고",
                modifier = Modifier
                    .width(110.dp)
                    .height(60.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(16.dp)) // 로고와 텍스트 간 간격
            Text("간편하게 로그인하고", style = AppTextStyles.title_regular_24)
            Spacer(modifier = Modifier.height(4.dp))
            Text("다양한 서비스를 이용하세요.", style = AppTextStyles.title_bold_24)
        }

        // 하단 정렬: Google 로그인 버튼
        Button(
            onClick = onGoogleLoginClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter) // ⬅️ 하단 고정
                .fillMaxWidth()
                .height(50.dp)
                .border(
                    width = 1.dp,
                    color = Color.Gray,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.btn_google_logo),
                contentDescription = "Google Logo",
                tint = Color.Unspecified // 아이콘 원본 색상 유지
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Google로 로그인하기", color = Color.Black, style = AppTextStyles.b3_medium_14)
        }
    }
}

