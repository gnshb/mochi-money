package com.mochimoney.app.data.splitwise

import com.mochimoney.app.domain.model.TransactionDirection
import java.time.LocalDate

data class SplitwiseUser(
    val id: Long,
    val name: String,
)

data class SplitwiseGroup(
    val id: Long,
    val name: String,
)

data class SplitwiseExpenseShare(
    val expenseId: Long,
    val groupId: Long,
    val groupName: String,
    val description: String,
    val date: LocalDate,
    val currency: String,
    val direction: TransactionDirection,
    val amountPaise: Long,
)
