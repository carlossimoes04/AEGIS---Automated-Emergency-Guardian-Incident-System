package cmov.grupo16.aegis.core.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cmov.grupo16.aegis.feature_map.ui.MapScreen
import cmov.grupo16.aegis.feature_map.ui.MapViewModel
import cmov.grupo16.aegis.feature_report.ui.ReportScreen
import cmov.grupo16.aegis.feature_scanner.ui.CameraScannerScreen
import cmov.grupo16.aegis.feature_scanner.ui.DetectionResultScreen
import cmov.grupo16.aegis.feature_scanner.ui.ScannerUiState
import cmov.grupo16.aegis.feature_scanner.ui.ScannerViewModel

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AegisNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val scannerViewModel: ScannerViewModel = viewModel(factory = ScannerViewModel.Factory)
    val mapViewModel: MapViewModel = viewModel(factory = MapViewModel.Factory)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        ScreenRoutes.MAP,
        ScreenRoutes.SCANNER,
        ScreenRoutes.REPORT
    )

    val navItems = listOf(
        BottomNavItem(ScreenRoutes.MAP, "MAP", Icons.Default.Map),
        BottomNavItem(ScreenRoutes.SCANNER, "SCANNER", Icons.Default.CameraAlt),
        BottomNavItem(ScreenRoutes.REPORT, "REPORT", Icons.Default.AddAlert)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF090D16),
        bottomBar = {
            if (showBottomBar) {
                AegisBottomBar(
                    items = navItems,
                    currentRoute = currentRoute,
                    onItemClick = { item ->
                        if (item.route == ScreenRoutes.SCANNER) {
                            scannerViewModel.resetScanner()
                        }
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ScreenRoutes.MAP,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Map
            composable(ScreenRoutes.MAP) {
                MapScreen(
                    viewModel = mapViewModel,
                    onOpenScanner = {
                        scannerViewModel.resetScanner()
                        navController.navigate(ScreenRoutes.SCANNER) {
                            launchSingleTop = true
                        }
                    },
                    onReportIncident = { _, _ ->
                        navController.navigate(ScreenRoutes.REPORT) {
                            launchSingleTop = true
                        }
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
                            // TODO: Joana - pré-preencher o ReportScreen com os dados do vilão
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

            // Incident Report
            composable(ScreenRoutes.REPORT) {
                val mapState by mapViewModel.uiState.collectAsStateWithLifecycle()

                ReportScreen(
                    latitude = mapState.userLocation?.latitude,
                    longitude = mapState.userLocation?.longitude,
                    onBack = {
                        navController.popBackStack()
                    },
                    onSubmit = { incidentType, description ->
                        Toast.makeText(
                            context,
                            "Report submitted: $incidentType",
                            Toast.LENGTH_SHORT
                        ).show()
                        navController.popBackStack(ScreenRoutes.MAP, inclusive = false)
                    }
                )
            }
        }
    }
}

@Composable
private fun AegisBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit
) {
    val hudSurface = Color(0xFF0F172A)
    val hudCyan = Color(0xFF00E5FF)
    val hudBorder = Color(0xFF38BDF8)

    Column {
        HorizontalDivider(
            thickness = 1.dp,
            color = hudBorder.copy(alpha = 0.4f)
        )
        NavigationBar(
            containerColor = hudSurface,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onItemClick(item) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = hudCyan,
                        selectedTextColor = hudCyan,
                        indicatorColor = hudCyan.copy(alpha = 0.15f),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
            }
        }
    }
}