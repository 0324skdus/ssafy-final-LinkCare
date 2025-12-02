package com.ssafy.sdk.health.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ssafy.sdk.health.presentation.ui.theme.PhoneAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: HealthSyncViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 앱 시작 시 권한 체크
        viewModel.checkPermissions()

        setContent {
            PhoneAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // userId는 실제 로그인된 사용자 ID로 변경해야 함
                    val userId = 2 // TODO: 실제 userId 전달
                    
                    HealthSyncScreen(
                        userId = userId,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
