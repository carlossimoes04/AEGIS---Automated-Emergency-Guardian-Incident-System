package cmov.grupo16.aegis.feature_map.domain.location

import android.location.Location
import kotlinx.coroutines.flow.Flow

/**
 * LocationClient (camada DOMAIN)
 *
 * Um "contrato" que diz O QUE uma fonte de localização tem de fazer,
 * sem dizer COMO. O MapViewModel só conhece esta interface; não sabe que
 * por trás está o GPS da Google. Assim podemos trocar a implementação
 * (ex.: localização falsa para testes/demonstração) sem mexer no ecrã.
 *
 * Quem implementa: feature_map/data/location/LocationClientImpl.kt
 */
interface LocationClient {

    /**
     * Devolve um Flow (fluxo de valores ao longo do tempo) com a posição.
     * Sempre que o GPS tem uma posição nova, o Flow emite-a.
     *
     * @param intervalMs de quanto em quanto tempo queremos uma posição (ms).
     */
    fun getLocationUpdates(intervalMs: Long): Flow<Location>

    /** Erro próprio da localização (sem permissão, GPS desligado...). */
    class LocationException(message: String) : Exception(message)
}