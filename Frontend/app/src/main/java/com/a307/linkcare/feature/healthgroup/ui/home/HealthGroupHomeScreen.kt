package com.a307.linkcare.feature.healthgroup.ui.home

import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.a307.linkcare.feature.commongroup.ui.home.MyGroupsViewModel

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
