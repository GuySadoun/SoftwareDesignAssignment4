package il.ac.technion.cs.softwaredesign.services.database

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.services.interfaces.db.IDbPasswordReader
import library.DbFactory
import library.SecureStorageStringWrapper
import library.interfaces.IDbReader
import java.util.concurrent.CompletableFuture

class DbPasswordReader @Inject constructor (databaseFactory: DbFactory) : IDbPasswordReader {
    private val dbPasswordsReader : IDbReader by lazy { databaseFactory.open(DbDirectoriesPaths.UsernameToPassword) }

    override fun getPassword(username: String): CompletableFuture<String?> {
        return dbPasswordsReader.read(username)
    }
}