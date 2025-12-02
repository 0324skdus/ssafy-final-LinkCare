package com.a307.linkcare.feature.auth.ui.login

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.a307.linkcare.MainActivity
import com.a307.linkcare.R
import com.a307.linkcare.common.component.atoms.*
import com.a307.linkcare.common.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.auth.model.OAuthToken
import com.a307.linkcare.feature.auth.data.model.request.KakaoLoginRequest
import com.a307.linkcare.feature.auth.data.model.request.GoogleLoginRequest
import com.a307.linkcare.common.network.client.RetrofitClient
import com.a307.linkcare.common.network.store.TokenStore
import com.a307.linkcare.feature.auth.data.model.request.LoginRequest
import com.a307.linkcare.feature.auth.data.model.response.LoginResponse
import com.a307.linkcare.feature.auth.data.model.request.UpdateFcmTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import com.a307.linkcare.feature.watch.manager.DataLayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun LoginScreen(
    navController: NavHostController,
    onLoginSuccess: (isFirstLogin: Boolean) -> Unit = {},
    onSignupClick: () -> Unit = {}
) {
    val TAG = "LoginScreen"
    val onLoginSuccessState by rememberUpdatedState(newValue = onLoginSuccess)

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val duckAspect = 462f / 281f

    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()
    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    // Google Sign-In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.web_client_id))
            .requestEmail()
            .build()
    }
    val googleClient = remember { GoogleSignIn.getClient(activity, gso) }

    LaunchedEffect(Unit) {
        try {
            googleClient.signOut()
            Log.d(TAG, "Cleared previous Google sign-in session")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear Google session: ${e.message}")
        }
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Google sign-in result: resultCode=${result.resultCode}, data=${result.data != null}")
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (!idToken.isNullOrBlank()) {
                    Log.d(TAG, "Google sign-in success: email=${account.email}")
                    scope.launch {
                        try {
                            val res = RetrofitClient.authApi.googleLogin(GoogleLoginRequest(idToken))
                            if (res.isSuccessful && res.body() != null) {
                                val auth: LoginResponse = res.body()!!
                                TokenStore(context).save(auth)

                                // 알림 권한 요청 (Android 13+)
                                requestNotificationPermissionAfterLogin(activity)

                                // FCM 토큰 발급 및 백엔드 전송
                                sendFcmTokenToServer(context, auth.accessToken)

                                // 워치에 테마 동기화
                                syncThemeToWatch(context)

                                // needsProfileCompletion == isFirstLogin
                                onLoginSuccessState(auth.needsProfileCompletion)
                            } else {
                                Log.e(TAG, "Backend Google login failed: code=${res.code()} body=${res.errorBody()?.string()}")
                                toast("서버 구글 로그인 실패")
                            }
                        } catch (ce: CancellationException) {
                            // ignore
                        } catch (e: Exception) {
                            Log.e(TAG, "Backend Google login error", e)
                            toast("서버 통신 오류")
                        }
                    }
                } else {
                    Log.e(TAG, "Google ID token is null")
                    toast("구글 로그인 토큰을 가져오지 못했어요.")
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Google sign-in failed: code=${e.statusCode}, msg=${e.message}", e)
                when (e.statusCode) {
                    12501 -> toast("구글 로그인이 취소되었어요.")
                    12500 -> toast("구글 로그인 설정 오류입니다.")
                    else -> toast("구글 로그인 실패")
                }
            } catch (e: Exception) {
                Log.e(TAG,"Unexpected error: ${e.message}", e)
                toast("구글 로그인 중 오류가 발생했어요.")
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            Log.w(TAG, "User canceled Google sign-in")
            toast("구글 로그인이 취소되었어요.")
        } else {
            Log.w(TAG, "Unexpected result code: ${result.resultCode}")
            toast("구글 로그인 실패")
        }
    }

    fun startGoogle() {
        googleClient.signOut().addOnCompleteListener {
            try {
                googleLauncher.launch(googleClient.signInIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "구글 로그인을 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun startKakao() {
        val userApi = UserApiClient.instance

        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                    Log.i(TAG, "Kakao login canceled")
                    toast("카카오 로그인이 취소되었어요.")
                } else {
                    Log.e(TAG, "Kakao login failed: ${error.message}", error)
                    toast("카카오 로그인 실패")
                }
            } else if (token != null) {
                val kakaoAccessToken = token.accessToken
                Log.d(TAG, "Kakao login success: tokenLen=${kakaoAccessToken.length}")
                scope.launch {
                    try {
                        val res = RetrofitClient.authApi.kakaoLogin(KakaoLoginRequest(kakaoAccessToken))
                        if (res.isSuccessful && res.body() != null) {
                            val auth: LoginResponse = res.body()!!
                            TokenStore(context).save(auth)

                            // 알림 권한 요청 (Android 13+)
                            requestNotificationPermissionAfterLogin(activity)

                            // FCM 토큰 발급 및 백엔드 전송
                            sendFcmTokenToServer(context, auth.accessToken)

                            // 워치에 테마 동기화
                            syncThemeToWatch(context)

                            onLoginSuccessState(auth.needsProfileCompletion)
                        } else {
                            Log.e(TAG, "Backend Kakao login failed: code=${res.code()} body=${res.errorBody()?.string()}")
                            toast("서버 카카오 로그인 실패")
                        }
                    } catch (ce: CancellationException) {
                        // ignore
                    } catch (e: Exception) {
                        Log.e(TAG, "Backend Kakao login error", e)
                        toast("서버 통신 오류")
                    }
                }
            } else {
                Log.e(TAG, "Kakao token is null")
                toast("카카오 토큰을 가져오지 못했어요.")
            }
        }

        try {
            if (userApi.isKakaoTalkLoginAvailable(context)) {
                Log.d(TAG, "Trying KakaoTalk login")
                userApi.loginWithKakaoTalk(context) { token, error ->
                    if (error != null) {
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            callback(null, error)
                        } else {
                            Log.w(TAG, "KakaoTalk login failed, fallback to account: ${error.message}")
                            userApi.loginWithKakaoAccount(context, callback = callback)
                        }
                    } else {
                        callback(token, null)
                    }
                }
            } else {
                Log.d(TAG, "KakaoTalk not available, using account login")
                userApi.loginWithKakaoAccount(context, callback = callback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Kakao login exception: ${e.message}", e)
            toast("카카오 로그인을 시작할 수 없습니다.")
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(main)
    ) {
        val duckHeight = maxWidth / duckAspect
        val contentHeight = (maxHeight - duckHeight).coerceAtLeast(0.dp)

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(contentHeight)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "LinkCare",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = white,
                    modifier = Modifier.padding(top = 150.dp, bottom = 32.dp)
                )

                LoginForm(
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it }
                )

                Spacer(Modifier.height(20.dp))

                // 이메일/비밀번호 로그인
                LcBtn(
                    text = "로그인",
                    buttonColor = point,
                    onClick = {
                        scope.launch {
                            try {
                                val res = RetrofitClient.authApi.login(
                                    LoginRequest(email = email, password = password)
                                )
                                if (res.isSuccessful && res.body() != null) {
                                    val auth: LoginResponse = res.body()!!
                                    TokenStore(context).save(auth)

                                    // 알림 권한 요청 (Android 13+)
                                    requestNotificationPermissionAfterLogin(activity)

                                    // FCM 토큰 발급 및 백엔드 전송
                                    sendFcmTokenToServer(context, auth.accessToken)

                                    // 워치에 테마 동기화
                                    syncThemeToWatch(context)

                                    onLoginSuccessState(auth.needsProfileCompletion) // == isFirstLogin
                                } else {
                                    Log.e(TAG, "Login failed: code=${res.code()}, body=${res.errorBody()?.string()}")
                                    toast("이메일 또는 비밀번호를 확인해주세요.")
                                }
                            } catch (ce: CancellationException) {
                                // ignore
                            } catch (e: Exception) {
                                Log.e(TAG, "Login error", e)
                                toast("서버 통신 오류")
                            }
                        }
                    },
                    isEnabled = email.isNotBlank() && password.isNotBlank()
                )

                Spacer(Modifier.height(36.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Divider(color = white, thickness = 1.dp, modifier = Modifier.weight(1f))
                    Text(
                        text = "또는",
                        color = white,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Divider(color = white, thickness = 1.dp, modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.google_login),
                        contentDescription = "google",
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { startGoogle() },
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFFFEE500))
                            .clickable { startKakao() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.kakao_login),
                            contentDescription = "kakao",
                            modifier = Modifier.size(44.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "계정이 없으신가요?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = white
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "회원가입",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = white,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { onSignupClick() }
                    )
                }
            }
        }

        Image(
            painter = painterResource(id = R.drawable.main_duck),
            contentDescription = "main_duck",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(duckHeight),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun LoginForm(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit
) {
    LcInputField(
        value = email,
        onValueChange = onEmailChange,
        placeholder = "이메일"
    )
    Spacer(modifier = Modifier.height(16.dp))
    LcInputField(
        value = password,
        onValueChange = onPasswordChange,
        placeholder = "비밀번호",
        isPassword = true
    )
}

/**
 * 로그인 성공 후 알림 권한 요청
 * Android 13 (API 33) 이상에서만 실행됩니다
 */
private fun requestNotificationPermissionAfterLogin(activity: Activity) {
    if (activity is MainActivity) {
        activity.requestNotificationPermission()
    } else {
        Log.w("LoginScreen", "Activity is not MainActivity, cannot request notification permission")
    }
}

/**
 * FCM 토큰을 발급받고 백엔드 서버에 전송하는 함수
 */
private fun sendFcmTokenToServer(context: Context, accessToken: String) {
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (!task.isSuccessful) {
            Log.w("LoginScreen", "FCM 토큰 가져오기 실패", task.exception)
            return@addOnCompleteListener
        }

        // FCM 토큰 획득 성공
        val fcmToken = task.result
        Log.d("LoginScreen", "FCM 토큰 획득: $fcmToken")

        // 백엔드에 FCM 토큰 전송
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.authApi.updateFcmToken(
                    token = "Bearer $accessToken",
                    request = UpdateFcmTokenRequest(fcmToken)
                )
                if (response.isSuccessful) {
                    Log.d("LoginScreen", "FCM 토큰 백엔드 전송 성공")
                } else {
                    Log.e("LoginScreen", "FCM 토큰 백엔드 전송 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("LoginScreen", "FCM 토큰 백엔드 전송 중 에러", e)
            }
        }
    }
}

