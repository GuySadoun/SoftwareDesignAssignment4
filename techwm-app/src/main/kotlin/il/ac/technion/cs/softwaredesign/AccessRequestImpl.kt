package il.ac.technion.cs.softwaredesign

import java.util.concurrent.CompletableFuture

class AccessRequestImpl(override val requestingUsername: String, override val reason: String) : AccessRequest  {
    override fun approve(): CompletableFuture<Unit> {
        TODO("Not yet implemented")
        //go to db to get the password
        //register


    //AccessRequestWithPassword().approve
    }

    override fun decline(): CompletableFuture<Unit> {
        TODO("Not yet implemented")
    }
}