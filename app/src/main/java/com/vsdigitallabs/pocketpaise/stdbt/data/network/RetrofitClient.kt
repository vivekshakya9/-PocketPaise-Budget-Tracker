package com.vsdigitallabs.pocketpaise.stdbt.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.squareup.moshi.JsonReader

class LenientBooleanAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Boolean {
        return when (reader.peek()) {
            JsonReader.Token.BOOLEAN -> reader.nextBoolean()
            JsonReader.Token.NUMBER -> reader.nextInt() != 0
            JsonReader.Token.STRING -> {
                val str = reader.nextString()
                str == "1" || str.equals("true", ignoreCase = true)
            }
            else -> {
                reader.skipValue()
                false
            }
        }
    }

    @ToJson
    fun toJson(writer: com.squareup.moshi.JsonWriter, value: Boolean?) {
        writer.value(value)
    }
}

object RetrofitClient {
    // API URL based on the user's host
    private const val BASE_URL = "https://pocketpaise.universitytak.in/backend/api/"

    private val moshi = Moshi.Builder()
        .add(LenientBooleanAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    var authToken: String? = null

    private val authInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request()
        val builder = request.newBuilder()
        authToken?.let { token ->
            builder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(builder.build())
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
