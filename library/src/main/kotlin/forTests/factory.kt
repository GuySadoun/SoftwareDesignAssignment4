package library

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.util.concurrent.CompletableFuture

class factory: SecureStorageFactory {
    val dict = mutableMapOf<String, SecureStorage>()

    override fun open(name: ByteArray): CompletableFuture<SecureStorage> {
        return CompletableFuture.supplyAsync {
            if (dict[String(name)] == null) {
                dict[String(name)] = secureStorageForTests()
                Thread.sleep((100 * dict.size).toLong())
            }
            dict[String(name)]!!
        }
    }
}