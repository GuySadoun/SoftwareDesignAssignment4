package il.ac.technion.cs.softwaredesign.services

import com.google.common.collect.ImmutableList
import il.ac.technion.cs.softwaredesign.PermissionLevel
import il.ac.technion.cs.softwaredesign.services.database.DbUserLoginHandler
import java.util.concurrent.CompletableFuture

class   UserLoginManager (private val mDbUserInfoHandler: DbUserLoginHandler) {
    fun isUsernameLoggedIn(username: String): CompletableFuture<Boolean> {
        return mDbUserInfoHandler.isUserLoggedIn(username)
    }

    fun loginUser(username: String, permissionLevel: PermissionLevel): CompletableFuture<Unit> {
        return mDbUserInfoHandler.login(username, permissionLevel)
    }

    fun logoutUser(username: String, permissionLevel: PermissionLevel): CompletableFuture<Unit> {
        return mDbUserInfoHandler.logout(username, permissionLevel)
    }

    fun getOnlineUsers(permissionLevel: PermissionLevel?): CompletableFuture<List<String>> {
        return if (permissionLevel == null)
            getAllOnlineUsers()
        else
            getOnlineUsersFiltered(permissionLevel)
    }

    private fun getAllOnlineUsers(): CompletableFuture<List<String>>{
        val onlineUsers = getOnlineUsersFiltered(PermissionLevel.USER)
        val onlineOperators = getOnlineUsersFiltered(PermissionLevel.OPERATOR)
        val onlineAdmins = getOnlineUsersFiltered(PermissionLevel.ADMINISTRATOR)

        return onlineUsers.thenCombine(onlineOperators) { users, operators -> users + operators}
            .thenCombine(onlineAdmins) { usersAndOperators, admins -> usersAndOperators + admins }
    }

    private fun getOnlineUsersFiltered(permissionLevel: PermissionLevel): CompletableFuture<List<String>>{
        val onlineUsersList = mutableListOf<String>()
        var listCompletable = CompletableFuture.completedFuture(onlineUsersList)

        return mDbUserInfoHandler.getNextSerialNumber().thenCompose { numberOfRequests ->
            for (i in 0 until numberOfRequests) {
                listCompletable = listCompletable.thenCompose {
                    mDbUserInfoHandler.getUsernameBySerialNumIfOnline(i, permissionLevel).thenApply { username ->
                        if (username != null) {
                            onlineUsersList.add(username)
                            onlineUsersList
                        } else {
                            onlineUsersList
                        }
                    }
                }
            }
            listCompletable
        }.thenApply { mutableList -> ImmutableList.copyOf(mutableList) }
    }
}