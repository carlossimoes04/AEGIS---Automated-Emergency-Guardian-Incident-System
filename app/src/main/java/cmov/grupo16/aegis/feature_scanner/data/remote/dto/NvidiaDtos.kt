package cmov.grupo16.aegis.feature_scanner.data.remote.dto

import com.google.gson.annotations.SerializedName

// NVidia's chat request
data class NvidiaChatRequest(
    val model: String = "meta/muse-glimmer-30b",
    val messages: List<NvidiaMessage>,
    @SerializedName("max_tokens")
    val maxTokens: Int = 256,
    val temperature: Double = 0.2
)

data class NvidiaMessage(
    val role: String, // "system" or "user"
    val content: Any  // String or List<NvidiaContentPart>
)

data class NvidiaContentPart(
    val type: String, // "text" or "image_url"
    val text: String? = null,
    @SerializedName("image_url")
    val imageUrl: NvidiaImageUrl? = null
)

data class NvidiaImageUrl(
    val url: String // "data:image/jpeg;base64,..."
)

// API's response

data class NvidiaChatResponse(
    val choices: List<NvidiaChoice> = emptyList()
)
data class NvidiaChoice(
    val message: NvidiaResponseMessage? = null
)
data class NvidiaResponseMessage(
    val role: String? = null,
    val content: String? = null
)

// Returned by the chat endpoint

data class NvidiaVillainResult(
    val villain: String = "Unknown Threat",
    @SerializedName("threat_level")
    val threatLevel: Int = 3
)

/**
 * Remove markdown blocks (```json ... ```) if needed
 */
fun String.extractCleanJson(): String {
    return this
        .trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
}