package com.a307.linkcare.feature.caregroup.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CareGroupHomeScreen(
    groupSeq: Long
) {
    var reloadKey by remember { mutableStateOf(0) }

    key(reloadKey) {
        CareGroupHome(
            groupSeq = groupSeq,
            onReloadRequest = { reloadKey++ }
        )


    }
}