package cmov.grupo16.aegis.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Request payload sent to the NVIDIA NIM `/v1/chat/completions` endpoint.
 */
data class NvidiaChatRequest(
    val model: String = "meta/muse-glimmer-30b", // nvidia's model
    val messages: List<NvidiaMessage>, // list of messages
    @SerializedName("max_tokens") // max tokens to generate
    val maxTokens: Int = 256, // 256 is the default value
    /**
     * Lower values (e.g., 0.1–0.2) produce more deterministic, less creative responses.
     */
    val temperature: Double = 0.2
)

/**
 * Represents a single message in the conversation history.
 * [content] can be a plain [String] (for system/text prompts) or
 * a [List] of [NvidiaContentPart] (for multimodal inputs).
 */
data class NvidiaMessage(
    val role: String, // "system" or "user"
    val content: Any  // String or List<NvidiaContentPart>
)

/**
 * Individual content block inside a multimodal message.
 */
data class NvidiaContentPart(
    val type: String, // "text" or "image_url"
    val text: String? = null, // "text" if type is "text"
    @SerializedName("image_url") // "image_url" if type is "image_url"
    val imageUrl: NvidiaImageUrl? = null // "data:image/jpeg;base64,..."
)

/**
 * Image payload wrapper containing either a public URL or a Base64-encoded
 * data URI (`data:image/jpeg;base64,...`).
 */
data class NvidiaImageUrl(
    val url: String // "data:image/jpeg;base64,..."
)

/**
 * Root response returned by the NVIDIA NIM chat completions endpoint.
 */
data class NvidiaChatResponse(
    val choices: List<NvidiaChoice> = emptyList()
)

/**
 * Single completion candidate returned by the model.
 */
data class NvidiaChoice(
    val message: NvidiaResponseMessage? = null
)

/**
 * Assistant message containing the generated text content.
 */
data class NvidiaResponseMessage(
    val role: String? = null,
    val content: String? = null
)

/**
 * Structured villain identification parsed from the model's JSON string output.
 */
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