package com.a307.linkcare.feature.mypage.ui.store

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.a307.linkcare.feature.mypage.ui.state.StoreEvent

@Composable
fun StoreRoute(
    viewModel: StoreViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val state = viewModel.uiState

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is StoreEvent.PurchaseSuccess ->
                    Toast.makeText(context, "구매 완료!", Toast.LENGTH_SHORT).show()

                is StoreEvent.NotEnoughCoins ->
                    Toast.makeText(context, "코인이 부족합니다.", Toast.LENGTH_SHORT).show()

                is StoreEvent.PurchaseFailed ->
                    Toast.makeText(context, "구매에 실패했어요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 순수 UI
    StoreScreen(
        state = state,
        onBack = onBack,
        onBuy = { item, tab ->
            viewModel.buy(item, tab)
        }
    )
}