package services

import il.ac.technion.cs.softwaredesign.services.RequestAccessManager
import il.ac.technion.cs.softwaredesign.services.database.DbRequestAccessHandler
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class RequestAccessManagerTests {
    private val dbRequestHandler = mockk<DbRequestAccessHandler>()
    private val userUsername = "userUsername"

    @BeforeEach
    fun configureAndClearDoubles() {
        every { dbRequestHandler.getNumberOfRequests() } returns CompletableFuture.completedFuture(10)
        every { dbRequestHandler.isActiveBySerialNumber(any()) } returns CompletableFuture.completedFuture(false)
        every { dbRequestHandler.getRequestBySerialNumber(any()) } returns CompletableFuture.completedFuture(null)
        for (i in 0..10 step 2) {
            every { dbRequestHandler.isActiveBySerialNumber(i) } returns CompletableFuture.completedFuture(true)
            every { dbRequestHandler.getRequestBySerialNumber(i) } returns CompletableFuture.completedFuture(Pair(i.toString(), userUsername + i.toString()))
        }
    }

    @Test
    fun `getAllRequestsStrings returns all online user for null param`() {
        // Arrange
        val requestAccessManager = RequestAccessManager(dbRequestHandler)

        // Act
        val actual = requestAccessManager.getAllRequestsStrings().join()

        // Assert
        Assertions.assertEquals(5, actual.size)
    }
}