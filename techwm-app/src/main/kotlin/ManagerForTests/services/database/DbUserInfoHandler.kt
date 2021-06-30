package il.ac.technion.cs.softwaredesign.services.database

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.AccountType
import il.ac.technion.cs.softwaredesign.AccountType.*
import il.ac.technion.cs.softwaredesign.PermissionLevel
import il.ac.technion.cs.softwaredesign.PermissionLevel.*
import il.ac.technion.cs.softwaredesign.User
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbUserInfoHandler
import library.DbFactory
import java.util.concurrent.CompletableFuture

class DbUserInfoHandler @Inject constructor(databaseFactory: DbFactory) : IDbUserInfoHandler {
    companion object {
        const val usernameSuffix = "_username"
        const val accountSuffix = "_account"
        const val permissionSuffix = "_permission"
    }

    private val dbUsernameToUserHandler by lazy { databaseFactory.open(DbDirectoriesPaths.UsernameToUser) }
    private val dbIsUsernameRevokedHandler by lazy { databaseFactory.open(DbDirectoriesPaths.UsernameIsRevoked) }

    private val accountTypeArray = mapOf<Int?, AccountType>(
        DEFAULT.ordinal to DEFAULT,
        RESEARCH.ordinal to RESEARCH,
        ROOT.ordinal to ROOT
    )

    private val permissionsLevelArray = mapOf<Int?, PermissionLevel>(
                USER.ordinal to USER,
                OPERATOR.ordinal to OPERATOR,
                ADMINISTRATOR.ordinal to ADMINISTRATOR
            )

    override fun getUserByUsername(username: String): CompletableFuture<User?> {
        return dbUsernameToUserHandler.read(username + usernameSuffix)
            .thenCompose { usernameString ->
                dbUsernameToUserHandler.read(username + accountSuffix).thenApply { userAccountType ->
                    Pair(usernameString, accountTypeArray[userAccountType?.toInt()])
                }
            }
            .thenCompose { usernameAccountTypePair ->
                dbUsernameToUserHandler.read(username + permissionSuffix).thenApply { userPermissionLevel ->
                    Triple(
                        usernameAccountTypePair.first,
                        usernameAccountTypePair.second,
                        permissionsLevelArray[userPermissionLevel?.toInt()]
                    )
                }
            }
            .thenApply { userDetailsTriple ->
                val usernameString = userDetailsTriple.first
                val accountType = userDetailsTriple.second
                val permissionLevel = userDetailsTriple.third

                if (usernameString != null && accountType != null && permissionLevel != null)
                    User(usernameString, accountType, permissionLevel)
                else
                    null
            }
    }


    override fun setUsernameToUser(user: User): CompletableFuture<Unit> {
        val username = user.username
        val accountType = user.account.ordinal.toString()
        val permission = user.permissionLevel.ordinal.toString()
        return dbUsernameToUserHandler.write(username + usernameSuffix, username)
            .thenCompose { dbUsernameToUserHandler.write(username + accountSuffix, accountType) }
            .thenCompose { dbUsernameToUserHandler.write(username + permissionSuffix, permission) }
    }

    override fun getUserPermissionLevel(username: String): CompletableFuture<PermissionLevel?> {
        return dbUsernameToUserHandler.read(username + permissionSuffix).thenApply { user ->
            permissionsLevelArray[user?.toInt()]
        }
    }

    override fun isUserRevoked(username: String): CompletableFuture<Boolean> {
        return dbIsUsernameRevokedHandler.read(username).thenApply {
            it == "1"
        }
    }

    override fun revokeUser(username: String): CompletableFuture<Unit> {
        return dbIsUsernameRevokedHandler.write(username, "1")
    }

    override fun clearNameFromRevokedList(username: String): CompletableFuture<Unit> {
        return dbIsUsernameRevokedHandler.write(username, "0")
    }

}