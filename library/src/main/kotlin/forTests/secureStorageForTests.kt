package library

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import java.util.concurrent.CompletableFuture

class secureStorageForTests : SecureStorage {
    private val dict = mutableMapOf<String, ByteArray>()

    override fun read(key: ByteArray): CompletableFuture<ByteArray?> {
        return CompletableFuture.supplyAsync{
            val res = dict[String(key)]
            if (res != null)
                Thread.sleep(res.size.toLong())
            res
        }
    }

    override fun write(key: ByteArray, value: ByteArray): CompletableFuture<Unit> {
        return CompletableFuture.supplyAsync{
            dict[String(key)] = value
        }
    }
}