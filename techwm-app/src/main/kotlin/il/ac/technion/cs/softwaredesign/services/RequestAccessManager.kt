package il.ac.technion.cs.softwaredesign.services

import com.google.common.collect.ImmutableList
import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.services.database.DbRequestAccessHandler
import java.util.concurrent.CompletableFuture

class RequestAccessManager @Inject constructor(private val dbRequestAccessHandler: DbRequestAccessHandler) {

    fun addAccessRequest(username: String, reason: String, password: String): CompletableFuture<Unit>{
        return dbRequestAccessHandler.addRequest(username, reason, password)
    }

    fun isRequestForUsernameExists(username: String): CompletableFuture<Boolean>{
        return dbRequestAccessHandler.isRequestForUsernameExists(username)
    }

    fun getAllRequestsStrings(): CompletableFuture<List<Pair<String, String>>> {
        val usernameReasonList = mutableListOf<Pair<String, String>>()
        var listCompletable = CompletableFuture.completedFuture(usernameReasonList)

        return dbRequestAccessHandler.getNumberOfRequests()
            .thenCompose { numberOfRequests ->
                for (i in 0 until numberOfRequests) {
                    listCompletable = listCompletable
                        .thenCompose {
                            dbRequestAccessHandler.isActiveBySerialNumber(i).thenCompose { isActive ->
                                if (isActive){
                                    dbRequestAccessHandler.getRequestBySerialNumber(i)
                                        .thenApply { usernameReasonPair ->
                                            usernameReasonList.add(usernameReasonPair!!)
                                            usernameReasonList
                                        }
                                } else {
                                    CompletableFuture.completedFuture(usernameReasonList)
                                }
                            }
                        }
                }
                listCompletable
            }.thenApply { mutableList -> ImmutableList.copyOf(mutableList) }
    }

    fun removeRequest(username: String): CompletableFuture<Unit>{
        return dbRequestAccessHandler.removeRequest(username)
    }

    fun getPasswordByUsername(username: String): CompletableFuture<String>{
        return dbRequestAccessHandler.getPasswordByUsername(username)
    }
}