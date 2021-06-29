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
        const val UsernameToIsLoggedIn = "_usernameToIsLoggedIn"
        const val UsernameToSerialNumber = "_usernameToSerialNumber"

        const val nonEmptyPrefix = "_"
        const val loggedInSymbol = "1"
        const val sizeKey = "size"
    }

    private val dbUsernameToLoginStateHandler by lazy { databaseFactory.open(DbDirectoriesPaths.UsersDbPath, StringSerializerImpl()) }

    fun isUserLoggedIn(username: String): CompletableFuture<Boolean> {
        return dbUsernameToLoginStateHandler.thenCompose { usernameToUserStorage ->
            usernameToUserStorage.read(username + UsernameToIsLoggedIn).thenApply { state ->
                state == loggedInSymbol
            }
        }
    }

    fun login(username: String, permissionLevel: PermissionLevel): CompletableFuture<Unit> {
        return dbUsernameToLoginStateHandler.thenCompose { storage ->
            storage.write(username + UsernameToIsLoggedIn, loggedInSymbol)
                .thenCompose {
                    storage.read(username + UsernameToSerialNumber).thenCompose { serial ->
                        if (serial == null) {
                            storage.read(SerialToLoggedInUsernameSuffix + sizeKey)
                                .thenCompose { size ->
                                    val serialNumber: Int = size?.toInt() ?: 0
                                    storage.write(
                                        serialNumber.toString() + SerialToLoggedInUsernameSuffix + permissionLevel.toString(),
                                        nonEmptyPrefix + username
                                    ).thenCompose {
                                        storage.write(SerialToLoggedInUsernameSuffix + sizeKey,
                                            (serialNumber + 1).toString())
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
            storage.delete(username + UsernameToIsLoggedIn).thenApply { throw IllegalArgumentException() }
                .thenCompose {
                    storage.read(username + UsernameToSerialNumber)
                        .thenCompose { serialNumber ->
                            storage.delete(serialNumber + SerialToLoggedInUsernameSuffix + permissionLevel.toString())
                                .thenApply { throw IllegalArgumentException() }
                        }
                }

        }
    }

    fun getNumberOfOnlineUsers(): CompletableFuture<Int> {
        return dbUsernameToLoginStateHandler.thenCompose { storage ->
            storage.read(SerialToLoggedInUsernameSuffix + sizeKey).thenApply { size -> size?.toInt() ?: 0 }
        }
    }

    fun getUsernameBySerialNumIfOnline(serial: Int, permissionLevel: PermissionLevel) : CompletableFuture<String?> {
        return dbUsernameToLoginStateHandler.thenCompose { storage ->
            storage.read(serial.toString() + SerialToLoggedInUsernameSuffix + permissionLevel.toString()).thenApply { username ->
                    username?.drop(1)
            }
        }
    }
}