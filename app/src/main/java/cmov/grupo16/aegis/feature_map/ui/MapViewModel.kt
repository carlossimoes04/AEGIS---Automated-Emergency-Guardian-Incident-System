package cmov.grupo16.aegis.feature_map.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cmov.grupo16.aegis.feature_map.data.location.LocationClientImpl
import cmov.grupo16.aegis.feature_map.domain.location.LocationClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MapViewModel (camada UI - lógica do ecrã do mapa)
 *
 * Guarda o ESTADO do ecrã (permissão, posição, erros). O ecrã (MapScreen)
 * só desenha o que está no estado; quando o utilizador faz algo, o ecrã
 * avisa o ViewModel, que atualiza o estado, e o ecrã redesenha-se sozinho.
 *
 * O ViewModel sobrevive a rotações do ecrã: se rodares o telemóvel, não
 * se perde a posição nem se volta a pedir a permissão.
 */

/**
 * Posição do utilizador. Classe nossa (em vez de android.location.Location)
 * para o ecrã não depender de classes do Android nem do Mapbox.
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float   // margem de erro do GPS em metros (ex.: ±12 m)
)

/**
 * Tudo o que o ecrã precisa de saber para se desenhar.
 * É imutável: para mudar, cria-se uma cópia com .copy(...)
 */
data class MapUiState(
    val hasLocationPermission: Boolean = false, // o utilizador deu permissão?
    val permissionDenied: Boolean = false,      // recusou? -> mostrar aviso
    val userLocation: UserLocation? = null,     // null = GPS ainda à procura
    val errorMessage: String? = null            // null = sem erros
)

class MapViewModel(
    private val locationClient: LocationClient
) : ViewModel() {

    // _uiState é privado e mutável (só o ViewModel altera).
    // uiState é público e só de leitura (o ecrã observa).
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    // A "tarefa" que está a ouvir o GPS (guardamos para a poder cancelar)
    private var locationJob: Job? = null

    /** Chamado pelo ecrã depois de o utilizador responder ao pedido de permissão. */
    fun onPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(hasLocationPermission = granted, permissionDenied = !granted)
        }
        if (granted) startLocationUpdates()
    }

    /** Começa a ouvir o GPS e atualiza o estado a cada posição nova. */
    private fun startLocationUpdates() {
        // Não começar duas vezes
        if (locationJob?.isActive == true) return

        // viewModelScope: cancelado automaticamente quando o ViewModel é
        // destruído (sair da app) -> o GPS para sozinho.
        locationJob = viewModelScope.launch {
            locationClient.getLocationUpdates(intervalMs = LOCATION_INTERVAL_MS)
                // Erro (sem permissão, GPS desligado...) -> guardar a mensagem
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "Erro no GPS") }
                }
                // Cada posição nova -> atualizar o estado
                .collect { location ->
                    _uiState.update {
                        it.copy(
                            userLocation = UserLocation(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracyMeters = location.accuracy
                            ),
                            errorMessage = null
                        )
                    }
                }
        }
    }

    /** Botão "Tentar de novo" (ex.: depois de ligar a localização). */
    fun retryLocation() {
        locationJob?.cancel()
        locationJob = null
        _uiState.update { it.copy(errorMessage = null) }
        if (_uiState.value.hasLocationPermission) startLocationUpdates()
    }

    /** Esconde a mensagem de erro. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        // De quanto em quanto tempo pedimos uma posição nova
        private const val LOCATION_INTERVAL_MS = 5_000L // 5 segundos

        /**
         * Factory: ensina o Android a CRIAR este ViewModel, porque precisa
         * de receber o LocationClient (que precisa do Context da app).
         * Mesmo padrão do ScannerViewModel do Membro 1.
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                MapViewModel(locationClient = LocationClientImpl(app.applicationContext))
            }
        }
    }
}