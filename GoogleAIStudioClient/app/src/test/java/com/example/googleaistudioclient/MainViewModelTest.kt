
package com.example.googleaistudioclient

import app.cash.turbine.test
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.TextPart
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val mockGenerativeModel: GenerativeModel = mock(GenerativeModel::class.java)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MainViewModel()
        // Mock the generativeModel in the ViewModel
        // This is a simplified mock, in a real app you might use a DI framework to inject mocks
        // For now, we'll use reflection or a test-specific constructor if available
        // Since GenerativeModel is final, we can't mock it directly with Mockito without additional setup
        // For this example, we'll assume a way to inject it or test around it.
        // For the purpose of this test, we'll focus on the ViewModel's logic assuming GenerativeModel works.
    }

    @Test
    fun `initializeChat sets up system prompt and initial messages`() = runTest {
        val apiKey = "test_api_key"
        val systemPrompt = "You are a helpful assistant."

        viewModel.chatMessages.test {
            assertEquals(emptyList<String>(), awaitItem())
            viewModel.initializeChat(apiKey, systemPrompt)
            advanceUntilIdle()
            assertEquals(listOf("System: You are a helpful assistant.", "AI: ОК. Я готовий."), awaitItem())
        }
    }

    @Test
    fun `sendMessage adds user message and AI response`() = runTest {
        val apiKey = "test_api_key"
        val systemPrompt = ""
        viewModel.initializeChat(apiKey, systemPrompt)
        advanceUntilIdle()

        // Mock the chat response
        val mockChat = mock(com.google.ai.client.generativeai.Chat::class.java)
        val mockResponse = mock(GenerateContentResponse::class.java)
        `when`(mockResponse.text).thenReturn("Hello from AI!")
        `when`(mockChat.sendMessage("Hello")).thenReturn(mockResponse)
        `when`(mockGenerativeModel.startChat()).thenReturn(mockChat)

        viewModel.chatMessages.test {
            // Skip initial empty list and any system prompt messages
            awaitItem()

            viewModel.sendMessage("Hello")
            advanceUntilIdle()
            assertEquals(listOf("User: Hello", "AI: Hello from AI!"), awaitItem())
        }
    }

    @Test
    fun `speak emits correct message`() = runTest {
        viewModel.speakEvent.test {
            viewModel.speak("Test speech")
            assertEquals("Test speech", awaitItem())
        }
    }

    @Test
    fun `generateImage updates isLoading and generatedImage`() = runTest {
        // Due to the complexity of mocking GenerativeModel and Bitmap, this test will focus
        // on the isLoading state and the fact that generatedImage is updated (not null).
        // A more comprehensive test would require a proper mocking framework or dependency injection.

        viewModel.isLoading.test {
            assertEquals(false, awaitItem())
            viewModel.generateImage("a red car")
            assertEquals(true, awaitItem())
            advanceUntilIdle()
            assertEquals(false, awaitItem())
        }

        viewModel.generatedImage.test {
            assertEquals(null, awaitItem())
            viewModel.generateImage("a blue sky")
            advanceUntilIdle()
            // Assert that the generatedImage is not null after generation attempt
            assert(awaitItem() != null)
        }
    }
}

