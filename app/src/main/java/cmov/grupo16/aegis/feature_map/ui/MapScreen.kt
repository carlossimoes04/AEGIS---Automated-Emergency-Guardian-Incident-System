package cmov.grupo16.aegis.feature_map.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cmov.grupo16.aegis.feature_map.ui.components.MapboxMapContainer
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import java.util.Locale

/**
 * ============================================================================
 *  MapScreen  (Ecrã 1 - ecrã principal da app)
 * ============================================================================
 *
 *  O QUE MOSTRA:
 *  - Mapa em ecrã inteiro (Mapbox Dark) com a posição do utilizador
 *  - Cartão no topo com o estado do GPS e as coordenadas (estilo telemetria)
 *  - Avisos se faltar a permissão ou se o GPS der erro
 *  - Botões no canto inferior direito:
 *      [◎]  centrar o mapa em mim
 *      [🔔] participar ocorrência manual  (vermelho)  -> Membro 3
 *      [📷] abrir o scanner da câmara     (ciano)     -> Membro 1
 *
 *  COMO FUNCIONA (MVVM):
 *  O ecrã NÃO guarda dados. Lê tudo do MapViewModel (uiState) e, quando o
 *  utilizador faz algo, chama funções do ViewModel.
 */

// ---------------------------------------------------------------------------
// Paleta de cores do guia visual da AEGIS
// (definidas aqui por agora; se o grupo criar um tema comum em ui/theme,
//  basta trocar estas constantes pelas do tema)
// ---------------------------------------------------------------------------
private val SurfaceCard = Color(0xCC1E293B)   // cinzento-azulado, ~80% opacidade (cartões)
private val NeonBorder = Color(0xFF38BDF8)    // azul néon (contornos)
private val HoloCyan = Color(0xFF00E5FF)      // ciano holográfico (destaques, scanner)
private val DangerRed = Color(0xFFEF4444)     // vermelho (perigo, reporte)
private val WarningAmber = Color(0xFFF59E0B)  // âmbar (avisos, GPS à procura)

// Cantos "cortados" em vez de redondos -> aspeto tático/militar (guia visual)
private val TacticalShape = CutCornerShape(8.dp)

// Onde o mapa começa antes de o GPS dar a posição: FCUL, Lisboa.
// ATENÇÃO: o Mapbox usa a ordem (LONGITUDE, LATITUDE)!
private val DEFAULT_CENTER: Point = Point.fromLngLat(-9.1558, 38.7562)

/**
 * @param onOpenScanner    o que fazer ao carregar no botão da câmara
 *                         (a navegação está no AegisNavGraph)
 * @param onReportIncident o que fazer ao carregar no botão de reporte
 * @param viewModel        o ViewModel do mapa (criado automaticamente)
 */
