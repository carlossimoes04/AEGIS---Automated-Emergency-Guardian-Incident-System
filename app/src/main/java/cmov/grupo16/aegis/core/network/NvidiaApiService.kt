package cmov.grupo16.aegis.core.network

import cmov.grupo16.aegis.BuildConfig
import cmov.grupo16.aegis.core.network.dto.NvidiaChatRequest
import cmov.grupo16.aegis.core.network.dto.NvidiaChatResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface NvidiaApiService {

    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Body request: NvidiaChatRequest
    ): NvidiaChatResponse

    companion object {
        private const val BASE_URL = "https://integrate.api.nvidia.com/"

        fun create(): NvidiaApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    // BASIC logs method, URL, HTTP status code,
                    // and timing without printing the large Base64 payload
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer ${BuildConfig.NVIDIA_API_KEY}")
                        .addHeader("Content-Type", "application/json")
                        .build()
                    chain.proceed(request)
                }
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NvidiaApiService::class.java)
        }
    }
}