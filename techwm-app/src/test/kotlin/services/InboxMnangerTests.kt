package services

import il.ac.technion.cs.softwaredesign.Message
import il.ac.technion.cs.softwaredesign.services.InboxManager
import il.ac.technion.cs.softwaredesign.services.database.DbInboxHandler
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class InboxManagerTests {
    private val dbInboxHandler = mockk<DbInboxHandler>()
    private val userUsername = "userUsername"

    @BeforeEach
    fun configureAndClearDoubles() {
        every { dbInboxHandler.getNextId(any()) } returns CompletableFuture.completedFuture(0)
        every { dbInboxHandler.getNextId(userUsername) } returns CompletableFuture.completedFuture(50)
        every { dbInboxHandler.getMessageById(any(), any()) } returns CompletableFuture.completedFuture(null)
        for (i in 0..50) {
            when (i%2) {
                0 -> every { dbInboxHandler.getMessageById(userUsername, i.toString()) } returns
                        CompletableFuture.completedFuture(Message(i.toString(), "evenUser", "hello world$i"))
                1-> every { dbInboxHandler.getMessageById(userUsername, i.toString()) } returns
                        CompletableFuture.completedFuture(Message(i.toString(), "oddUser", "shalom olam$i"))
            }
        }
    }

    @Test
    fun `getUserInbox returns all massages`() {
        // Arrange
        val inboxManager = InboxManager(dbInboxHandler)

        // Act
        val actual = inboxManager.getUserInbox(userUsername).join()

        // Assert
        Assertions.assertEquals(2, actual?.size)
        Assertions.assertEquals(25, actual["evenUser"]?.size)
        Assertions.assertEquals(25, actual["oddUser"]?.size)
    }

    @Test
    fun `getUserInbox on non-existing user returns empty list`() {
        // Arrange
        val inboxManager = InboxManager(dbInboxHandler)

        // Act
        val actual = inboxManager.getUserInbox("non-exist").join()

        // Assert
        Assertions.assertEquals(0, actual?.size)
    }
}