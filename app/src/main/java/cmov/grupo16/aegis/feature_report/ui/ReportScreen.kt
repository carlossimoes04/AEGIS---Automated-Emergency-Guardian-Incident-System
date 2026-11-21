
package cmov.grupo16.aegis.feature_report.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ReportBackground = Color(0xFF111827)
private val ReportCyan = Color(0xFF00E5FF)
private val ReportRed = Color(0xFFEF4444)
private val ReportSurface = Color(0xFF1E293B)

@Composable
fun ReportScreen(
    latitude: Double?,
    longitude: Double?,
    onBack: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var incidentType by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReportBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "A.E.G.I.S. // INCIDENT REPORT",
            color = ReportCyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Text(
            text = "REPORT AN INCIDENT",
            color = Color.White,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Provide the details of the incident below.",
            color = Color.LightGray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "INCIDENT TYPE",
            color = ReportCyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )

        OutlinedTextField(
            value = incidentType,
            onValueChange = { incidentType = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Suspicious activity") },
            label = { Text("Type of incident") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = ReportCyan
            )
        )

        Text(
            text = "DESCRIPTION",
            color = ReportCyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            placeholder = {
                Text("Describe what happened...")
            },
            label = { Text("Incident description") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = ReportCyan
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "LOCATION",
            color = ReportCyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )

        if (latitude != null && longitude != null) {
            Text(
                text = "LAT: %.5f\nLNG: %.5f".format(
                    latitude,
                    longitude
                ),
                color = ReportCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        } else {
            Text(
                text = "GPS LOCATION UNAVAILABLE",
                color = ReportRed,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                onSubmit(incidentType.trim(), description.trim())
            },
            enabled = incidentType.isNotBlank() &&
                    description.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = CutCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ReportRed,
                disabledContainerColor = ReportSurface
            )
        ) {
            Text(
                text = "SUBMIT INCIDENT REPORT",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "CANCEL",
                color = ReportCyan,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
