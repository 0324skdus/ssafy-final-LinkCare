package com.a307.linkcare.feature.mypage.ui.state

sealed class StoreEvent {
    data class PurchaseSuccess(val itemName: String) : StoreEvent()
    data class PurchaseFailed(val reason: String) : StoreEvent()
    object NotEnoughCoins : StoreEvent()
}