package cmov.grupo16.aegis.feature_scanner.data.repository

import android.graphics.Bitmap
import cmov.grupo16.aegis.core.model.Villain
import cmov.grupo16.aegis.core.network.NetworkResult
import cmov.grupo16.aegis.core.network.NvidiaApiService
import cmov.grupo16.aegis.core.network.dto.NvidiaChatRequest
import cmov.grupo16.aegis.core.network.dto.NvidiaContentPart
import cmov.grupo16.aegis.core.network.dto.NvidiaImageUrl
import cmov.grupo16.aegis.core.network.dto.NvidiaMessage
import cmov.grupo16.aegis.core.network.dto.NvidiaVillainResult
import cmov.grupo16.aegis.core.network.dto.extractCleanJson
import cmov.grupo16.aegis.di.AppContainer
import cmov.grupo16.aegis.feature_scanner.data.remote.FandomApiService
import cmov.grupo16.aegis.feature_scanner.data.util.ImageUtils
import cmov.grupo16.aegis.feature_scanner.domain.repository.VillainDetectionRepository
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VillainDetectionRepositoryImpl(
    private val nvidiaApiService: NvidiaApiService = AppContainer.nvidiaApiService,
    private val fandomApiService: FandomApiService = AppContainer.fandomApiService,
    private val gson: Gson = AppContainer.gson
) : VillainDetectionRepository {

    override suspend fun detectVillain(
        bitmap: Bitmap,
        rotationDegrees: Int
    ): NetworkResult<Villain> = withContext(Dispatchers.IO) {
        try {
            // Resize, rotate and encode image to Base64 data URL
            val base64DataUrl = ImageUtils.bitmapToBase64DataUrl(
                bitmap = bitmap,
                rotationDegrees = rotationDegrees
            )

            // Build multimodal request for NVIDIA NIM
            val request = NvidiaChatRequest(
                messages = listOf(
                    NvidiaMessage(
                        role = "system",
                        content = SYSTEM_PROMPT
                    ),
                    NvidiaMessage(
                        role = "user",
                        content = listOf(
                            NvidiaContentPart(
                                type = "text",
                                text = "Identify the MCU villain or threat in this image."
                            ),
                            NvidiaContentPart(
                                type = "image_url",
                                imageUrl = NvidiaImageUrl(url = base64DataUrl)
                            )
                        )
                    )
                )
            )

            // Call NVIDIA NIM API and parse JSON output
            val response = nvidiaApiService.createChatCompletion(request)
            val rawContent = response.choices.firstOrNull()?.message?.content
                ?: return@withContext NetworkResult.Error("Empty response from NVIDIA AI.")

            val cleanJson = rawContent.extractCleanJson()
            val aiResult = gson.fromJson(cleanJson, NvidiaVillainResult::class.java)
                ?: return@withContext NetworkResult.Error("Could not parse villain data.")

            // Fetch official MCU Wiki thumbnail (falls back to null on failure)
            val officialImageUrl = fetchOfficialImageUrl(aiResult.villain)

            // Return domain model
            NetworkResult.Success(
                Villain(
                    name = aiResult.villain,
                    officialImageUrl = officialImageUrl,
                    threatLevel = aiResult.threatLevel.coerceIn(1, 5)
                )
            )
        } catch (e: CancellationException) {
            // Always rethrow coroutine cancellations so lifecycle cancellation works properly
            throw e
        } catch (e: Exception) {
            NetworkResult.Error(
                message = e.message ?: "Failed to analyze threat.",
                exception = e
            )
        }
    }

    /**
     * Queries the MCU Fandom MediaWiki API for the villain's official image.
     * Returns null instead of throwing if the page or image is not found.
     */
    private suspend fun fetchOfficialImageUrl(villainName: String): String? {
        return try {
            val normalizedTitle = villainName.trim().replace(" ", "_")
            if (normalizedTitle.isBlank()) return null

            val fandomResponse = fandomApiService.getVillainPageImage(title = normalizedTitle)
            fandomResponse.query?.pages?.values?.firstOrNull()?.thumbnail?.source
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val SYSTEM_PROMPT = """
            You are A.E.G.I.S., a S.H.I.E.L.D. tactical threat-recognition AI.
            Analyze the image and match the subject STRICTLY to a Marvel Cinematic Universe (MCU) VILLAIN or ANTAGONIST:
            - If an actual MCU villain is shown, identify that villain.
            - If a real person is shown, carefully observe their gender, hair, facial features, glasses, beard, expression, and clothing, and match them to the closest MCU villain lookalike across ALL MCU movies and series.
            - STRICTLY FORBIDDEN: Never pick superheroes or good guys. Only true villains or antagonists.
            - Always use their supervillain codename or alias rather than their civilian birth name when one exists.
            - NEVER return "Unknown Threat" or "Unknown" when a person is visible.
            - Return the exact English page title used on marvelcinematicuniverse.fandom.com.
            Respond strictly with a single valid JSON object and nothing else:
            {"villain": "Exact Villain Title", "threat_level": 4}
            threat_level must be an integer from 1 to 5.
        """
    }
}