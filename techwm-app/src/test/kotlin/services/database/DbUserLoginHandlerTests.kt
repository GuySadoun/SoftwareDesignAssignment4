package services.database

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import il.ac.technion.cs.softwaredesign.PermissionLevel
import il.ac.technion.cs.softwaredesign.services.database.DbUserLoginHandler
import io.mockk.every
import io.mockk.mockk
import main.kotlin.StorageFactory
import main.kotlin.StringSerializerImpl
import org.junit.jupiter.api.*
import testDoubles.StorageFake
import java.lang.IllegalArgumentException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class DbUserLoginHandlerTests {
    private val dbFactoryMock = mockk<StorageFactory>()
    private val fakeStorage = StorageFake(StringSerializerImpl())

    private val username1 = "username1"
    private val username2 = "username2"
    private val token1 = "token1"
    private val token2 = "token2"

    @BeforeEach
    fun configureAndClearDoubles() {
        every { dbFactoryMock.open<String>(any(), any()) } returns CompletableFuture.completedFuture(fakeStorage)
        fakeStorage.clearDatabase()
    }

    @Nested
    inner class `login-logout tests` {
        @Test
        fun `login with username succeed`() {
            // Arrange
            val userLoginHandler = DbUserLoginHandler(dbFactoryMock)

            // Act
            userLoginHandler.login(username1, PermissionLevel.USER, token1).join()
            userLoginHandler.login(username2, PermissionLevel.OPERATOR, token2).join()

            // Assert
            Assertions.assertEquals(token1, userLoginHandler.getUsernameTokenIfLoggedIn(username1).join())
            Assertions.assertEquals(token2, userLoginHandler.getUsernameTokenIfLoggedIn(username2).join())
        }

        @Test
        fun `login for logged in user should not do anything`() {
            // Arrange
            val userLoginHandler = DbUserLoginHandler(dbFactoryMock)

            // Act
            userLoginHandler.login(username1, PermissionLevel.USER, token1).join()

            // Assert
            Assertions.assertDoesNotThrow {
                userLoginHandler.login(username1, PermissionLevel.USER, token2).join()
            }
            Assertions.assertEquals(token2, userLoginHandler.getUsernameTokenIfLoggedIn(username1).join())
        }

        @Test
        fun `login with empty username succeed`() {
            // Arrange
            val userLoginHandler = DbUserLoginHandler(dbFactoryMock)

            // Act
            userLoginHandler.login("", PermissionLevel.USER, token1).join()

            // Assert
            Assertions.assertEquals(token1, userLoginHandler.getUsernameTokenIfLoggedIn("").join())
        }

        @Test
        fun `logout after login logging out the user`() {
            // Arrange
            val userLoginHandler = DbUserLoginHandler(dbFactoryMock)

            // Act
            userLoginHandler.login(username1, PermissionLevel.USER, token1).join()
            userLoginHandler.login(username2, PermissionLevel.USER, token1).join()
            userLoginHandler.logout(username1, PermissionLevel.USER).join()
            userLoginHandler.logout(username2, PermissionLevel.USER).join()

            // Assert
            Assertions.assertNull(userLoginHandler.getUsernameTokenIfLoggedIn(username1).join())
            Assertions.assertNull(userLoginHandler.getUsernameTokenIfLoggedIn(username2).join())
        }

        @Test
        fun `logout for non existing user throws exception`() {
            // Arrange
            val userLoginHandler = DbUserLoginHandler(dbFactoryMock)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                userLoginHandler.logout("non-exist", PermissionLevel.ADMINISTRATOR).join()
            }

            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }

        @Test
        fun `2 logout follows login throws exception`() {
            // Arrange
            val userLoginHandler = DbUserLoginHandler(dbFactoryMock)

            // Act
            userLoginHandler.login(username1, PermissionLevel.USER, token1).join()
            userLoginHandler.logout(username1, PermissionLevel.USER).join()

            // Assert
            val throwable = assertThrows<CompletionException> {
                userLoginHandler.logout(username1, PermissionLevel.USER).join()
            }

            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }
    }

    @Test
    fun `logout after login logging out the user`() {
        // Arrange
        val userLoginHandler = DbUserLoginHandler(dbFactoryMock)

        // Act & Assert
        val firstSerial = userLoginHandler.getNextSerialNumber().join()
        userLoginHandler.login(username1, PermissionLevel.USER, token1).join()
        val secondSerial = userLoginHandler.getNextSerialNumber().join()
        userLoginHandler.login(username2, PermissionLevel.USER,token1).join()
        userLoginHandler.logout(username1, PermissionLevel.USER).join()
        val thirdSerial = userLoginHandler.getNextSerialNumber().join()


        // Assert
        Assertions.assertEquals(0, firstSerial)
        Assertions.assertEquals(1, secondSerial)
        Assertions.assertEquals(2, thirdSerial)
    }

    @Test
    fun `getUsernameTokenIfLoggedIn returns null for non-existing user`() {
        // Arrange
        val userLoginHandler = DbUserLoginHandler(dbFactoryMock)

        // Act & Assert
        val actual = userLoginHandler.getUsernameTokenIfLoggedIn("non-existing").join()

        // Assert
        Assertions.assertNull(actual)
    }

    @Nested
    inner class `getUsernameBySerialNumIfOnline tests` {
        @Test
        fun `if username is not online returns null`() {
            // Arrange
            val userLoginHandler = DbUserLoginHandler(dbFactoryMock)

            // Act
            userLoginHandler.login(username1, PermissionLevel.USER, token1).join()
            userLoginHandler.logout(username1, PermissionLevel.USER).join()
            val actual = userLoginHandler.getUsernameBySerialNumIfOnline(0, PermissionLevel.USER).join()

            // Assert
            Assertions.assertNull(actual)
        }

        @Test
        fun `if username is online returns user`() {
            // Arrange
            val userLoginHandler = DbUserLoginHandler(dbFactoryMock)

            // Act
            userLoginHandler.login(username1, PermissionLevel.USER, token1).join()
            val actual = userLoginHandler.getUsernameBySerialNumIfOnline(0, PermissionLevel.USER).join()

            // Assert
            Assertions.assertEquals(username1, actual)
        }
    }
}