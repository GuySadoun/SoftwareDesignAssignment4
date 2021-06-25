package il.ac.technion.cs.softwaredesign.Services

import il.ac.technion.cs.softwaredesign.PermissionLevel
import il.ac.technion.cs.softwaredesign.Services.Database.DbUserLoginHandler
import java.util.concurrent.CompletableFuture

class UserLoginManager (private val mDbUserInfoHandler: DbUserLoginHandler) {
    fun isUsernameLoggedIn(username: String): CompletableFuture<Boolean> {
        return mDbUserInfoHandler.isUserLoggedIn(username)
    }

    fun loginUser(username: String, permissionLevel: PermissionLevel): CompletableFuture<Unit> {
        return mDbUserInfoHandler.login(username, permissionLevel)
    }

    fun logoutUser(username: String, permissionLevel: PermissionLevel): CompletableFuture<Unit> {
        return mDbUserInfoHandler.logout(username, permissionLevel)
    }
}