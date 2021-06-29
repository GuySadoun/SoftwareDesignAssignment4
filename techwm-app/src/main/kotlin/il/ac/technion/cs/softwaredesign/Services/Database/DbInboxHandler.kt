package il.ac.technion.cs.softwaredesign.Services.Database

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.Message
import il.ac.technion.cs.softwaredesign.TechWorkloadManager
import il.ac.technion.cs.softwaredesign.services.database.DbDirectoriesPaths
import il.ac.technion.cs.softwaredesign.services.database.DbRequestAccessHandler
import main.kotlin.PairSerializerImpl
import main.kotlin.StorageFactoryImpl
import java.util.concurrent.CompletableFuture

class DbInboxHandler @Inject constructor(databaseFactory: StorageFactoryImpl){
    companion object {
        const val separatorKey = "^"
        const val sizeKey = "size"
    }
    private val dbInbox by lazy { databaseFactory.open(DbDirectoriesPaths.Inbox, PairSerializerImpl()) }

    fun addMessage(from: String, to: String, message: String) : CompletableFuture<Unit> {
        return dbInbox.thenCompose { storage ->
            storage.read(to + separatorKey + sizeKey).thenCompose { size ->
                val serialNumber: Int = size?.first?.toInt() ?: 0
                storage.write(to + separatorKey + serialNumber.toString(), Pair(from, message)).thenCompose {
                    storage.write(to + separatorKey + sizeKey, Pair((serialNumber+1).toString(), ""))
                }
            }
        }
    }

    fun getMessageById(to: String, id: String): CompletableFuture<Message?> {
        return dbInbox.thenCompose { storage ->
            storage.read(to + separatorKey + id).thenApply {
                if (it != null)
                    Message(id, it.first, it.second)
                else
                    null
            }
        }
    }

    fun deleteMsg(username: String, id: String) : CompletableFuture<Unit>  {
        return dbInbox.thenCompose { storage ->
            storage.delete(username + separatorKey + id).thenApply {}
        }
    }

    fun getNumberOfMessages(username: String): CompletableFuture<Int> {
        return dbInbox.thenCompose { storage ->
            storage.read(username + separatorKey + sizeKey).thenApply { size -> size?.first?.toInt() ?: 0 }
        }
    }
}