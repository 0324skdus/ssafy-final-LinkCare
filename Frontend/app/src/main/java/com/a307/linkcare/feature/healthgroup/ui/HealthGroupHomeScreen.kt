package com.a307.linkcare.feature.healthgroup.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.a307.linkcare.feature.commongroup.MyGroupsViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HealthGroupHomeScreen(
    groupSeq: Long,
    vm: MyGroupsViewModel? = null
) {
    var reloadKey by remember { mutableStateOf(0) }

    key(reloadKey) {
        HealthGroupHome(
            groupSeq = groupSeq,
            modifier = Modifier,
            vm = vm,
            onReloadRequest = { reloadKey++ }
        )
    }
}
