package il.ac.technion.cs.softwaredesign.services.database

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.TokenWasDeletedAndCantBeReusedException
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbTokenHandler
import library.DbFactory
import library.SecureStorageStringWrapper
import library.interfaces.IDbHandler
import java.util.concurrent.CompletableFuture

class DbTokenHandler @Inject constructor(databaseFactory: DbFactory) : IDbTokenHandler {
    companion object {
        const val deletedTokenDbValue = ""
    }

    private val dbTokenToUsernameHandler by lazy { databaseFactory.open(DbDirectoriesPaths.TokenToUsername) }
    private val dbUsernameToTokenHandler by lazy { databaseFactory.open(DbDirectoriesPaths.UsernameToToken) }
    private val dbDeletedTokenHandler by lazy { databaseFactory.open(DbDirectoriesPaths.DeletedTokens) }

    override fun getUsernameByToken(token: String): CompletableFuture<String?> {
        return isDeleted(token).thenCompose { deleted ->
            if (deleted)
                CompletableFuture.completedFuture(null)
            else
                dbTokenToUsernameHandler.read(token)
        }
    }

    override fun setOrReplaceTokenToUsername(token: String, username: String) : CompletableFuture<Unit> {
        return isDeleted(token).thenCompose { deleted ->
            if (deleted)
                throw TokenWasDeletedAndCantBeReusedException()

            deleteUserPreviousTokenIfExist(username)
                .thenCompose { dbTokenToUsernameHandler.write(token, username) }
                .thenCompose { dbUsernameToTokenHandler.write(username, token) }
        }
    }

    override fun isDeleted(token: String): CompletableFuture<Boolean> {
        return dbDeletedTokenHandler.read(token).thenApply { deletedIfNotNull ->
            deletedIfNotNull != null
        }
    }

    override fun deleteUserPreviousTokenIfExist(username: String): CompletableFuture<Unit> {
        return getTokenByUsername(username).thenCompose { userPreviousToken ->
            if (userPreviousToken != null)
                deleteToken((userPreviousToken))
            else
                CompletableFuture.completedFuture(null)
        }
    }

    private fun deleteToken(token: String): CompletableFuture<Unit> {
        return dbDeletedTokenHandler.write(token, deletedTokenDbValue)
    }

    private fun getTokenByUsername(username: String): CompletableFuture<String?> {
        return dbUsernameToTokenHandler.read(username)
    }
}
