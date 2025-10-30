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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.ssafy.linkcaretest.api.GoogleLoginRequest
import com.ssafy.linkcaretest.api.KakaoLoginRequest
import com.ssafy.linkcaretest.api.RetrofitClient
import com.ssafy.linkcaretest.ui.theme.LinkCareTestTheme
import kotlinx.coroutines.launch
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient

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
                    onGoogleLoginClick = { signInWithGoogle() },
                    onKakaoLoginClick = { signInWithKakao() }  // ⭐ 카카오 추가
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
                sendGoogleTokenToBackend(idToken)
            } else {
                Log.e("GoogleLogin", "ID Token is null")
                Toast.makeText(this, "ID Token을 가져올 수 없습니다", Toast.LENGTH_SHORT).show()
            }

        } catch (e: ApiException) {
            Log.e("GoogleLogin", "Google Sign-In failed: ${e.statusCode}", e)
            Toast.makeText(this, "Google 로그인 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendGoogleTokenToBackend(idToken: String) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val request = GoogleLoginRequest(idToken)
                val response = RetrofitClient.authApi.googleLogin(request)

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        Log.d("GoogleLogin", "백엔드 로그인 성공: ${loginResponse?.email}")

                        Toast.makeText(
                            this@MainActivity,
                            "Google 로그인 성공!\n이름: ${loginResponse?.name}\n이메일: ${loginResponse?.email}",
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

    private fun signInWithKakao() {
        Log.d("KakaoLogin", "========== 카카오 로그인 버튼 클릭! ==========")

        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            Log.d("KakaoLogin", "========== 콜백 실행됨! ==========")
            Log.d("KakaoLogin", "token = $token")
            Log.d("KakaoLogin", "error = $error")

            if (error != null) {
                Log.e("KakaoLogin", "카카오 로그인 실패", error)
                Toast.makeText(this, "카카오 로그인 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            } else if (token != null) {
                Log.i("KakaoLogin", "카카오 로그인 성공 ${token.accessToken}")
                sendKakaoTokenToBackend(token.accessToken)
            } else {
                Log.e("KakaoLogin", "========== token과 error 둘 다 null! ==========")
            }
        }

        // 카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
        Log.d("KakaoLogin", "카카오톡 설치 여부: ${UserApiClient.instance.isKakaoTalkLoginAvailable(this)}")

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            Log.d("KakaoLogin", "카카오톡으로 로그인 시도")
            UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                Log.d("KakaoLogin", "========== 카카오톡 로그인 콜백 실행됨! ==========")

                if (error != null) {
                    Log.e("KakaoLogin", "카카오톡으로 로그인 실패", error)

                    // 사용자가 취소한 경우
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        Log.d("KakaoLogin", "사용자가 취소함")
                        return@loginWithKakaoTalk
                    }

                    // 카카오톡에 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도
                    Log.d("KakaoLogin", "카카오계정으로 재시도")
                    UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                } else if (token != null) {
                    Log.i("KakaoLogin", "카카오톡으로 로그인 성공 ${token.accessToken}")
                    sendKakaoTokenToBackend(token.accessToken)
                }
            }
        } else {
            Log.d("KakaoLogin", "카카오계정으로 로그인 시도")
            UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
        }
    }

    private fun sendKakaoTokenToBackend(accessToken: String) {
        Log.d("KakaoLogin", "========== sendKakaoTokenToBackend 호출됨! ==========")
        Log.d("KakaoLogin", "accessToken = ${accessToken.take(20)}...")
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                Log.d("KakaoLogin", "백엔드 요청 시작")
                val request = KakaoLoginRequest(accessToken)
                val response = RetrofitClient.authApi.kakaoLogin(request)
                Log.d("KakaoLogin", "백엔드 응답: ${response.code()}")

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        Log.d("KakaoLogin", "백엔드 로그인 성공: ${loginResponse?.email}")

                        Toast.makeText(
                            this@MainActivity,
                            "카카오 로그인 성공!\n이름: ${loginResponse?.name}\n이메일: ${loginResponse?.email}",
                            Toast.LENGTH_LONG
                        ).show()

                        // TODO: JWT 토큰 저장, 메인 화면 이동 등

                    } else {
                        Log.e("KakaoLogin", "백엔드 에러: ${response.code()} - ${response.message()}")
                        Toast.makeText(
                            this@MainActivity,
                            "백엔드 로그인 실패: ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("KakaoLogin", "네트워크 에러", e)
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

@Composable
fun LoginScreen(
    onGoogleLoginClick: () -> Unit,
    onKakaoLoginClick: () -> Unit
) {
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
                text = "LinkCare 로그인",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Google 로그인 버튼
            Button(
                onClick = onGoogleLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Google로 로그인")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 카카오 로그인 버튼
            Button(
                onClick = onKakaoLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFEE500),  // 카카오 노란색
                    contentColor = Color.Black
                )
            ) {
                Text("카카오로 로그인")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LinkCareTestTheme {
        LoginScreen(
            onGoogleLoginClick = {},
            onKakaoLoginClick = {}
        )
    }
}