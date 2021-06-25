package il.ac.technion.cs.softwaredesign.Services.Database

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.services.database.DbDirectoriesPaths
import main.kotlin.SerializerImpl
import main.kotlin.StorageFactoryImpl
import java.util.concurrent.CompletableFuture

class DbUserLoginHandler @Inject constructor(databaseFactory: StorageFactoryImpl) {

    private val dbUsernameToUserHandler by lazy { databaseFactory.open(DbDirectoriesPaths.UsernameToUser, SerializerImpl()) }

    fun getUsernameState(username: String): CompletableFuture<Boolean> {
        return dbUsernameToUserHandler.thenCompose { usernameToUserStorage ->
            usernameToUserStorage.read(username).thenApply { state ->
                state == "1"
            }
        }
    }

    fun setUserLoginState(username: String, state: Boolean): CompletableFuture<Unit> {
        return dbUsernameToUserHandler.thenCompose { usernameToUserStorage ->
            val stateString = if(state) "1" else "0"
            usernameToUserStorage.write(username, stateString)
        }
    }
}