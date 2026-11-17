package cmov.grupo16.aegis.feature_scanner.domain.repository

import android.graphics.Bitmap
import cmov.grupo16.aegis.core.model.Villain
import cmov.grupo16.aegis.core.network.NetworkResult

interface VillainDetectionRepository {

    suspend fun detectVillain(bitmap: Bitmap, rotationDegrees: Int = 0): NetworkResult<Villain>
}
