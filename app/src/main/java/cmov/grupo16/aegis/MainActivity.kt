package cmov.grupo16.aegis

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cmov.grupo16.aegis.core.navigation.AegisNavGraph
import cmov.grupo16.aegis.feature_map.ui.MapScreen
import cmov.grupo16.aegis.ui.theme.AEGISTheme
import com.mapbox.common.MapboxOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AEGISTheme {
                AegisNavGraph()
            }
        }
    }
}