package com.lizht.app.repository


import com.lizht.app.data.remote.PaymentApi
import com.lizht.app.data.remote.TransactionDto

class TransactionsRepository(
    private val api: PaymentApi
) {
    suspend fun myTransactions(): List<TransactionDto> =
        api.getMyTransactions()

    suspend fun myStoreTransactions(storeId: String): List<TransactionDto> =
        api.getMyStoreTransactions(storeId)
}
