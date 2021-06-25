package il.ac.technion.cs.softwaredesign.services.database

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.AccessRequest
import il.ac.technion.cs.softwaredesign.AccessRequestImpl
import il.ac.technion.cs.softwaredesign.NoUsernameExistException
import main.kotlin.SerializerImpl
import main.kotlin.StorageFactoryImpl
import java.util.concurrent.CompletableFuture


class DbRequestAccessHandler @Inject constructor(databaseFactory: StorageFactoryImpl) {
    companion object {
        const val serialNumberToUsernameSuffix = "_serialToUsername"
        const val serialNumberToIsActiveSuffix = "_serialToIsActive"

        const val usernameToReasonSuffix = "_usernameToReason"
        const val usernameToPasswordSuffix = "_usernameToPassword"
        const val usernameToSerialNumberSuffix = "_usernameToSerial"

        const val nonEmptyPrefix = "_"
        const val sizeKey = "size"
    }
    private val dbAccessRequests by lazy { databaseFactory.open(DbDirectoriesPaths.AccessRequests, SerializerImpl()) }

    fun addRequest(request: AccessRequest, password: String): CompletableFuture<Unit>{
        return isRequestForUsernameExists(request.requestingUsername).thenApply { alreadyExistsRequest ->
            if (alreadyExistsRequest) {
                throw IllegalArgumentException()
            }
        }.thenCompose {
            dbAccessRequests.thenCompose { storage ->
                storage.read(sizeKey).thenCompose { size ->
                    val serialNumber: Int = size?.toInt() ?: 0

                    storage.write(serialNumber.toString() + serialNumberToUsernameSuffix, nonEmptyPrefix + request.requestingUsername)
                        .thenCompose {
                            storage.write(sizeKey, (serialNumber + 1).toString())
                        }.thenCompose {
                            storage.write(request.requestingUsername + usernameToReasonSuffix, nonEmptyPrefix + request.reason)
                        }.thenCompose {
                            storage.write(request.requestingUsername + usernameToSerialNumberSuffix, serialNumber.toString())
                        }.thenCompose {
                            storage.write(serialNumber.toString() + serialNumberToIsActiveSuffix, "1")
                        }.thenCompose {
                            storage.write(request.requestingUsername + usernameToPasswordSuffix, password)
                        }
                }
            }
        }
    }

    private fun getRequestByUsername(username: String): CompletableFuture<AccessRequest?> {
        return dbAccessRequests.thenCompose { storage ->
            storage.read(username + usernameToReasonSuffix).thenApply { it?.drop(nonEmptyPrefix.length) }
        }.thenApply { reason ->
            if (reason == null)
                null
            else
                AccessRequestImpl(username, reason)
        }
    }

    fun getPasswordByUsername(username: String): CompletableFuture<String> {
        return dbAccessRequests.thenCompose { storage ->
            storage.read(username + usernameToPasswordSuffix)
        }.thenApply { password ->
            password ?: throw NoUsernameExistException()
        }
    }

    fun getRequestBySerialNumber(serialNumber: Int): CompletableFuture<AccessRequest?> {
        return dbAccessRequests.thenCompose { storage ->
            storage.read(serialNumber.toString() + serialNumberToUsernameSuffix)
                .thenCompose { username ->
                    storage.read(username + usernameToReasonSuffix).thenApply { reason ->
                        if (username == null || reason == null)
                            null
                        else
                            AccessRequestImpl(username.drop(nonEmptyPrefix.length), reason.drop(nonEmptyPrefix.length))
                    }
                }
        }
    }

    fun removeRequest(username: String): CompletableFuture<Unit>{
        return dbAccessRequests.thenCompose { storage ->
            storage.read(username + usernameToSerialNumberSuffix)
                .thenCompose { serialNumber ->
                    storage.write(serialNumber.toString() + serialNumberToIsActiveSuffix, "")
                }
        }
    }

    fun isRequestForUsernameExists(username: String): CompletableFuture<Boolean> {
        return dbAccessRequests.thenCompose { storage ->
            storage.read(username + usernameToSerialNumberSuffix)
                .thenCompose { serialNumber ->
                    if (serialNumber != null)
                        storage.read(serialNumber + serialNumberToIsActiveSuffix)
                            .thenApply { isActive ->
                                isActive == "1"
                            }
                    else
                        CompletableFuture.completedFuture(false)
                }
        }
    }

    fun getNumberOfRequests(): CompletableFuture<Int>{
        return dbAccessRequests.thenCompose { storage ->
            storage.read(sizeKey).thenApply { size -> size?.toInt() ?: 0 }
        }
    }

    fun isActiveBySerialNumber(serialNumber: Int): CompletableFuture<Boolean> {
        return dbAccessRequests.thenCompose { storage ->
            storage.read(serialNumber.toString() + serialNumberToIsActiveSuffix).thenApply { it == "1" }
        }
    }
}
