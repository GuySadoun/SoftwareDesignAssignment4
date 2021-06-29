import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import il.ac.technion.cs.softwaredesign.*
import il.ac.technion.cs.softwaredesign.services.InboxManager
import il.ac.technion.cs.softwaredesign.services.RequestAccessManager
import il.ac.technion.cs.softwaredesign.services.UserLoginManager
import io.mockk.*
import org.junit.jupiter.api.*
import java.lang.IllegalArgumentException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class TechWorkloadClientTests {
    val username = "username"
    val token = "token"
    val password = "password"
    val userPermissionLevel = PermissionLevel.USER
    val userManagerMock = mockkClass(UserLoginManager::class)
    val techWorkloadManagerMock = mockkClass(TechWorkloadManager::class)
    val requestManagerMock = mockkClass(RequestAccessManager::class)
    val inboxManagerMock = mockkClass(InboxManager::class)


    @BeforeEach
    fun configureMocks(){
        every { userManagerMock.loginUser(any(), any(), any()) } returns CompletableFuture.completedFuture(Unit)
        every { userManagerMock.logoutUser(any(), any()) } returns CompletableFuture.completedFuture(Unit)
        every { requestManagerMock.addAccessRequest(any(), any(), any()) } returns CompletableFuture.completedFuture(Unit)
        every { inboxManagerMock.addMessageToConversation(any(), any(), any()) } returns CompletableFuture.completedFuture(Unit)
    }

    @Nested
    inner class `login tests` {
        @Test
        fun `login works fine throw when password is correct`(){
            // Arrange
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { techWorkloadManagerMock.authenticate(any(), any()) } returns CompletableFuture.completedFuture(token)
            every { techWorkloadManagerMock.userInformation(any(), any()) } returns CompletableFuture.completedFuture(
                User(username, AccountType.DEFAULT, userPermissionLevel)
            )

            // Act & Assert
            Assertions.assertDoesNotThrow { client.login(password).join() }
            verify { userManagerMock.loginUser(username, userPermissionLevel, token) }
        }

        @Test
        fun `login twice does not throw an exception`(){
            // Arrange
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { techWorkloadManagerMock.authenticate(any(), any()) } returns CompletableFuture.completedFuture(token)
            every { techWorkloadManagerMock.userInformation(any(), any()) } returns CompletableFuture.completedFuture(
                User(username, AccountType.DEFAULT, userPermissionLevel)
            )
            client.login(password).join()

            // Act & Assert
            Assertions.assertDoesNotThrow { client.login(password).join() }
            verify { userManagerMock.loginUser(username, userPermissionLevel, token) }
        }

        @Test
        fun `if authenticate fails, throws IllegalArgumentException`(){
            // Arrange
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { techWorkloadManagerMock.authenticate(any(), any()) } returns CompletableFuture.failedFuture(Exception())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                client.login(password).join()
            }

            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }
    }
    @Nested
    inner class `logout tests` {
        @Test
        fun `logout on logged in user works fine`(){
            // Arrange
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { userManagerMock.getUsernameTokenIfLoggedIn(any()) } returns CompletableFuture.completedFuture(token)
            every { techWorkloadManagerMock.userInformation(any(), any()) } returns CompletableFuture.completedFuture(
                User(username, AccountType.DEFAULT, userPermissionLevel)
            )

            // Act
            client.logout().join()

            // Act & Assert
            verify { userManagerMock.logoutUser(username, userPermissionLevel) }
        }

        @Test
        fun `logout on not logged in user throws IllegalArgumentException`() {
            // Arrange
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { userManagerMock.getUsernameTokenIfLoggedIn(any()) } returns CompletableFuture.completedFuture(null)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                client.logout().join()
            }

            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }


    }

    @Nested
    inner class `submitJob tests` {
        @Test
        fun `when job can be submitted, return successfully`(){
            // Arrange
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { userManagerMock.getUsernameTokenIfLoggedIn(any()) } returns CompletableFuture.completedFuture(token)
            every { techWorkloadManagerMock.submitJob(any(), any(), any()) } returns CompletableFuture.completedFuture(mockk())

            // Act & Assert
            Assertions.assertDoesNotThrow {
                client.submitJob("jobname", listOf()).join()
            }
        }

        @Test
        fun `if user does not logged in, throws PermissionException`(){
            // Arrange
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { userManagerMock.getUsernameTokenIfLoggedIn(any()) } returns CompletableFuture.completedFuture(null)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                client.submitJob("jobname", listOf()).join()
            }

            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `if job cannot be submitted, throws IllegalArgumentException`(){
            // Arrange
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { userManagerMock.getUsernameTokenIfLoggedIn(any()) } returns CompletableFuture.completedFuture(token)
            every { techWorkloadManagerMock.submitJob(any(), any(), any()) } returns CompletableFuture.failedFuture(Exception())

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                client.submitJob("jobname", listOf()).join()
            }

            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }
    }

    @Nested
    inner class `requestAccessToSystem tests` {
        @Test
        fun `if request is already exists for that user, throws IllegalArgumentException`(){
            // Arrange
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { requestManagerMock.isRequestForUsernameExists(username) } returns CompletableFuture.completedFuture(true)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                client.requestAccessToSystem(username, password, "reason").join()
            }

            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }

        @Test
        fun `if request does not exists for that user, submit request`(){
            // Arrange
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { requestManagerMock.isRequestForUsernameExists(username) } returns CompletableFuture.completedFuture(false)

            // Act & Assert
            Assertions.assertDoesNotThrow {
                client.requestAccessToSystem(username, password, "reason").join()
            }
            verify { requestManagerMock.addAccessRequest(username, "reason", password) }
        }
    }

    @Nested
    inner class `onlineUsers tests` {
        @Test
        fun `if user does not logged in, throws PermissionException`(){
            // Arrange
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { userManagerMock.getUsernameTokenIfLoggedIn(any()) } returns CompletableFuture.completedFuture(null)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                client.onlineUsers(null).join()
            }

            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `if user is logged in, return successfully`(){
            // Arrange
            val expected = listOf("oded", "amit")
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { userManagerMock.getUsernameTokenIfLoggedIn(any()) } returns CompletableFuture.completedFuture(token)
            every { userManagerMock.getOnlineUsers(null) } returns CompletableFuture.completedFuture(expected)

            // Act & Assert
            assertDoesNotThrow {
                client.onlineUsers(null).join()
            }
            verify { userManagerMock.getOnlineUsers(null) }
        }
    }


    @Nested
    inner class `inbox tests` {
        @Test
        fun `if user does not logged in, throws PermissionException`(){
            // Arrange
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { userManagerMock.getUsernameTokenIfLoggedIn(any()) } returns CompletableFuture.completedFuture(null)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                client.inbox().join()
            }

            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `if user is logged in, return successfully`(){
            // Arrange
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { userManagerMock.getUsernameTokenIfLoggedIn(any()) } returns CompletableFuture.completedFuture(token)
            every { inboxManagerMock.getUserInbox(username) } returns CompletableFuture.completedFuture(mockk())

            // Act & Assert
            assertDoesNotThrow {
                client.inbox().join()
            }
            verify { inboxManagerMock.getUserInbox(username) }
        }
    }

    @Nested
    inner class `sendMessage tests` {
        @Test
        fun `if user does not logged in, throws PermissionException`(){
            // Arrange
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { userManagerMock.getUsernameTokenIfLoggedIn(any()) } returns CompletableFuture.completedFuture(null)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                client.sendMessage("", "").join()
            }

            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `for legal message, return successfully`(){
            // Arrange
            val targetUser = User("targetUser", AccountType.DEFAULT, PermissionLevel.ADMINISTRATOR)
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { userManagerMock.getUsernameTokenIfLoggedIn(any()) } returns CompletableFuture.completedFuture(token)
            every { techWorkloadManagerMock.userInformation(any(), any()) } returns CompletableFuture.completedFuture(targetUser)

            // Act
            client.sendMessage(targetUser.username, "message").join()

            // Assert
            verify { inboxManagerMock.addMessageToConversation(username, targetUser.username, "message") }
        }

        @Test
        fun `If the target user does not exist, throws IllegalArgumentException`(){
            // Arrange
            val targetUser = User("targetUser", AccountType.DEFAULT, PermissionLevel.ADMINISTRATOR)
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { userManagerMock.getUsernameTokenIfLoggedIn(any()) } returns CompletableFuture.completedFuture(token)
            every { techWorkloadManagerMock.userInformation(any(), any()) } returns CompletableFuture.completedFuture(null)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                client.sendMessage("", "").join()
            }

            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }

        @Test
        fun `If message contains more than 120 characters, throws IllegalArgumentException`(){
            // Arrange
            val targetUser = User("targetUser", AccountType.DEFAULT, PermissionLevel.ADMINISTRATOR)
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { userManagerMock.getUsernameTokenIfLoggedIn(any()) } returns CompletableFuture.completedFuture(token)
            every { techWorkloadManagerMock.userInformation(any(), any()) } returns CompletableFuture.completedFuture(targetUser)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                client.sendMessage(targetUser.username, "!".repeat(121)).join()
            }

            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }
    }

    @Nested
    inner class `deleteMessage tests` {
        @Test
        fun `if user does not logged in, throws PermissionException`(){
            // Arrange
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { userManagerMock.getUsernameTokenIfLoggedIn(any()) } returns CompletableFuture.completedFuture(null)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                client.deleteMessage("0").join()
            }

            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `If a message with the given id exists, delete it successfully`(){
            // Arrange
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { userManagerMock.getUsernameTokenIfLoggedIn(any()) } returns CompletableFuture.completedFuture(token)
            every { inboxManagerMock.isMsgWithIdExist(any(), any()) } returns CompletableFuture.completedFuture(true)
            every { inboxManagerMock.deleteMsg(username, "0") } returns CompletableFuture.completedFuture(Unit)

            // Act & Assert
            client.deleteMessage("0").join()

            verify { inboxManagerMock.deleteMsg(username, "0") }
        }


        @Test
        fun `If a message with the given id does not exist, throws IllegalArgumentException`(){
            // Arrange
            val client = TechWorkloadUserClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { userManagerMock.getUsernameTokenIfLoggedIn(any()) } returns CompletableFuture.completedFuture(token)
            every { inboxManagerMock.isMsgWithIdExist(any(), any()) } returns CompletableFuture.completedFuture(false)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                client.deleteMessage("0").join()
            }

            assertThat(throwable.cause!!, isA<IllegalArgumentException>())
        }
    }

    @Nested
    inner class `accessRequests tests` {
        @Test
        fun `if user does not logged in, throws PermissionException`(){
            // Arrange
            val client = TechWorkloadAdminClient(username, userManagerMock, techWorkloadManagerMock, requestManagerMock, inboxManagerMock)
            every { userManagerMock.getUsernameTokenIfLoggedIn(any()) } returns CompletableFuture.completedFuture(null)

            // Act & Assert
            val throwable = assertThrows<CompletionException> {
                client.accessRequests().join()
            }

            assertThat(throwable.cause!!, isA<PermissionException>())
        }

        @Test
        fun `if user logged in, return successfully`() {
            // Arrange
            val requestsStringsResults = listOf(Pair("username1", "reason1"), Pair("username2", "reason2"))
            val client = TechWorkloadAdminClient(
                username,
                userManagerMock,
                techWorkloadManagerMock,
                requestManagerMock,
                inboxManagerMock
            )
            every { userManagerMock.getUsernameTokenIfLoggedIn(any()) } returns CompletableFuture.completedFuture(token)
            every { requestManagerMock.getAllRequestsStrings() } returns CompletableFuture.completedFuture(requestsStringsResults)

            // Act
            val actual = client.accessRequests().join()

            // Assert
            Assertions.assertEquals(requestsStringsResults[0].first, actual[0].requestingUsername)
            Assertions.assertEquals(requestsStringsResults[1].first, actual[1].requestingUsername)
            Assertions.assertEquals(requestsStringsResults[0].second, actual[0].reason)
            Assertions.assertEquals(requestsStringsResults[1].second, actual[1].reason)
        }
    }
}