package services.database

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import il.ac.technion.cs.softwaredesign.services.database.DbRequestAccessHandler
import io.mockk.every
import io.mockk.mockk
import main.kotlin.StorageFactory
import main.kotlin.StringSerializerImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import testDoubles.StorageFake
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class DbRequestAccessHandlerTests {
    private val dbFactoryMock = mockk<StorageFactory>()
    private val fakeStorage = StorageFake(StringSerializerImpl())

    private val username1 = "username1"
    private val username2 = "username2"
    private val reason1 = "reason1"
    private val reason2 = "reason2"
    private val password = "password"

    @BeforeEach
    fun configureAndClearDoubles() {
        every { dbFactoryMock.open<String>(any(), any()) } returns CompletableFuture.completedFuture(fakeStorage)
        fakeStorage.clearDatabase()
    }

    @Test
    fun `send a request and then read it by serial number`(){
        // Arrange
        val requestHandler = DbRequestAccessHandler(dbFactoryMock)

        // Act
        requestHandler.addRequest(username1, reason1, password).join()
        requestHandler.addRequest(username2, reason2, password).join()

        val firstRequest = requestHandler.getRequestBySerialNumber(0).join()
        val secondRequest = requestHandler.getRequestBySerialNumber(1).join()

        // Assert
        Assertions.assertEquals(Pair(username1, reason1), firstRequest)
        Assertions.assertEquals(Pair(username2, reason2), secondRequest)
    }

    @Test
    fun `cant add two requests from the same username, should throw IllegalArgumentException`(){
        // Arrange
        val requestHandler = DbRequestAccessHandler(dbFactoryMock)
        requestHandler.addRequest("username", "reason", "password")

        // Act & Assert
        val throwable = assertThrows<CompletionException> {
            requestHandler.addRequest("username", "reason", "password").join()
        }

        assertThat(throwable.cause!!, isA<IllegalArgumentException>())
    }
}