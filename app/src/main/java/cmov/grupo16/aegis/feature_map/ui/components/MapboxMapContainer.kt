package cmov.grupo16.aegis.feature_map.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location

/**
 * ============================================================================
 *  MapboxMapContainer  (componente de UI)
 * ============================================================================
 *
 *  O QUE FAZ:
 *  Desenha o mapa do Mapbox dentro do Jetpack Compose.
 *  - Estilo escuro "Mapbox Dark" (pedido no guia visual: ruas escuras, água preta)
 *  - Mostra o "puck" (o ponto azul a pulsar) na posição do utilizador
 *
 *  PORQUÊ UM COMPONENTE SEPARADO DO MapScreen?
 *  O MapScreen trata dos botões, avisos e permissões. Este ficheiro trata só
 *  do mapa. No passo 3 vamos acrescentar AQUI DENTRO os marcadores dos vilões
 *  e os círculos das zonas de perigo, sem mexer no resto do ecrã.
 *
 *  @param mapViewportState controla a "câmara" do mapa (onde está centrado,
 *                          zoom, seguir o utilizador). É criado no MapScreen
 *                          para que os botões (ex.: "centrar em mim") a possam
 *                          controlar.
 *  @param showUserLocation true -> mostra o ponto azul do utilizador
 *                          (só deve ser true se houver permissão de localização)
 */
@Composable
fun MapboxMapContainer(
    mapViewportState: MapViewportState,
    showUserLocation: Boolean,
    modifier: Modifier = Modifier
) {
    MapboxMap(
        modifier = modifier,
        mapViewportState = mapViewportState,
        // Estilo do mapa. Alternativas: Style.STANDARD, Style.SATELLITE_STREETS...
        style = { MapStyle(style = Style.DARK) }
    ) {
        // -------------------------------------------------------------------
        // MapEffect dá-nos acesso ao MapView "clássico" do Mapbox, para
        // configurar plugins que ainda não têm versão Compose (como o puck).
        // O bloco volta a correr sempre que `showUserLocation` muda
        // (ex.: o utilizador acabou de dar permissão).
        // -------------------------------------------------------------------
        MapEffect(showUserLocation) { mapView ->
            mapView.location.updateSettings {
                // Liga/desliga o ponto do utilizador
                enabled = showUserLocation

                if (showUserLocation) {
                    // Ícone padrão do Mapbox, com seta a indicar a direção
                    locationPuck = createDefault2DPuck(withBearing = true)
                    // A seta aponta para onde o utilizador se está a MOVER
                    // (COURSE). Alternativa: HEADING = para onde o telemóvel aponta.
                    puckBearing = PuckBearing.COURSE
                    puckBearingEnabled = true
                    // Efeito de "pulso" à volta do ponto (estilo radar)
                    pulsingEnabled = true
                }
            }
        }

        // TODO passo 3: marcadores de incidentes e círculos das zonas de perigo
    }
}