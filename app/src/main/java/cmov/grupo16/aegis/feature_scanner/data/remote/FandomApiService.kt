package cmov.grupo16.aegis.feature_scanner.data.remote

import cmov.grupo16.aegis.feature_scanner.data.remote.dto.FandomResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface FandomApiService {

    @GET("api.php")
    suspend fun getVillainPageImage(
        @Query("titles") title: String,
        @Query("action") action: String = "query",
        @Query("prop") prop: String = "pageimages",
        @Query("pithumbsize") thumbSize: Int = 600,
        @Query("redirects") redirects: Int = 1,
        @Query("format") format: String = "json"
    ): FandomResponse

    companion object {
        private const val BASE_URL = "https://marvelcinematicuniverse.fandom.com/"

        fun create(): FandomApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FandomApiService::class.java)
        }
    }
}