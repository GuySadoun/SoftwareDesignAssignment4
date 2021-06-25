package il.ac.technion.cs.softwaredesign.Services.Database

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.TechWorkloadManager
import il.ac.technion.cs.softwaredesign.services.database.DbDirectoriesPaths
import main.kotlin.PairSerializerImpl
import main.kotlin.StorageFactoryImpl
import java.util.concurrent.CompletableFuture

class DbInboxHandler @Inject constructor(databaseFactory: StorageFactoryImpl, techWM: TechWorkloadManager){
    companion object {
        const val separatorKey = "^"
        const val nonEmptyPrefix = "_"
        const val sizeKey = "size"
    }
    private val dbAccessRequests by lazy { databaseFactory.open(DbDirectoriesPaths.Inbox, PairSerializerImpl()) }
    fun addMessage(from: String, to: String, message: String) :CompletableFuture<Unit> {
        return dbAccessRequests.thenCompose { storage ->
            storage.read(to + separatorKey + sizeKey).thenCompose { size ->
                val serialNumber: Int = size?.first?.toInt() ?: 0
                storage.write(to + separatorKey + serialNumber.toString(), Pair(from, message)).thenCompose {
                    storage.write(to + separatorKey + sizeKey, Pair((serialNumber+1).toString(), ""))
                }
            }
        }
    }


}