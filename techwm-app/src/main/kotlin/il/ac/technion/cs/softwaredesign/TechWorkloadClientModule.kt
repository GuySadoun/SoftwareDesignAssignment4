package il.ac.technion.cs.softwaredesign

import dev.misfitlabs.kotlinguice4.KotlinModule
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule

class TechWorkloadClientModule: KotlinModule() {
    override fun configure() {
        install(TechWorkloadManagerModule())

        install(SecureStorageModule())
            //install(ExecutionServiceModule())

    }
}