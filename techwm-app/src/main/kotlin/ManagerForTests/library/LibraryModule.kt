package library

import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import il.ac.technion.cs.softwaredesign.execution.ExecutionService
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule
import il.ac.technion.cs.softwaredesign.execution.ExecutionServiceModule

class LibraryModule : KotlinModule()  {
    override fun configure() {
        bind<DbFactory>().`in`<Singleton>()

        bind<SecureStorageFactory>().to<factory>().`in`<Singleton>()
        bind<ExecutionService>().to<ExecutionServiceFake>().`in`<Singleton>()
    }
}