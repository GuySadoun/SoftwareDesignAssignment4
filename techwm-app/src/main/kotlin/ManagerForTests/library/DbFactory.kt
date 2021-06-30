package library

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import library.interfaces.IDbHandler

class DbFactory @Inject constructor(private val secureStorageFactory: SecureStorageFactory){
    private val dataBasesCache = mutableMapOf<String, SecureStorageStringWrapper>()

    /**
     * Open - return new [IDbHandler] - like open of the primitive library with secureStorage
     * if you call open few times with the same name it'll return the same database
     *
     * @param databaseName db
     * @return IDbHandler that supply read/write actions
     */
    fun open(databaseName : String) : IDbHandler {
        if (dataBasesCache.containsKey(databaseName))
            return dataBasesCache[databaseName]!!

        val secureStorage = secureStorageFactory.open(databaseName.toByteArray())
        val wrappedSecureStorage = SecureStorageStringWrapper(secureStorage)
        dataBasesCache[databaseName] = wrappedSecureStorage
        return wrappedSecureStorage
    }
}


