package library.interfaces

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import java.util.concurrent.CompletableFuture

interface IDbReader {
    fun read(key: String): CompletableFuture<String?>
}