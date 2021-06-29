package services

import il.ac.technion.cs.softwaredesign.PermissionLevel
import il.ac.technion.cs.softwaredesign.services.UserLoginManager
import il.ac.technion.cs.softwaredesign.services.database.DbUserLoginHandler
import io.mockk.every
import io.mockk.mockk
import main.kotlin.StorageFactory
import main.kotlin.StringSerializerImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testDoubles.StorageFake
import java.util.concurrent.CompletableFuture

class UserLoginManagerTests {
    private val dbLoginHandler = mockk<DbUserLoginHandler>()
    private val userUsername = "userUsername"
    private val operatorUsername = "operatorUsername"
    private val adminUsername = "adminUsername"

    @BeforeEach
    fun configureAndClearDoubles() {
        every { dbLoginHandler.getNextSerialNumber() } returns CompletableFuture.completedFuture(5)
        every { dbLoginHandler.getUsernameBySerialNumIfOnline(any(), any()) } returns CompletableFuture.completedFuture(null)
        for (i in 0..5) {
            when (i%3) {
                0 -> every { dbLoginHandler.getUsernameBySerialNumIfOnline(i, PermissionLevel.USER) } returns CompletableFuture.completedFuture(userUsername + i.toString())
                1 -> every { dbLoginHandler.getUsernameBySerialNumIfOnline(i, PermissionLevel.OPERATOR) } returns CompletableFuture.completedFuture(operatorUsername + i.toString())
                2 -> every { dbLoginHandler.getUsernameBySerialNumIfOnline(i, PermissionLevel.ADMINISTRATOR) } returns CompletableFuture.completedFuture(adminUsername + i.toString())
            }
        }
    }

    @Test
    fun `getOnlineUsers returns all online user for null param`() {
        // Arrange
        val loginManager = UserLoginManager(dbLoginHandler)

        // Act
        val actual = loginManager.getOnlineUsers(null).join()

        // Assert
        Assertions.assertEquals(5, actual.size)
    }

    @Test
    fun `getOnlineUsers for specific permission level`() {
        // Arrange
        val loginManager = UserLoginManager(dbLoginHandler)

        // Act
        val usersActual = loginManager.getOnlineUsers(PermissionLevel.USER).join()
        val operatorActual = loginManager.getOnlineUsers(PermissionLevel.OPERATOR).join()
        val adminActual = loginManager.getOnlineUsers(PermissionLevel.ADMINISTRATOR).join()


        // Assert
        Assertions.assertEquals(2, usersActual.size)
        Assertions.assertEquals(2, operatorActual.size)
        Assertions.assertEquals(1, adminActual.size)
    }
}