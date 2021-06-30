package il.ac.technion.cs.softwaredesign

import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import il.ac.technion.cs.softwaredesign.execution.ExecutionService
import il.ac.technion.cs.softwaredesign.execution.ExecutionServiceModule
import il.ac.technion.cs.softwaredesign.services.InboxManager
import il.ac.technion.cs.softwaredesign.services.RequestAccessManager
import il.ac.technion.cs.softwaredesign.services.UserLoginManager
import il.ac.technion.cs.softwaredesign.services.database.DbInboxHandler
import il.ac.technion.cs.softwaredesign.services.database.DbRequestAccessHandler
import il.ac.technion.cs.softwaredesign.services.database.DbUserLoginHandler
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule
import library.ExecutionServiceFake
import library.factory
import main.kotlin.StorageFactory
import main.kotlin.StorageFactoryImpl

class TechWorkloadClientModule: KotlinModule() {
    override fun configure() {
        bind<TechWorkloadClientFactory>().to<TechWorkloadClientFactoryImpl>().`in`<Singleton>()
        bind<StorageFactory>().to<StorageFactoryImpl>().`in`<Singleton>()

        bind<DbInboxHandler>().`in`<Singleton>()
        bind<DbRequestAccessHandler>().`in`<Singleton>()
        bind<DbUserLoginHandler>().`in`<Singleton>()

        bind<InboxManager>().`in`<Singleton>()
        bind<RequestAccessManager>().`in`<Singleton>()
        bind<UserLoginManager>().`in`<Singleton>()

        //install(TechWorkloadManagerModule())


        // remove before submission
        bind<SecureStorageFactory>().to<factory>().`in`<Singleton>()
        install(TechWorkloadManagerModuleTests())
        bind<ExecutionService>().to<ExecutionServiceFake>().`in`<Singleton>()

        // uncomment before submission
        //install(SecureStorageModule())
        //install(ExecutionServiceModule())
    }
}