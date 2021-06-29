package il.ac.technion.cs.softwaredesign.services.database

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.PermissionLevel
import main.kotlin.StorageFactory
import main.kotlin.StringSerializerImpl
import java.lang.IllegalArgumentException
import java.util.concurrent.CompletableFuture

class DbUserLoginHandler @Inject constructor(databaseFactory: StorageFactory) {
    companion object {
        const val SerialToLoggedInUsernameSuffix = "_serialToLoggedInUsername"
        const val UsernameToIsLoggedInSuffix = "_usernameToIsLoggedIn"
        const val UsernameToSerialNumberSuffix = "_usernameToSerialNumber"

        const val nonEmptyPrefix = "_"
        const val loggedInSymbol = "1"
        const val dbSizeKey = "size"
    }

    private val dbUsernameToLoginStateHandler by lazy {
        databaseFactory.open(
            DbDirectoriesPaths.UsersDbPath,
            StringSerializerImpl()
        )
    }

    fun login(username: String, permissionLevel: PermissionLevel): CompletableFuture<Unit> {
        return dbUsernameToLoginStateHandler.thenCompose { storage ->
            storage.write(username + UsernameToIsLoggedInSuffix, loggedInSymbol)
                .thenCompose {
                    storage.read(username + UsernameToSerialNumberSuffix).thenCompose { serial ->
                        if (serial == null) {
                            storage.read(SerialToLoggedInUsernameSuffix + dbSizeKey)
                                .thenCompose { size ->
                                    val serialNumber: Int = size?.toInt() ?: 0
                                    storage.write(
                                        serialNumber.toString() + SerialToLoggedInUsernameSuffix + permissionLevel.toString(),
                                        nonEmptyPrefix + username
                                    ).thenCompose {
                                        storage.write(
                                            SerialToLoggedInUsernameSuffix + dbSizeKey,
                                            (serialNumber + 1).toString()
                                        )
                                    }.thenCompose {
                                        storage.write(
                                            username + UsernameToSerialNumberSuffix,
                                            serialNumber.toString()
                                        )
                                    }
                                }
                        } else {
                            storage.write(
                                serial.toString() + SerialToLoggedInUsernameSuffix + permissionLevel.toString(),
                                username
                            )
                        }
                    }
                }
        }
    }

    fun logout(username: String, permissionLevel: PermissionLevel): CompletableFuture<Unit> {
        return dbUsernameToLoginStateHandler.thenCompose { storage ->
            storage.delete(username + UsernameToIsLoggedInSuffix)
                .thenApply { if (!it) throw IllegalArgumentException() }
                .thenCompose {
                    storage.read(username + UsernameToSerialNumberSuffix)
                        .thenCompose { serialNumber ->
                            storage.delete(serialNumber + SerialToLoggedInUsernameSuffix + permissionLevel.toString())
                                .thenApply { if (!it) throw IllegalArgumentException() }
                        }
                }
        }
    }

    fun isUserLoggedIn(username: String): CompletableFuture<Boolean> {
        return dbUsernameToLoginStateHandler.thenCompose { usernameToUserStorage ->
            usernameToUserStorage.read(username + UsernameToIsLoggedInSuffix).thenApply { state ->
                state == loggedInSymbol
            }
        }
    }

    fun getNextSerialNumber(): CompletableFuture<Int> {
        return dbUsernameToLoginStateHandler.thenCompose { storage ->
            storage.read(SerialToLoggedInUsernameSuffix + dbSizeKey).thenApply { size -> size?.toInt() ?: 0 }
        }
    }

    fun getUsernameBySerialNumIfOnline(serial: Int, permissionLevel: PermissionLevel): CompletableFuture<String?> {
        return dbUsernameToLoginStateHandler.thenCompose { storage ->
            storage.read(serial.toString() + SerialToLoggedInUsernameSuffix + permissionLevel.toString())
                .thenApply { username ->
                    username?.drop(nonEmptyPrefix.length)
                }
        }
    }
}