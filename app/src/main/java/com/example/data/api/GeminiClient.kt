package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessage
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val service = retrofit.create(GeminiApiService::class.java)

    // Helper to extract API key safely
    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Generates a response representing an anonymous stranger chat partner.
     */
    suspend fun generateStrangerReply(
        partnerName: String,
        partnerAge: Int,
        partnerGender: String,
        chatHistory: List<ChatMessage>
    ): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured.")
            return getFallbackResponse(partnerName)
        }

        // Format history for Gemini
        val promptBuilder = StringBuilder()
        promptBuilder.append("We are simulating an anonymous chat like Omegle. ")
        promptBuilder.append("You are playing the role of a stranger named $partnerName, a $partnerAge year old $partnerGender. ")
        promptBuilder.append("Keep your responses conversational, informal, youthful, slightly casual, and brief (under 2 sentences). ")
        promptBuilder.append("Never admit you are an AI. Speak like a real person over chat text.\n\n")
        promptBuilder.append("Chat History:\n")

        chatHistory.takeLast(12).forEach { msg ->
            val senderLabel = if (msg.sender == "user") "Me" else partnerName
            promptBuilder.append("$senderLabel: ${msg.messageText}\n")
        }
        promptBuilder.append("$partnerName: ")

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = promptBuilder.toString())))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.8,
                maxOutputTokens = 150
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = "You are a real human stranger chatting on an app named Uskha. Play the persona meticulously. Do not sound like a robotic assistant."))
            )
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: getFallbackResponse(partnerName)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini for stranger chat response", e)
            getFallbackResponse(partnerName)
        }
    }

    /**
     * Performs content moderation on a conversation block to flag inappropriate behavior.
     */
    suspend fun analyzeConversationModeration(
        reportingHistory: List<ChatMessage>
    ): ModerationResult {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is missing. Skipping actual API moderation check.")
            // Simulate locally based on words
            return inspectLocally(reportingHistory)
        }

        val chatExcerpt = reportingHistory.takeLast(10).joinToString("\n") {
            "${it.sender.uppercase()}: ${it.messageText}"
        }

        val prompt = """
            You are a rigorous safety moderation AI. Analyze the following anonymous conversation excerpt for safety violations.
            Check for:
            1. Harassment, insults, or violent threats.
            2. Hardcore explicit NSFW/sexual text.
            3. Dangerous activities, scamming, or spam.
            4. Illegal drug promotion or self-harm encouragement.
            
            Your response MUST be formatted strictly as:
            - If safe, reply exactly: SAFE
            - If unsafe, reply in this exact format: INAPPROPRIATE: [short reason of violation]
            
            Conversation Excerpt:
            $chatExcerpt
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.2,
                maxOutputTokens = 40
            )
        )

        return try {
            val response = service.generateContent(apiKey, request)
            val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: "SAFE"
            if (resultText.startsWith("INAPPROPRIATE", ignoreCase = true)) {
                val reason = resultText.substringAfter(":").trim()
                ModerationResult(isSafe = false, reason = if (reason.isNotEmpty()) reason else "Inappropriate conduct flagged by AI")
            } else {
                ModerationResult(isSafe = true, reason = null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini for content safety verification", e)
            inspectLocally(reportingHistory)
        }
    }

    // Backup local scanner to work even without any API key or during network outage
    private fun inspectLocally(history: List<ChatMessage>): ModerationResult {
        val badWords = listOf("abuse", "kill yourself", "send nudes", "scam", "hack", "fuck you", "dick", "bitch", "slut")
        history.forEach { msg ->
            val text = msg.messageText.lowercase()
            badWords.forEach { word ->
                if (text.contains(word)) {
                    return ModerationResult(isSafe = false, reason = "Explicit content flagged (Detected word: '$word')")
                }
            }
        }
        return ModerationResult(isSafe = true, reason = null)
    }

    private fun getFallbackResponse(partnerName: String): String {
        val responses = listOf(
            "Hey, sorry my connection glitched a bit! What are you doing right now?",
            "That's pretty cool! Tell me more about yourself.",
            "Haha nice. I'm just listening to music and relaxing. Where are you from?",
            "Wow really? That. is so interesting! Are you single?",
            "Yeah I agree. By the way, how was your day?",
            "Cool cool! Let's play a truth or dare game, what do you say?"
        )
        return responses.random()
    }
}

data class ModerationResult(
    val isSafe: Boolean,
    val reason: String?
)