@Composable
fun MapScreen(
    onOpenScanner: () -> Unit,
    onReportIncident: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory)
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // uiState: o estado atual (permissão, posição, erro).
    // collectAsStateWithLifecycle -> sempre que o ViewModel muda o estado,
    // este ecrã redesenha-se automaticamente.

    // =======================================================================
    // 1) PERMISSÕES DE LOCALIZAÇÃO
    // No Android 6+ a permissão tem de ser pedida em tempo de execução
    // (aparece a janela "Permitir que o AEGIS aceda à localização?").
    // O "launcher" abre essa janela e recebe a resposta do utilizador.
    // =======================================================================
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // results = mapa { permissão -> aceite? }
        // Basta uma das duas (precisa OU aproximada) para continuarmos.
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(granted)
    }

    // Abre a janela de pedido de permissão (precisa + aproximada)
    fun requestLocationPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // LaunchedEffect(Unit) corre UMA vez quando o ecrã aparece:
    // se a permissão já tinha sido dada antes, não voltamos a perguntar.
    LaunchedEffect(Unit) {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) viewModel.onPermissionResult(true) else requestLocationPermission()
    }

    // =======================================================================
    // 2) "CÂMARA" DO MAPA (para onde o mapa está a olhar)
    // Começa centrada na FCUL com zoom 12 (vê-se a cidade toda).
    // =======================================================================
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(DEFAULT_CENTER)
            zoom(12.0)
        }
    }

    // Na PRIMEIRA vez que o GPS dá uma posição, o mapa voa até ao utilizador
    // e passa a segui-lo. Depois disso, se o utilizador arrastar o mapa,
    // deixa de seguir (até carregar no botão "centrar em mim").
    var hasCenteredOnUser by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.userLocation != null) {
        if (uiState.userLocation != null && !hasCenteredOnUser) {
            hasCenteredOnUser = true
            mapViewportState.transitionToFollowPuckState()
        }
    }

    // =======================================================================
    // 3) DESENHO DO ECRÃ
    // Box = camadas umas por cima das outras:
    //   - fundo: o mapa
    //   - por cima: cartões do topo e botões do canto
    // =======================================================================
    Box(modifier = modifier.fillMaxSize()) {

        // Camada de fundo: o mapa (ver components/MapboxMapContainer.kt)
        MapboxMapContainer(
            mapViewportState = mapViewportState,
            showUserLocation = uiState.hasLocationPermission,
            modifier = Modifier.fillMaxSize()
        )

        // ---------- HUD superior: estado do GPS e coordenadas ----------
        // windowInsetsPadding(safeDrawing): evita que os cartões fiquem por
        // baixo da barra de estado / notch do telemóvel.
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            GpsStatusCard(uiState)

            // Aviso se o utilizador recusou a permissão
            if (uiState.permissionDenied) {
                Spacer(Modifier.size(8.dp))
                MessageCard(
                    text = "Sem permissão de localização. O AEGIS precisa do GPS para mostrar a tua posição e as zonas de perigo.",
                    actionLabel = "Permitir",
                    onAction = { requestLocationPermission() }
                )
            }

            // Aviso se houve erro no GPS (ex.: localização desligada)
            uiState.errorMessage?.let { msg ->
                Spacer(Modifier.size(8.dp))
                MessageCard(
                    text = msg,
                    actionLabel = "Tentar de novo",
                    onAction = { viewModel.retryLocation() }
                )
            }
        }

        // ---------- Botões de ação (canto inferior direito) ----------
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Botão pequeno "centrar em mim" (só faz sentido com permissão)
            if (uiState.hasLocationPermission) {
                SmallFloatingActionButton(
                    onClick = { mapViewportState.transitionToFollowPuckState() },
                    containerColor = SurfaceCard,
                    contentColor = HoloCyan
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Centrar na minha posição")
                }
            }

            // Botão "Participar ocorrência" (vermelho)
            FloatingActionButton(
                onClick = onReportIncident,
                shape = TacticalShape,
                containerColor = DangerRed,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.AddAlert, contentDescription = "Participar ocorrência")
            }

            // Botão "Scanner" (ciano)
            FloatingActionButton(
                onClick = onOpenScanner,
                shape = TacticalShape,
                containerColor = HoloCyan,
                contentColor = Color(0xFF090D16)
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Abrir scanner")
            }
        }
    }
}

/**
 * Cartão no topo do ecrã, estilo telemetria militar:
 *   [ícone] AEGIS // GPS_LOCK: ACTIVE
 *           LAT 38.75620  LNG -9.15580  ±12m
 * Fica âmbar ("SEARCHING") enquanto o GPS ainda não deu posição.
 */
@Composable
private fun GpsStatusCard(uiState: MapUiState) {
    val location = uiState.userLocation
    val locked = location != null

    Surface(
        color = SurfaceCard,
        shape = TacticalShape,
        border = BorderStroke(1.dp, NeonBorder.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (locked) Icons.Filled.GpsFixed else Icons.Filled.GpsNotFixed,
                contentDescription = null,
                tint = if (locked) HoloCyan else WarningAmber,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = if (locked) "AEGIS // GPS_LOCK: ACTIVE" else "AEGIS // GPS_LOCK: SEARCHING",
                    color = if (locked) HoloCyan else WarningAmber,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                if (location != null) {
                    Text(
                        text = String.format(
                            Locale.US, "LAT %.5f  LNG %.5f  ±%.0fm",
                            location.latitude, location.longitude, location.accuracyMeters
                        ),
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Cartão de aviso genérico (texto + botão de ação).
 * Usado para "sem permissão" e para erros do GPS.
 */
@Composable
private fun MessageCard(text: String, actionLabel: String, onAction: () -> Unit) {
    Surface(
        color = SurfaceCard,
        shape = TacticalShape,
        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = text, color = Color.White, fontSize = 13.sp)
            TextButton(
                onClick = onAction,
                colors = ButtonDefaults.textButtonColors(contentColor = HoloCyan),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(actionLabel.uppercase(), fontFamily = FontFamily.Monospace)
            }
        }
    }
}