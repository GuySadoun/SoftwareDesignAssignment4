package il.ac.technion.cs.softwaredesign.services

import com.google.common.collect.ImmutableList
import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.Inbox
import il.ac.technion.cs.softwaredesign.Message
import il.ac.technion.cs.softwaredesign.services.database.DbInboxHandler
import java.util.concurrent.CompletableFuture

class InboxManager @Inject constructor(private val dbInboxHandler: DbInboxHandler) {
    fun addMessageToConversation(from: String, to: String, message: String) : CompletableFuture<Unit> {
        return dbInboxHandler.addMessage(from, to, message)
    }

    fun isMsgWithIdExist(to: String, id: String) : CompletableFuture<Boolean> {
        return dbInboxHandler.getMessageById(to, id).thenApply { it == null }
    }

    fun deleteMsg(username: String, id: String) : CompletableFuture<Unit> {
        return dbInboxHandler.deleteMsg(username, id)
    }

    fun getUserInbox(username: String): CompletableFuture<Inbox> {
        return getMessagesOfUser(username).thenApply { messagesList ->
            messagesList.asReversed().groupBy { x -> x.fromUser}
        }
    }

    private fun getMessagesOfUser(username: String): CompletableFuture<List<Message>> {
        val messagesList = mutableListOf<Message>()
        var listCompletable = CompletableFuture.completedFuture(messagesList)

        return dbInboxHandler.getNextId(username)
            .thenCompose { numberOfMessages ->
                for (i in 0 until numberOfMessages) {
                    listCompletable = listCompletable
                        .thenCompose {
                            dbInboxHandler.getMessageById(username, i.toString()).thenApply { message ->
                                if (message != null){
                                    messagesList.add(message)
                                    messagesList
                                } else {
                                    messagesList
                                }
                            }
                        }
                }
                listCompletable
            }.thenApply { mutableList -> ImmutableList.copyOf(mutableList as MutableCollection<Message>) }
    }
}