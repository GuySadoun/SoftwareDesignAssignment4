package services.database

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import il.ac.technion.cs.softwaredesign.NoUsernameExistException
import il.ac.technion.cs.softwaredesign.services.database.DbRequestAccessHandler
import io.mockk.every
import io.mockk.mockk
import main.kotlin.StorageFactory
import main.kotlin.StringSerializerImpl
import org.junit.jupiter.api.*
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
    fun `send a request with empty username, reason and password`(){
        // Arrange
        val requestHandler = DbRequestAccessHandler(dbFactoryMock)

        // Act
        requestHandler.addRequest("", "", "").join()

        val requestPair = requestHandler.getRequestBySerialNumber(0).join()
        val requestPassword = requestHandler.getPasswordByUsername("").join()

        // Assert
        Assertions.assertEquals(Pair("", ""), requestPair)
        Assertions.assertEquals("", requestPassword)

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

    @Test
    fun `get request by serial number return null if there is no request with the suitable serialNumber`(){
        // Arrange
        val requestHandler = DbRequestAccessHandler(dbFactoryMock)

        // Act
        val actual = requestHandler.getRequestBySerialNumber(0).join()

        // Assert
        Assertions.assertNull(actual)
    }

    @Test
    fun `read removed request returns null`(){
        // Arrange
        val requestHandler = DbRequestAccessHandler(dbFactoryMock)
        requestHandler.addRequest(username1, reason1, password).join()
        requestHandler.removeRequest(username1).join()

        // Act
        val actual = requestHandler.getRequestBySerialNumber(0).join()

        // Assert
        Assertions.assertNull(actual)
    }

    @Nested
    inner class `get password by username tests`{
        @Test
        fun `get password works correctly`(){
            // Arrange
            val requestHandler = DbRequestAccessHandler(dbFactoryMock)
            requestHandler.addRequest(username1, reason1, password).join()

            // Act
            val actual = requestHandler.getPasswordByUsername(username1).join()

            // Assert
            Assertions.assertEquals(password, actual)
        }

        @Test
        fun `get password works correctly when password is empty`(){
            // Arrange
            val requestHandler = DbRequestAccessHandler(dbFactoryMock)
            val emptyPassword = ""
            requestHandler.addRequest(username1, reason1, emptyPassword).join()

            // Act
            val actual = requestHandler.getPasswordByUsername(username1).join()

            // Assert
            Assertions.assertEquals(emptyPassword, actual)
        }

        @Test
        fun `get password from non-exists username, throws NoUsernameExistException`(){
            // Arrange
            val requestHandler = DbRequestAccessHandler(dbFactoryMock)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                requestHandler.getPasswordByUsername(username1).join()
            }
            assertThat(throwable.cause!!, isA<NoUsernameExistException>())
        }
    }

    @Test
    fun `getNumberOfRequests works correctly`(){
        // Arrange
        val requestHandler = DbRequestAccessHandler(dbFactoryMock)

        // pre-act Assertion
        Assertions.assertEquals(0, requestHandler.getNumberOfRequests().join())

        // Act
        requestHandler.addRequest(username1, reason1, password).join()
        requestHandler.addRequest(username2, reason2, password).join()

        // Assert
        Assertions.assertEquals(2, requestHandler.getNumberOfRequests().join())
    }

    @Test
    fun `isRequestForUsernameExists works correctly`(){
        // Arrange
        val requestHandler = DbRequestAccessHandler(dbFactoryMock)
        requestHandler.addRequest(username1, reason1, password).join()

        // Act
        val requestForUsername1 = requestHandler.isRequestForUsernameExists(username1).join()
        val requestForUsername2 = requestHandler.isRequestForUsernameExists(username2).join()

        // Assert
        Assertions.assertTrue(requestForUsername1)
        Assertions.assertFalse(requestForUsername2)
    }

    @Test
    fun `isRequestForUsernameExists on removed request return false`(){
        // Arrange
        val requestHandler = DbRequestAccessHandler(dbFactoryMock)
        requestHandler.addRequest(username1, reason1, password).join()
        requestHandler.removeRequest(username1).join()

        // Act
        val actual = requestHandler.isRequestForUsernameExists(username1).join()

        // Assert
        Assertions.assertFalse(actual)
    }

    @Test
    fun `isActiveBySerialNumber works correctly`(){
        // Arrange
        val requestHandler = DbRequestAccessHandler(dbFactoryMock)
        requestHandler.addRequest(username1, reason1, password).join()

        // Act
        val isFirstRequestActive = requestHandler.isActiveBySerialNumber(0).join()
        val isSecondRequestActive = requestHandler.isActiveBySerialNumber(1).join()

        // Assert
        Assertions.assertTrue(isFirstRequestActive)
        Assertions.assertFalse(isSecondRequestActive)
    }

    @Test
    fun `isActiveBySerialNumber on removed request return false`(){
        // Arrange
        val requestHandler = DbRequestAccessHandler(dbFactoryMock)
        requestHandler.addRequest(username1, reason1, password).join()
        requestHandler.removeRequest(username1).join()

        // Act
        val actual = requestHandler.isActiveBySerialNumber(0).join()

        // Assert
        Assertions.assertFalse(actual)
    }
}