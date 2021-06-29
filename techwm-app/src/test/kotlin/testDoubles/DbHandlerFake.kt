package testDoubles

import main.kotlin.Storage
import java.util.concurrent.CompletableFuture

class StorageFake : Storage<String> {
    private val dbDictionary: MutableMap<String, String> = mutableMapOf()

    override fun write(key: String, dataEntry: String): CompletableFuture<Unit> {
        return CompletableFuture.supplyAsync { dbDictionary[key] = dataEntry }
    }

    override fun read(key: String): CompletableFuture<String?> {
        return CompletableFuture.supplyAsync { dbDictionary[key] }
    }

    override fun create(key: String, dataEntry: String): CompletableFuture<Boolean> {
        throw NotImplementedError()
    }

    override fun update(key: String, dataEntry: String): CompletableFuture<Boolean> {
        throw NotImplementedError()
    }

    override fun delete(key: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync { dbDictionary[key] }.thenApply {
            if (it == null){
                false
            }
            else {
                dbDictionary[key] = ""
                true
            }
        }
    }

    fun clearDatabase() {
        dbDictionary.clear()
    }
}