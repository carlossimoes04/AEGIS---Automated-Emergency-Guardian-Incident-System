package cmov.grupo16.aegis.di

import cmov.grupo16.aegis.core.network.NvidiaApiService
import cmov.grupo16.aegis.feature_scanner.data.remote.FandomApiService
import cmov.grupo16.aegis.feature_scanner.data.repository.VillainDetectionRepositoryImpl
import cmov.grupo16.aegis.feature_scanner.domain.repository.VillainDetectionRepository
import com.google.gson.Gson

/**
 * Manual Dependency Injection container holding app-wide singletons.
 */
object AppContainer {

    val gson: Gson by lazy { Gson() }

    val nvidiaApiService: NvidiaApiService by lazy {
        NvidiaApiService.create()
    }

    val fandomApiService: FandomApiService by lazy {
        FandomApiService.create()
    }

    val villainDetectionRepository: VillainDetectionRepository by lazy {
        VillainDetectionRepositoryImpl(
            nvidiaApiService = nvidiaApiService,
            fandomApiService = fandomApiService,
            gson = gson
        )
    }
}