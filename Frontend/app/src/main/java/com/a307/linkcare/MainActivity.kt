package com.a307.linkcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.a307.linkcare.common.theme.LinkCareTheme
import com.a307.linkcare.feature.auth.LinkCareLoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LinkCareTheme {
                var isLoggedIn by rememberSaveable { mutableStateOf(false) }

                if (!isLoggedIn) {
                    LinkCareLoginScreen(
                        onLoginSuccess = {
                            isLoggedIn = true
                        },
                        onSignupClick = {
                            // TODO: 회원가입 화면 연결
                        }
                    )
                } else {
                    LinkCareApp()
                }
            }
        }
    }
}
