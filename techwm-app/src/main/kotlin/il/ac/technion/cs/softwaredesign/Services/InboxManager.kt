package il.ac.technion.cs.softwaredesign.Services

import il.ac.technion.cs.softwaredesign.Services.Database.DbInboxHandler
import java.util.concurrent.CompletableFuture

class InboxManager(private val dbInboxHandler: DbInboxHandler) {
    fun addMessageToConversation(from: String, to: String, message: String) : CompletableFuture<Unit> {
        return dbInboxHandler.addMessage(from, to, message)
    }
}