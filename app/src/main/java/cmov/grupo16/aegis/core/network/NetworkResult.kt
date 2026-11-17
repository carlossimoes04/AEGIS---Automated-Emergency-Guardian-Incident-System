package cmov.grupo16.aegis.core.network

/**
 * This class is used to handle the network requests.
 */
sealed class NetworkResult<out T> {
    data object Loading : NetworkResult<Nothing>()
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : NetworkResult<Nothing>()
}