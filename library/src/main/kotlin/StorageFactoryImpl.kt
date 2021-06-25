package main.kotlin

import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.util.concurrent.CompletableFuture
import javax.inject.Inject

class StorageFactoryImpl @Inject constructor(private val secureStorageFactory: SecureStorageFactory) : StorageFactory {
    override fun <DataEntry> open(name: String, serializer: Serializer<DataEntry>): CompletableFuture<Storage<DataEntry>> =
        secureStorageFactory.open(name.toByteArray()).thenApply { StorageImpl(serializer, it) }
}










// first open: requests -> 6 paths

// sec open: users -> 9 paths