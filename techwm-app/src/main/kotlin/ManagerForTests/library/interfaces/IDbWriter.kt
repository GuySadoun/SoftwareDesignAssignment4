package library.interfaces

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import java.util.concurrent.CompletableFuture

interface IDbWriter {
    fun write(key: String, value: String) : CompletableFuture<Unit>
}
