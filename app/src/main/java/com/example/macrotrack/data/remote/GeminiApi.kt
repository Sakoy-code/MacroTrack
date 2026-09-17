package com.example.macrotrack.data.remote

import android.graphics.Bitmap
import android.util.Base64
import com.example.macrotrack.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class MealAnalysisResult(
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int
)

class GeminiAnalysisException(message: String) : Exception(message)

/**
 * Appelle directement l'API REST Gemini (generateContent) avec une image
 * (Bitmap encode en base64) + une description texte, et force une reponse
 * JSON stricte que l'on parse ensuite en objet Kotlin.
 *
 * On utilise l'API REST brute (plutot que le SDK Google AI) pour garder un
 * controle total sur le prompt systeme et le parsing, comme demande.
 */
class GeminiRepository(private val apiKeyProvider: () -> String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val systemPrompt = """
        Tu es un nutritionniste expert. Analyse la photo de ce plat et prends en
        compte la description de l'utilisateur. Calcule les calories et les
        macronutriments avec le plus de precision possible.
        Tu DOIS repondre UNIQUEMENT au format JSON strict suivant, sans aucun
        autre texte, sans balises markdown, sans phrase d'introduction :
        {"calories": 450, "proteines": 35, "glucides": 40, "lipides": 15}
    """.trimIndent()

    suspend fun analyzeMeal(photo: Bitmap, description: String, model: String): MealAnalysisResult =
        withContext(Dispatchers.IO) {
            val apiKey = apiKeyProvider()
            if (apiKey.isBlank() || apiKey == "COLLE_TA_CLE_API_ICI") {
                throw GeminiAnalysisException(
                    "Aucune cle API Gemini configuree. Ajoute-la dans gradle.properties (GEMINI_API_KEY)."
                )
            }

            val base64Image = bitmapToBase64(photo)
            val userText = if (description.isBlank()) {
                "Pas de description fournie par l'utilisateur."
            } else {
                "Description de l'utilisateur : $description"
            }

            val requestBody = buildRequestJson(userText, base64Image)

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw GeminiAnalysisException("Erreur API Gemini (${response.code}) : $rawBody")
                }
                parseGeminiResponse(rawBody)
            }
        }

    private fun buildRequestJson(userText: String, base64Image: String): JSONObject {
        val textPart = JSONObject().put("text", userText)
        val imagePart = JSONObject().put(
            "inline_data",
            JSONObject().put("mime_type", "image/jpeg").put("data", base64Image)
        )
        val userContent = JSONObject()
            .put("role", "user")
            .put("parts", org.json.JSONArray().put(textPart).put(imagePart))

        val systemInstruction = JSONObject().put(
            "parts",
            org.json.JSONArray().put(JSONObject().put("text", systemPrompt))
        )

        return JSONObject()
            .put("system_instruction", systemInstruction)
            .put("contents", org.json.JSONArray().put(userContent))
            .put("generationConfig", JSONObject().put("temperature", 0.2))
    }

    private fun parseGeminiResponse(rawBody: String): MealAnalysisResult {
        val text = try {
            JSONObject(rawBody)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        } catch (e: Exception) {
            throw GeminiAnalysisException("Reponse Gemini illisible : $rawBody")
        }

        // Gemini repond parfois quand meme avec des balises ```json ... ``` : on nettoie.
        val cleaned = text.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        return try {
            val obj = JSONObject(cleaned)
            MealAnalysisResult(
                calories = obj.getDouble("calories").toInt(),
                proteinG = obj.getDouble("proteines").toInt(),
                carbsG = obj.getDouble("glucides").toInt(),
                fatG = obj.getDouble("lipides").toInt()
            )
        } catch (e: Exception) {
            throw GeminiAnalysisException("Impossible de lire les macros renvoyees par Gemini : $cleaned")
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    companion object {
        fun default(): GeminiRepository = GeminiRepository { BuildConfig.GEMINI_API_KEY }
    }
}
