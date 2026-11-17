package cmov.grupo16.aegis.feature_scanner.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cmov.grupo16.aegis.core.model.Villain
import cmov.grupo16.aegis.core.network.NetworkResult
import cmov.grupo16.aegis.di.AppContainer
import cmov.grupo16.aegis.feature_scanner.domain.repository.VillainDetectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScannerUiState {
    data object Scanning : ScannerUiState
    data object Analyzing : ScannerUiState
    data class Success(
        val villain: Villain,
        val capturedBitmap: Bitmap
    ) : ScannerUiState
    data class Error(val message: String) : ScannerUiState
}

class ScannerViewModel(
    private val repository: VillainDetectionRepository = AppContainer.villainDetectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Scanning)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onImageCaptured(bitmap: Bitmap, rotationDegrees: Int) {
        if (_uiState.value is ScannerUiState.Analyzing) return

        _uiState.value = ScannerUiState.Analyzing

        viewModelScope.launch {
            when (val result = repository.detectVillain(bitmap, rotationDegrees)) {
                is NetworkResult.Success -> {
                    _uiState.value = ScannerUiState.Success(
                        villain = result.data,
                        capturedBitmap = bitmap
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = ScannerUiState.Error(result.message)
                }
                is NetworkResult.Loading -> {
                    _uiState.value = ScannerUiState.Analyzing
                }
            }
        }
    }

    fun onCameraError(message: String) {
        _uiState.value = ScannerUiState.Error(message)
    }

    fun resetScanner() {
        _uiState.value = ScannerUiState.Scanning
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ScannerViewModel()
            }
        }
    }
}