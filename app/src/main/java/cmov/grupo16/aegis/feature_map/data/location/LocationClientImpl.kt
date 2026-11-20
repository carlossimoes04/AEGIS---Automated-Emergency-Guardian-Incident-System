package cmov.grupo16.aegis.feature_map.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import cmov.grupo16.aegis.feature_map.domain.location.LocationClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * LocationClientImpl (camada DATA)
 *
 * Lê a posição real do telemóvel com o FusedLocationProviderClient
 * (Google Play Services), que combina GPS, Wi-Fi e rede móvel para dar
 * a posição mais precisa gastando pouca bateria.
 *
 * O Google funciona com "callbacks" (chama uma função nossa quando há
 * posição nova). Como a app usa Flows, o `callbackFlow` converte
 * callbacks -> Flow.
 */
class LocationClientImpl(
    private val context: Context,
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
) : LocationClient {

    // O Android Studio avisaria que pode faltar a permissão; nós
    // verificamos isso à mão no PASSO 1, por isso desligamos o aviso.
    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(intervalMs: Long): Flow<Location> = callbackFlow {

        // PASSO 1: o utilizador deu permissão? Se não, o Flow termina com erro.
        if (!hasLocationPermission()) {
            throw LocationClient.LocationException("Permissão de localização não concedida")
        }

        // PASSO 2: a localização do telemóvel está ligada?
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkOn = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!gpsOn && !networkOn) {
            throw LocationClient.LocationException("A localização do dispositivo está desligada")
        }

        // PASSO 3: enviar já a ÚLTIMA posição conhecida, para o mapa não
        // ficar à espera enquanto o GPS arranca. trySend = "emite no Flow".
        fusedClient.lastLocation.addOnSuccessListener { last ->
            last?.let { trySend(it) }
        }

        // PASSO 4: pedido de atualizações contínuas.
        // HIGH_ACCURACY = usar GPS; no máximo uma posição a cada intervalMs/2.
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .build()

        // PASSO 5: função que o Google chama a cada posição nova -> mandamos para o Flow.
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

        // PASSO 6: quando ninguém estiver a ouvir (ecrã fechado),
        // paramos o GPS para poupar bateria.
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }

    /** true se houver permissão precisa (GPS) ou aproximada (rede). */
    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
}