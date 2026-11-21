package cmov.grupo16.aegis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cmov.grupo16.aegis.core.navigation.AegisNavGraph
import cmov.grupo16.aegis.ui.theme.AEGISTheme

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