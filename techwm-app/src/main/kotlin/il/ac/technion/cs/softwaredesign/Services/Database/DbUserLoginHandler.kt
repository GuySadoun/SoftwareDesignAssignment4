package il.ac.technion.cs.softwaredesign.Services.Database

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.PermissionLevel
import il.ac.technion.cs.softwaredesign.services.database.DbDirectoriesPaths
import il.ac.technion.cs.softwaredesign.services.database.DbRequestAccessHandler
import main.kotlin.SerializerImpl
import main.kotlin.StorageFactoryImpl
import java.lang.IllegalArgumentException
import java.util.concurrent.CompletableFuture

class DbUserLoginHandler @Inject constructor(databaseFactory: StorageFactoryImpl) {
    companion object {
        const val SerialToLoggedInUsernameSuffix = "_serialToLoggedInUsername"
        const val UsernameToIsLoggedIn = "_usernameToIsLoggedIn"
        const val UsernameToSerialNumber = "_usernameToSerialNumber"

        const val nonEmptyPrefix = "_"
        const val loggedInSymbol = "1"
        const val sizeKey = "size"
    }

    private val dbUsernameToLoginStateHandler by lazy { databaseFactory.open(DbDirectoriesPaths.UsersDbPath, SerializerImpl()) }

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
                    storage.read(SerialToLoggedInUsernameSuffix + DbRequestAccessHandler.sizeKey).thenCompose { size ->
                        val serialNumber: Int = size?.toInt() ?: 0
                        storage.write(
                            serialNumber.toString() + SerialToLoggedInUsernameSuffix + permissionLevel.toString(),
                            loggedInSymbol
                        )
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
}