/**
 * 로그인 성공 후 사용자의 테마 설정을 워치에 동기화하는 함수
 */
private fun syncThemeToWatch(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // 서버에서 사용자 정보 조회
            val userResponse = RetrofitClient.userApi.getMyInfo()

            if (userResponse.isSuccessful && userResponse.body() != null) {
                val userData = userResponse.body()!!

                // URL에서 캐릭터 ID와 배경 ID 추출
                val charId = extractCharacterId(userData.mainCharacterImageUrl) ?: 1
                val bgId = extractBackgroundId(userData.mainBackgroundImageUrl) ?: 1

                // 워치에 테마 전송
                DataLayerManager.sendTheme(context, charId, bgId)
                Log.d("LoginScreen", "테마 워치 동기화 완료: char=$charId, bg=$bgId")
            } else {
                Log.e("LoginScreen", "사용자 정보 조회 실패: ${userResponse.code()}")
            }
        } catch (e: Exception) {
            Log.e("LoginScreen", "테마 워치 동기화 실패", e)
        }
    }
}

/**
 * 캐릭터 이미지 URL에서 캐릭터 ID 추출
 * URL 형식: Characters/bear/v1/base.png, Characters/duck/v2/base.png 등
 * 워치 ID 매핑: 1=bear_v1, 2=bear_v2, 3=bear_v3, 4=duck_v1, 5=duck_v2
 */
private fun extractCharacterId(url: String?): Int? {
    if (url == null) return null

    val isBear = url.contains("bear", ignoreCase = true)
    val isDuck = url.contains("duck", ignoreCase = true)

    // variant 추출 (v1, v2, v3)
    val variantMatch = Regex("/(v\\d)/").find(url)
    val variant = variantMatch?.groupValues?.get(1) ?: "v1"

    return when {
        isBear && variant == "v1" -> 1
        isBear && variant == "v2" -> 2
        isBear && variant == "v3" -> 3
        isDuck && variant == "v1" -> 4
        isDuck && variant == "v2" -> 5
        isBear -> 1  // fallback
        isDuck -> 4  // fallback
        else -> 1
    }
}

/**
 * 배경 이미지 URL에서 배경 ID 추출
 * 예: background_3.png -> 3
 */
private fun extractBackgroundId(url: String?): Int? {
    return url?.let {
        Regex("background_(\\d+)").find(it)?.groupValues?.get(1)?.toIntOrNull()
    }
}
