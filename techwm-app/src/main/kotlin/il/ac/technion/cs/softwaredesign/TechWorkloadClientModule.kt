package il.ac.technion.cs.softwaredesign

import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import il.ac.technion.cs.softwaredesign.execution.ExecutionServiceModule
import il.ac.technion.cs.softwaredesign.services.InboxManager
import il.ac.technion.cs.softwaredesign.services.RequestAccessManager
import il.ac.technion.cs.softwaredesign.services.UserLoginManager
import il.ac.technion.cs.softwaredesign.services.database.DbInboxHandler
import il.ac.technion.cs.softwaredesign.services.database.DbRequestAccessHandler
import il.ac.technion.cs.softwaredesign.services.database.DbUserLoginHandler
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule
import main.kotlin.StorageFactory

class TechWorkloadClientModule: KotlinModule() {
    override fun configure() {
        bind<TechWorkloadClientFactory>().to<TechWorkloadClientFactoryImpl>().`in`<Singleton>()

        bind<DbInboxHandler>().`in`<Singleton>()
        bind<DbRequestAccessHandler>().`in`<Singleton>()
        bind<DbUserLoginHandler>().`in`<Singleton>()

        bind<InboxManager>().`in`<Singleton>()
        bind<RequestAccessManager>().`in`<Singleton>()
        bind<UserLoginManager>().`in`<Singleton>()

        bind<StorageFactory>().`in`<Singleton>()

        install(TechWorkloadManagerModule())

        install(SecureStorageModule())
        install(ExecutionServiceModule())
    }
}