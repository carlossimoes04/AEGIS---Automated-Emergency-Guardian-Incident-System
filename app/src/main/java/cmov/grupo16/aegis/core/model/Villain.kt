package cmov.grupo16.aegis.core.model

data class Villain(
    val name: String, // The name of the villain
    val officialImageUrl: String?, // The URL of the villain's official image
    val threatLevel: Int, // The threat level of the villain
    val confidence: Float = 1.0f // The confidence level of the villain
)