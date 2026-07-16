package com.vsdigitallabs.pocketpaise.stdbt.data.network

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

    @POST("auth.php?action=register")
    suspend fun register(@Body body: Map<String, String>): Response<ApiResponse<AuthResponse>>

    // Expenses
    @GET("expenses.php?action=read")
    suspend fun getExpenses(@Header("Authorization") token: String): Response<ApiResponse<List<Transaction>>>

    @POST("expenses.php?action=create")
    suspend fun createExpense(@Header("Authorization") token: String, @Body expense: Map<String, Any>): Response<ApiResponse<Any>>

    @POST("expenses.php?action=delete")
    suspend fun deleteExpense(@Header("Authorization") token: String, @Body body: Map<String, Int>): Response<ApiResponse<Any>>
    
    // Incomes
    @GET("income.php?action=read")
    suspend fun getIncomes(@Header("Authorization") token: String): Response<ApiResponse<List<Transaction>>>

    @POST("income.php?action=create")
    suspend fun createIncome(@Header("Authorization") token: String, @Body income: Map<String, Any>): Response<ApiResponse<Any>>

    @POST("income.php?action=delete")
    suspend fun deleteIncome(@Header("Authorization") token: String, @Body body: Map<String, Int>): Response<ApiResponse<Any>>
    
    // App Config (AdMob etc)
    @GET("config.php")
    suspend fun getConfig(): Response<ApiResponse<Map<String, Any>>>
}
