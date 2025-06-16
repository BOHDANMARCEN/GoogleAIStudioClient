package com.example.googleaistudioclient

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    private val _chatMessages = MutableStateFlow<List<Message>>(emptyList())
    val chatMessages: StateFlow<List<Message>> = _chatMessages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _generatedImage = MutableStateFlow<Bitmap?>(null)
    val generatedImage: StateFlow<Bitmap?> = _generatedImage.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _speakEvent = MutableSharedFlow<String>()
    val speakEvent: SharedFlow<String> = _speakEvent

    private var apiKey: String? = null
    private var systemPrompt: String? = null
    private lateinit var generativeModel: GenerativeModel

    fun initializeChat(apiKey: String, systemPrompt: String) {
        if (apiKey.isBlank()) {
            _error.value = "API key cannot be empty"
            return
        }
        this.apiKey = apiKey
        this.systemPrompt = systemPrompt
        _error.value = null
        _chatMessages.value = emptyList()
        generativeModel = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = apiKey
        )
        viewModelScope.launch {
            if (!systemPrompt.isNullOrBlank()) {
                val chat = generativeModel.startChat(
                    history = listOf(
                        content(role = "user") { text(systemPrompt) },
                        content(role = "model") { text("ОК. Я готовий.") }
                    )
                )
                _chatMessages.value = _chatMessages.value + Message(
                    content = "System: $systemPrompt",
                    isUser = false
                )
                _chatMessages.value = _chatMessages.value + Message(
                    content = "AI: ОК. Я готовий.",
                    isUser = false
                )
            }
        }
    }

    fun sendMessage(message: String) {
        if (apiKey == null) {
            _error.value = "Please initialize chat with API key first"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Add user message
                _chatMessages.value = _chatMessages.value + Message(
                    content = "User: $message",
                    isUser = true
                )

                val chat = generativeModel.startChat()
                val response = chat.sendMessage(message)
                response.text?.let { 
                    _chatMessages.value = _chatMessages.value + Message(
                        content = "AI: $it",
                        isUser = false
                    )
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateImage(prompt: String) {
        if (apiKey == null) {
            _error.value = "Please initialize chat with API key first"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val imageGenerativeModel = GenerativeModel(
                    modelName = "gemini-pro-vision",
                    apiKey = generativeModel.apiKey
                )
                val response = imageGenerativeModel.generateContent(prompt)
                response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.blob?.data?.let {
                    val decodedString = Base64.decode(it.toByteArray(), Base64.DEFAULT)
                    _generatedImage.value = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                } ?: run {
                    _error.value = "Failed to generate image"
                }
            } catch (e: Exception) {
                Log.e("ImageGeneration", "Error generating image: ${e.message}")
                _error.value = "Error generating image: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun speak(text: String) {
        if (text.isNotBlank()) {
            viewModelScope.launch {
                _speakEvent.emit(text)
            }
        }
    }
}

