package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.services.RequestAccessManager
import java.util.concurrent.CompletableFuture

class AccessRequestImpl(
    override val requestingUsername: String,
    override val reason: String,
    private val techWM: TechWorkloadManager,
    private val requestManager: RequestAccessManager,
    private val token: String
) : AccessRequest  {

    override fun approve(): CompletableFuture<Unit> {
        return requestManager.getPasswordByUsername(requestingUsername).thenCompose { password ->
            techWM.register(token, requestingUsername, password, PermissionLevel.USER).thenCompose {
                requestManager.removeRequest(requestingUsername)
            }
        }
    }

    override fun decline(): CompletableFuture<Unit> {
        return requestManager.removeRequest(requestingUsername)
    }
}