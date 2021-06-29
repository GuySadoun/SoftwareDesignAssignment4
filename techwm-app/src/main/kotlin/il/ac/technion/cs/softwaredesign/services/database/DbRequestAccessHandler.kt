package il.ac.technion.cs.softwaredesign.services.database

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.NoUsernameExistException
import il.ac.technion.cs.softwaredesign.services.database.DbDirectoriesPaths
import main.kotlin.StorageFactory
import main.kotlin.StringSerializerImpl
import java.util.concurrent.CompletableFuture


class DbRequestAccessHandler @Inject constructor(databaseFactory: StorageFactory) {
    companion object {
        const val serialNumberToUsernameSuffix = "_serialToUsername"
        const val serialNumberToIsActiveSuffix = "_serialToIsActive"

        const val usernameToReasonSuffix = "_usernameToReason"
        const val usernameToPasswordSuffix = "_usernameToPassword"
        const val usernameToSerialNumberSuffix = "_usernameToSerial"

        const val nonEmptyPrefix = "_"
        const val sizeKey = "size"
    }
    private val dbAccessRequests by lazy { databaseFactory.open(DbDirectoriesPaths.AccessRequests, StringSerializerImpl()) }

    fun addRequest(username: String, reason: String, password: String): CompletableFuture<Unit>{
        return isRequestForUsernameExists(username).thenApply { alreadyExistsRequest ->
            if (alreadyExistsRequest) {
                throw IllegalArgumentException()
            }
        }.thenCompose {
            dbAccessRequests.thenCompose { storage ->
                storage.read(sizeKey).thenCompose { size ->
                    val serialNumber: Int = size?.toInt() ?: 0

                    storage.write(serialNumber.toString() + serialNumberToUsernameSuffix, nonEmptyPrefix + username)
                        .thenCompose { // increase size
                            storage.write(sizeKey, (serialNumber + 1).toString())
                        }.thenCompose { // insert username -> reason
                            storage.write(username + usernameToReasonSuffix, nonEmptyPrefix + reason)
                        }.thenCompose { // insert username -> serial
                            storage.write(username + usernameToSerialNumberSuffix, serialNumber.toString())
                        }.thenCompose { // update serial -> isActive
                            storage.write(serialNumber.toString() + serialNumberToIsActiveSuffix, "1")
                        }.thenCompose { // insert username -> password
                            storage.write(username + usernameToPasswordSuffix, password)
                        }
                }
            }
        }
    }

    fun getRequestBySerialNumber(serialNumber: Int): CompletableFuture<Pair<String, String>?> {
        return dbAccessRequests.thenCompose { storage ->
            storage.read(serialNumber.toString() + serialNumberToUsernameSuffix)
            .thenCompose { username ->
                storage.read(username?.drop(nonEmptyPrefix.length) + usernameToReasonSuffix).thenApply { reason ->
                    if (username == null || reason == null)
                        null
                    else
                        Pair(username.drop(nonEmptyPrefix.length), reason.drop(nonEmptyPrefix.length))
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

    fun getPasswordByUsername(username: String): CompletableFuture<String> {
        return dbAccessRequests.thenCompose { storage ->
            storage.read(username + usernameToPasswordSuffix)
        }.thenApply { password ->
            password ?: throw NoUsernameExistException()
        }
    }

    fun getNumberOfRequests(): CompletableFuture<Int>{
        return dbAccessRequests.thenCompose { storage ->
            storage.read(sizeKey).thenApply { size -> size?.toInt() ?: 0 }
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

    fun isActiveBySerialNumber(serialNumber: Int): CompletableFuture<Boolean> {
        return dbAccessRequests.thenCompose { storage ->
            storage.read(serialNumber.toString() + serialNumberToIsActiveSuffix).thenApply { it == "1" }
        }
    }
}
