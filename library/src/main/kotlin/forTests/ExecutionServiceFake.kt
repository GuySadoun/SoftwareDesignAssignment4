package library

import il.ac.technion.cs.softwaredesign.execution.CPUResource
import il.ac.technion.cs.softwaredesign.execution.ExecutionService
import il.ac.technion.cs.softwaredesign.execution.GPUResource
import il.ac.technion.cs.softwaredesign.execution.GeneralResource
import java.util.concurrent.CompletableFuture

class ExecutionServiceFake : ExecutionService {
    val resources = mutableMapOf<String, Boolean>()

    override fun allocateResource(id: String): CompletableFuture<GeneralResource> {
        return CompletableFuture.supplyAsync {
            if (resources[id] != true)
                throw Exception("resource already allocated!")
            resources[id] = false
            if(id[0] == 'c')
                CPUForTests(id, "name")
            else
                GPUForTests(id, "name")
        }
    }

    override fun releaseResource(resource: GeneralResource): CompletableFuture<Unit> {
        return CompletableFuture.supplyAsync {
            resources[resource.id] = true
        }
    }

    override fun verifyResource(id: String): CompletableFuture<Class<out GeneralResource>> {
        return CompletableFuture.supplyAsync {
            resources[id] = true
            if(id[0] == 'c')
                CPUResource::class.java
            else
                GPUResource::class.java

        }
    }
}

class CPUForTests (override val id: String,override val name: String) : CPUResource {
    override fun<T> invoke(function: () -> T): T { return function.invoke() }
}
class GPUForTests (override val id: String,override val name: String) : GPUResource {
    override fun add(a: Array<Number>, b: Array<Number>): Array<Number> {
        TODO("Not yet implemented")
    }

    override fun multiply(a: Array<Number>, b: Array<Number>): Array<Number> {
        TODO("Not yet implemented")
    }
}