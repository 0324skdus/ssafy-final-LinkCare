package com.a307.linkcare

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.remember
import com.a307.linkcare.common.theme.LinkCareTheme
import com.a307.linkcare.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            LinkCareTheme {
                // 딥링크에서 토큰 추출
                val deepLinkToken = remember {
                    intent?.data?.pathSegments?.lastOrNull()
                }

                AppNavGraph(initialDeepLinkToken = deepLinkToken)
            }
        }
    }
}
