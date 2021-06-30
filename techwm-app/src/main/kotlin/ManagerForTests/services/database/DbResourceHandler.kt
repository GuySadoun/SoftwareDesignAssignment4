package il.ac.technion.cs.softwaredesign.services.database

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.IdAlreadyExistException
import library.DbFactory
import java.util.concurrent.CompletableFuture

class DbResourceHandler @Inject constructor(databaseFactory: DbFactory) {
    companion object {
        const val serialNumberSeparator = "-"
        const val AvailableSymbol = "1"
        const val UnavailableSymbol = "0"
    }

    private val dbSerialNumberToIdHandler by lazy { databaseFactory.open(DbDirectoriesPaths.SerialNumberToId) }
    private val dbIdToResourceInfoHandler by lazy { databaseFactory.open(DbDirectoriesPaths.IdToResourceName) }

    fun getResourceById(id: String) : CompletableFuture<ResourceInfo?> {
        return dbIdToResourceInfoHandler.read(id).thenApply { resourceInfoString ->
            if (resourceInfoString.isNullOrEmpty())
                null
            else {
                val isAvailable = resourceInfoString[0] == '1'
                val splitResourceInfoString = resourceInfoString.split(serialNumberSeparator)

                val serialNumber = splitResourceInfoString[0]
                    .substring(1) // emit availability symbol
                    .toInt()
                val resourceName = splitResourceInfoString[1]

                ResourceInfo(isAvailable, serialNumber, resourceName)
            }
        }
    }

    fun addHardwareResource(id: String, name: String): CompletableFuture<Unit> {
        return getResourceById(id).thenApply { resource ->
            if (resource != null) {
                throw IdAlreadyExistException()
            }
        }.thenCompose { dbSerialNumberToIdHandler.read("size") }.thenCompose { flow ->
                val serialNumber: Int = flow?.toInt() ?: 0

                dbSerialNumberToIdHandler.write(serialNumber.toString(), id).thenCompose {
                    dbSerialNumberToIdHandler.write("size", (serialNumber + 1).toString()).thenCompose {
                        dbIdToResourceInfoHandler.write(id, "$AvailableSymbol$serialNumber$serialNumberSeparator$name") }
                    }
            }
    }

    fun getResourceIdBySerialNumber(serialNumber: Int): CompletableFuture<String?> {
        return dbSerialNumberToIdHandler.read(serialNumber.toString())
    }

    fun getResourcesSize(): CompletableFuture<Int> {
        return dbSerialNumberToIdHandler.read("size")
            .thenApply { size -> size?.toInt() ?: 0 }
    }

    fun MarkResourceAsAvailable(id: String): CompletableFuture<Unit> {
        return getResourceById(id).thenCompose { resource ->
            val serialNumber = resource!!.serialNumber
            val name = resource.name
            dbIdToResourceInfoHandler.write(id, "$AvailableSymbol$serialNumber$serialNumberSeparator$name")
        }
    }
    fun MarkResourceAsUnavailable(id: String): CompletableFuture<Unit> {
        return getResourceById(id).thenCompose { resource ->
            val serialNumber = resource!!.serialNumber
            val name = resource.name
            dbIdToResourceInfoHandler.write(id, "$UnavailableSymbol$serialNumber$serialNumberSeparator$name")
        }
    }
}

data class ResourceInfo (val isAvailable: Boolean, val serialNumber: Int, val name: String)