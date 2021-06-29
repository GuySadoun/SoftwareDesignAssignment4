package testDoubles

import main.kotlin.Serializer
import main.kotlin.Storage
import java.util.concurrent.CompletableFuture

class StorageFake<DataEntry>(private val serializer: Serializer<DataEntry>) : Storage<DataEntry> {
    private val dbDictionary: MutableMap<String, ByteArray> = mutableMapOf()

    override fun write(key: String, dataEntry: DataEntry): CompletableFuture<Unit> {
        return CompletableFuture.supplyAsync { dbDictionary[key] = serializer.serialize(dataEntry) }
    }

    override fun read(key: String): CompletableFuture<DataEntry?> {
        return CompletableFuture.supplyAsync { dbDictionary[key] }.thenApply {
            if (it == null || it.isEmpty())
                null
            else
                serializer.deserialize(dbDictionary[key]!!)
        }
    }

    override fun create(key: String, dataEntry: DataEntry): CompletableFuture<Boolean> {
        throw NotImplementedError()
    }

    override fun update(key: String, dataEntry: DataEntry): CompletableFuture<Boolean> {
        throw NotImplementedError()
    }

    override fun delete(key: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync { dbDictionary[key] }.thenApply {
            if (it == null){
                false
            }
            else {
                dbDictionary[key] = ByteArray(0)
                true
            }
        }
    }

    fun clearDatabase() {
        dbDictionary.clear()
    }
}