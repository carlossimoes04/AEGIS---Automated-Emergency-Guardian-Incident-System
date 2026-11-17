package cmov.grupo16.aegis

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cmov.grupo16.aegis.feature_scanner.ui.CameraScannerScreen
import cmov.grupo16.aegis.feature_scanner.ui.DetectionResultScreen
import cmov.grupo16.aegis.feature_scanner.ui.ScannerUiState
import cmov.grupo16.aegis.feature_scanner.ui.ScannerViewModel
import cmov.grupo16.aegis.ui.theme.AEGISTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AEGISTheme {
                val viewModel: ScannerViewModel = viewModel(factory = ScannerViewModel.Factory)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                when (val state = uiState) {
                    is ScannerUiState.Success -> {
                        DetectionResultScreen(
                            villain = state.villain,
                            capturedBitmap = state.capturedBitmap,
                            onReportIncident = { villain ->
                                Toast.makeText(
                                    this,
                                    "Report ready: ${villain.name} (Threat ${villain.threatLevel})",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onRetryScan = { viewModel.resetScanner() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        CameraScannerScreen(
                            uiState = state,
                            onImageCaptured = { bitmap, rotation ->
                                viewModel.onImageCaptured(bitmap, rotation)
                            },
                            onCameraError = { error ->
                                viewModel.onCameraError(error)
                            },
                            onRetry = { viewModel.resetScanner() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}