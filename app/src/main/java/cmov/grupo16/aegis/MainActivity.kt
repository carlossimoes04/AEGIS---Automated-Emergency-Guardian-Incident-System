package cmov.grupo16.aegis

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cmov.grupo16.aegis.feature_map.ui.MapScreen
import cmov.grupo16.aegis.ui.theme.AEGISTheme
import com.mapbox.common.MapboxOptions

/**
 * VERSÃO DE TESTE: abre diretamente no ecrã do mapa.
 * (O código do scanner volta no passo 9, através da navegação.)
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Token público do Mapbox (local.properties -> BuildConfig).
        // Sem esta linha o mapa não carrega!
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN

        enableEdgeToEdge()
        setContent {
            AEGISTheme {
                // O MapScreen cria sozinho o seu MapViewModel
                MapScreen(
                    onOpenScanner = {
                        Toast.makeText(this, "Scanner (ligado no passo 9)", Toast.LENGTH_SHORT).show()
                    },
                    onReportIncident = {
                        Toast.makeText(this, "Reporte (Membro 3)", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}