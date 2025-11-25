package com.manuelbena.synkron.di

import android.util.Base64
import com.manuelbena.synkron.data.remote.n8n.N8nApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.Instant
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://n8n-n8n.l9a3of.easypanel.host/"
    // TODO: Mueve esto a local.properties en producción
    private const val API_KEY = "TU_API_KEY_AQUI"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val timestamp = Instant.now().toString()

            // Clonamos body para firmar (HMAC)
            val buffer = okio.Buffer()
            original.body?.writeTo(buffer)
            val bodyStr = buffer.readUtf8()

            val mac = Mac.getInstance("HmacSHA256").apply {
                init(SecretKeySpec(API_KEY.toByteArray(), "HmacSHA256"))
            }
            val signature = Base64.encodeToString(
                mac.doFinal("$timestamp\n$bodyStr".toByteArray()), Base64.NO_WRAP
            )

            val request = original.newBuilder()
                .header("x-api-key", API_KEY)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("x-timestamp", timestamp)
                .header("x-signature", signature)
                .build()

            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()
    }

    @Provides
    @Singleton
    fun provideN8nApi(client: OkHttpClient): N8nApi {
        // 1. Creamos la instancia de Moshi con soporte para Kotlin
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            // 2. Pasamos nuestra instancia configurada de Moshi aquí
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(N8nApi::class.java)
    }
}