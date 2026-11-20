package cmov.grupo16.aegis.core.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cmov.grupo16.aegis.feature_map.ui.MapScreen
import cmov.grupo16.aegis.feature_scanner.ui.CameraScannerScreen
import cmov.grupo16.aegis.feature_scanner.ui.DetectionResultScreen
import cmov.grupo16.aegis.feature_scanner.ui.ScannerUiState
import cmov.grupo16.aegis.feature_scanner.ui.ScannerViewModel

@Composable
fun AegisNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val scannerViewModel: ScannerViewModel = viewModel(factory = ScannerViewModel.Factory)
    NavHost(
        navController = navController,
        startDestination = ScreenRoutes.MAP,
        modifier = modifier.fillMaxSize()
    ) {
        // Map
        composable(ScreenRoutes.MAP) {
            MapScreen(
                onOpenScanner = {
                    scannerViewModel.resetScanner()
                    navController.navigate(ScreenRoutes.SCANNER)
                },
                onReportIncident = {
                    // TODO: Joana - navegar para ScreenRoutes.REPORT
                    Toast.makeText(context, "Manual Report", Toast.LENGTH_SHORT).show()
                }
            )
        }
        // Camera Scanner
        composable(ScreenRoutes.SCANNER) {
            val uiState by scannerViewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(uiState) {
                if (uiState is ScannerUiState.Success) {
                    navController.navigate(ScreenRoutes.RESULT) {
                        popUpTo(ScreenRoutes.SCANNER) { inclusive = true }
                    }
                }
            }
            CameraScannerScreen(
                uiState = uiState,
                onImageCaptured = { bitmap, rotation ->
                    scannerViewModel.onImageCaptured(bitmap, rotation)
                },
                onCameraError = { error ->
                    scannerViewModel.onCameraError(error)
                },
                onRetry = {
                    scannerViewModel.resetScanner()
                }
            )
        }
        // Result screen
        composable(ScreenRoutes.RESULT) {
            val uiState by scannerViewModel.uiState.collectAsStateWithLifecycle()
            if (uiState is ScannerUiState.Success) {
                val successState = uiState as ScannerUiState.Success
                DetectionResultScreen(
                    villain = successState.villain,
                    capturedBitmap = successState.capturedBitmap,
                    onReportIncident = { villain ->
                        // TODO: Joana - navegar para ScreenRoutes.REPORT com dados do vilão
                        Toast.makeText(
                            context,
                            "Report Ready: ${villain.name} (Level ${villain.threatLevel})",
                            Toast.LENGTH_SHORT
                        ).show()
                        scannerViewModel.resetScanner()
                        navController.popBackStack(ScreenRoutes.MAP, inclusive = false)
                    },
                    onRetryScan = {
                        scannerViewModel.resetScanner()
                        navController.navigate(ScreenRoutes.SCANNER) {
                            popUpTo(ScreenRoutes.RESULT) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}