package library

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import library.interfaces.IDbHandler
import java.util.concurrent.CompletableFuture

/**
 * provides read/write behavior with string instead of byte arrays
 *
 * @property secureStorage - secure storage to initialize the database
 * @constructor Create empty Secure storage string wrapper
 */
class SecureStorageStringWrapper(val secureStorage: CompletableFuture<SecureStorage>) : IDbHandler {
    /**
     * Read
     *
     * @param key
     * @return CompletableFuture with the result
     */
    override fun read(key: String): CompletableFuture<String?> {
        return secureStorage.thenCompose { it.read(key.toByteArray()) }
            .thenApply { x -> if (x == null) null else String(x) }
    }

    /**
     * Write - if the key already exists we override its value
     *
     * @param key
     * @param value
     * @return CompletableFuture of the action
     */
    override fun write(key: String, value: String): CompletableFuture<Unit> {
        return secureStorage.thenCompose {
                it.write(key.toByteArray(), value.toByteArray())
            }

    }

    override fun equals(other: Any?) =
        (other is SecureStorageStringWrapper) &&
                secureStorage == other.secureStorage

    override fun hashCode(): Int {
        return secureStorage.hashCode()
    }
}

