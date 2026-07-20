package com.vsdigitallabs.pocketpaise.stdbt.data.model

data class NetworkTransaction(
    val id: Long? = null,
    val title: String? = null,
    val amount: Double? = null,
    val type: String? = null,
    val category: String? = null,
    val timestamp: Long? = null,
    val notes: String? = null,
    val receiptUri: String? = null,
    val uuid: String? = null
) {
    fun toTransaction(localUserId: Long) = Transaction(
        id = 0L,
        userId = localUserId,
        title = title ?: "",
        amount = amount ?: 0.0,
        type = type ?: "EXPENSE",
        category = category ?: "General",
        timestamp = timestamp ?: 0L,
        notes = notes ?: "",
        receiptUri = receiptUri,
        serverId = id ?: 0L,
        uuid = uuid ?: java.util.UUID.randomUUID().toString(),
        isSynced = true
    )
}

data class NetworkBudget(
    val id: Long? = null,
    val category: String? = null,
    val limitAmount: Double? = null,
    val monthYear: String? = null,
    val uuid: String? = null
) {
    fun toBudget(localUserId: Long) = Budget(
        id = 0L,
        userId = localUserId,
        category = category ?: "General",
        limitAmount = limitAmount ?: 0.0,
        monthYear = monthYear,
        serverId = id ?: 0L,
        uuid = uuid ?: java.util.UUID.randomUUID().toString(),
        isSynced = true
    )
}

data class NetworkSavingsGoal(
    val id: Long? = null,
    val name: String? = null,
    val targetAmount: Double? = null,
    val currentAmount: Double? = null,
    val targetDate: Long? = null,
    val isCompleted: Boolean? = null,
    val uuid: String? = null
) {
    fun toSavingsGoal(localUserId: Long) = SavingsGoal(
        id = 0L,
        userId = localUserId,
        name = name ?: "",
        targetAmount = targetAmount ?: 0.0,
        currentAmount = currentAmount ?: 0.0,
        targetDate = targetDate ?: 0L,
        isCompleted = isCompleted ?: false,
        serverId = id ?: 0L,
        uuid = uuid ?: java.util.UUID.randomUUID().toString(),
        isSynced = true
    )
}

data class SyncData(
    val transactions: List<NetworkTransaction> = emptyList(),
    val budgets: List<NetworkBudget> = emptyList(),
    val goals: List<NetworkSavingsGoal> = emptyList(),
    val deleted_transactions: List<String> = emptyList(),
    val deleted_budgets: List<String> = emptyList(),
    val deleted_goals: List<String> = emptyList()
)
