package services.database

import il.ac.technion.cs.softwaredesign.Message
import il.ac.technion.cs.softwaredesign.services.database.DbInboxHandler
import io.mockk.every
import io.mockk.mockk
import main.kotlin.PairSerializerImpl
import main.kotlin.StorageFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testDoubles.StorageFake
import java.util.concurrent.CompletableFuture

class DbInboxHandlerTests {
    private val username1 = "u1"
    private val username2 = "u2"

    private val from1To2_1 = "from1To2_1"
    private val from1To2_2 = "from1To2_2"
    private val from2To1_1 = "from2To1_1"
    private val from2To1_2 = "from2To1_2"
    private val emptyMessage = ""

    private val messageId1 = "0"
    private val messageId2 = "1"


    private val dbFactoryMock = mockk<StorageFactory>()
    private val fakeStorage = StorageFake(PairSerializerImpl())

    @BeforeEach
    fun configureAndClearDoubles() {
        every { dbFactoryMock.open<Pair<String, String>>(any(), any()) } returns CompletableFuture.completedFuture(fakeStorage)
        fakeStorage.clearDatabase()
    }

    @Test
    fun `add 2 messages and get them by id`() {
        // Arrange
        val inboxHandler = DbInboxHandler(dbFactoryMock)

        // Act
        inboxHandler.addMessage(from = username1, to = username2, from1To2_1).join()
        inboxHandler.addMessage(from = username1, to = username2, from1To2_2).join()
        inboxHandler.addMessage(from = username2, to = username1, from2To1_1).join()
        inboxHandler.addMessage(from = username2, to = username1, from2To1_2).join()

        //Assert
        val username1Message2 = inboxHandler.getMessageById(username1, messageId2).join()
        val username2Message1 = inboxHandler.getMessageById(username2, messageId1).join()

        Assertions.assertEquals(Message(messageId2, username2, from2To1_2), username1Message2)
        Assertions.assertEquals(Message(messageId1, username1, from1To2_1), username2Message1)
    }

    @Test
    fun `getNextId return correctly`() {
        // Arrange
        val inboxHandler = DbInboxHandler(dbFactoryMock)
        inboxHandler.addMessage(from = username1, to = username2, from1To2_1).join()
        inboxHandler.addMessage(from = username1, to = username2, from1To2_2).join()
        inboxHandler.addMessage(from = username1, to = username2, from1To2_2).join()

        // Act
        val username1_nextId = inboxHandler.getNextId(username1).join()
        val username2_nextId = inboxHandler.getNextId(username2).join()

        //Assert
        Assertions.assertEquals(0, username1_nextId)
        Assertions.assertEquals(3, username2_nextId)
    }

    @Test
    fun `getMessageById return null after message was deleted`() {
        // Arrange
        val inboxHandler = DbInboxHandler(dbFactoryMock)
        inboxHandler.addMessage(from = username1, to = username2, from1To2_1).join()
        inboxHandler.addMessage(from = username1, to = username2, from1To2_2).join()
        inboxHandler.addMessage(from = username1, to = username2, from1To2_2).join()

        // Act
        inboxHandler.deleteMsg(username2, messageId2).join()

        //Assert
        Assertions.assertNull(inboxHandler.getMessageById(username2, messageId2).join())
    }

    @Test
    fun `write and read empty message`() {
        // Arrange
        val inboxHandler = DbInboxHandler(dbFactoryMock)

        // Act
        inboxHandler.addMessage(from = username1, to = username2, emptyMessage).join()

        //Assert
        val username1Message2 = inboxHandler.getMessageById(username2, messageId1).join()

        Assertions.assertEquals(Message(messageId1, username1, emptyMessage), username1Message2)
    }

    @Test
    fun `write and read message from empty user`() {
        // Arrange
        val inboxHandler = DbInboxHandler(dbFactoryMock)

        // Act
        inboxHandler.addMessage(from = "", to = username2, from1To2_1).join()

        //Assert
        val username1Message2 = inboxHandler.getMessageById(username2, messageId1).join()

        Assertions.assertEquals(Message(messageId1, "", from1To2_1), username1Message2)
    }

    @Test
    fun `write and read message to empty user`() {
        // Arrange
        val inboxHandler = DbInboxHandler(dbFactoryMock)

        // Act
        inboxHandler.addMessage(from = username1, to = "", from1To2_1).join()

        //Assert
        val username1Message2 = inboxHandler.getMessageById("", messageId1).join()

        Assertions.assertEquals(Message(messageId1, username1, from1To2_1), username1Message2)
    }

    @Test
    fun `write and read empty message from empty user`() {
        // Arrange
        val inboxHandler = DbInboxHandler(dbFactoryMock)

        // Act
        inboxHandler.addMessage(from = "", to = username2, emptyMessage).join()

        //Assert
        val username1Message2 = inboxHandler.getMessageById(username2, messageId1).join()

        Assertions.assertEquals(Message(messageId1, "", emptyMessage), username1Message2)
    }
}