package com.ssafy.linkcaretest

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.ssafy.linkcaretest.api.GoogleLoginRequest
import com.ssafy.linkcaretest.api.RetrofitClient
import com.ssafy.linkcaretest.ui.theme.LinkCareTestTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    // Google Sign-In Result Launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } else {
            Log.e("GoogleLogin", "Google Sign-In cancelled")
            Toast.makeText(this, "Google 로그인이 취소되었습니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Google Sign-In 옵션 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))  // 웹 클라이언트 ID
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            LinkCareTestTheme {
                LoginScreen(
                    onGoogleLoginClick = { signInWithGoogle() }
                )
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val idToken = account?.idToken

            if (idToken != null) {
                Log.d("GoogleLogin", "ID Token 획득 성공")
                sendTokenToBackend(idToken)
            } else {
                Log.e("GoogleLogin", "ID Token is null")
                Toast.makeText(this, "ID Token을 가져올 수 없습니다", Toast.LENGTH_SHORT).show()
            }

        } catch (e: ApiException) {
            Log.e("GoogleLogin", "Google Sign-In failed: ${e.statusCode}", e)
            Toast.makeText(this, "Google 로그인 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendTokenToBackend(idToken: String) {
        // Coroutine으로 네트워크 요청
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val request = GoogleLoginRequest(idToken)
                val response = RetrofitClient.authApi.googleLogin(request)

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        Log.d("GoogleLogin", "백엔드 로그인 성공: ${loginResponse?.email}")

                        // 성공 메시지 표시
                        Toast.makeText(
                            this@MainActivity,
                            "로그인 성공!\n이름: ${loginResponse?.name}\n이메일: ${loginResponse?.email}",
                            Toast.LENGTH_LONG
                        ).show()

                        // TODO: JWT 토큰 저장, 메인 화면 이동 등

                    } else {
                        Log.e("GoogleLogin", "백엔드 에러: ${response.code()} - ${response.message()}")
                        Toast.makeText(
                            this@MainActivity,
                            "백엔드 로그인 실패: ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("GoogleLogin", "네트워크 에러", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "네트워크 에러: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}

// 제 LoginScreen 함수
@Composable
fun LoginScreen(onGoogleLoginClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LinkCare Google 로그인 테스트",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = onGoogleLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Google로 로그인")
            }
        }
    }
}

// Preview 전용 함수 (별도로 분리)
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LinkCareTestTheme {
        LoginScreen(onGoogleLoginClick = {})
    }
}