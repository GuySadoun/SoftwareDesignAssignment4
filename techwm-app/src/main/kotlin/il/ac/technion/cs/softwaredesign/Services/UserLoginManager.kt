package il.ac.technion.cs.softwaredesign.Services

import il.ac.technion.cs.softwaredesign.Services.Database.DbUserLoginHandler
import java.util.concurrent.CompletableFuture

class UserLoginManager (private val mDbUserInfoHandler: DbUserLoginHandler) {
    fun isUsernameLoggedIn(username: String): CompletableFuture<Boolean> {
        return mDbUserInfoHandler.getUsernameState(username)
    }

    fun setUserLoginState(username: String, state: Boolean): CompletableFuture<Unit> {
        return mDbUserInfoHandler.setUserLoginState(username, state)
    }
}