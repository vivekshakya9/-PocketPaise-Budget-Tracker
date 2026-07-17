package com.vsdigitallabs.pocketpaise.stdbt.data.network

import com.vsdigitallabs.pocketpaise.stdbt.data.model.Budget
import com.vsdigitallabs.pocketpaise.stdbt.data.model.SavingsGoal
import com.vsdigitallabs.pocketpaise.stdbt.data.model.Transaction
import retrofit2.Response
import retrofit2.http.*

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

data class AuthResponse(
    val token: String,
    val user_id: Int,
    val name: String? = null
)

interface ApiService {
    @POST("auth.php?action=login")
    suspend fun login(@Body body: Map<String, String>): Response<ApiResponse<AuthResponse>>
    
    @POST("auth.php?action=google_auth")
    suspend fun googleAuth(@Body body: Map<String, String>): Response<ApiResponse<AuthResponse>>

    @POST("auth.php?action=register")
    suspend fun register(@Body body: Map<String, String>): Response<ApiResponse<AuthResponse>>

    // Expenses
    @GET("expenses.php")
    suspend fun getExpenses(): Response<ApiResponse<List<Transaction>>>

    @POST("expenses.php")
    suspend fun createExpense(@Body expense: Map<String, Any>): Response<ApiResponse<Any>>

    @DELETE("expenses.php")
    suspend fun deleteExpense(@Query("id") id: Long): Response<ApiResponse<Any>>
    
    // Incomes
    @GET("income.php")
    suspend fun getIncomes(): Response<ApiResponse<List<Transaction>>>

    @POST("income.php")
    suspend fun createIncome(@Body income: Map<String, Any>): Response<ApiResponse<Any>>

    @DELETE("income.php")
    suspend fun deleteIncome(@Query("id") id: Long): Response<ApiResponse<Any>>
    
    // Budgets
    @GET("budgets.php")
    suspend fun getBudgets(@Query("month_year") monthYear: String? = null): Response<ApiResponse<List<Budget>>>

    @POST("budgets.php")
    suspend fun createBudget(@Body budget: Map<String, Any>): Response<ApiResponse<Any>>

    @DELETE("budgets.php")
    suspend fun deleteBudget(@Query("id") id: Long): Response<ApiResponse<Any>>

    // Goals
    @GET("goals.php")
    suspend fun getGoals(): Response<ApiResponse<List<SavingsGoal>>>

    @POST("goals.php")
    suspend fun createGoal(@Body goal: Map<String, Any>): Response<ApiResponse<Any>>
    
    @PUT("goals.php")
    suspend fun updateGoal(@Body goal: Map<String, Any>): Response<ApiResponse<Any>>

    @DELETE("goals.php")
    suspend fun deleteGoal(@Query("id") id: Long): Response<ApiResponse<Any>>

    // App Config (AdMob etc)
    @GET("config.php")
    suspend fun getConfig(): Response<ApiResponse<Map<String, Any>>>
}
