package com.a307.linkcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.a307.linkcare.common.theme.LinkCareTheme
import com.a307.linkcare.feature.healthgroup.HealthGroupHome
import com.a307.linkcare.navigation.AppNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LinkCareTheme {
                AppNavGraph()
            }
        }
    }
}
