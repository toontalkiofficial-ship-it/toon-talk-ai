package com.toontalkai.app

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object SupabaseAuth {
    private const val PREFS = "toon_talk_private"

    fun configured(): Boolean =
        BuildConfig.SUPABASE_URL.startsWith("https://") &&
        BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    fun hasSession(context: Context): Boolean =
        !prefs(context).getString("supabase_access_token", null).isNullOrBlank()

    fun accessToken(context: Context): String =
        prefs(context).getString("supabase_access_token", "").orEmpty()

    fun signUp(context: Context, email: String, password: String): String {
        val json = request("/auth/v1/signup", JSONObject().put("email", email).put("password", password), null)
        if (json.optString("access_token").isNotBlank()) saveSession(context, json)
        return if (hasSession(context)) "Account created. You are signed in."
        else "Account created. Check your email to confirm it, then sign in."
    }

    fun signIn(context: Context, email: String, password: String) {
        val json = request("/auth/v1/token?grant_type=password",
            JSONObject().put("email", email).put("password", password), null)
        if (json.optString("access_token").isBlank()) {
            throw IllegalStateException("Login response did not contain a session. Confirm your email and try again.")
        }
        saveSession(context, json)
    }

    fun resetPassword(context: Context, email: String) {
        request("/auth/v1/recover", JSONObject().put("email", email), null)
    }

    fun signOut(context: Context) {
        val token = accessToken(context)
        if (token.isNotBlank() && configured()) {
            try { request("/auth/v1/logout", JSONObject(), token) } catch (_: Exception) { }
        }
        prefs(context).edit().remove("supabase_access_token").remove("supabase_refresh_token")
            .remove("supabase_user_email").apply()
    }

    fun userEmail(context: Context): String =
        prefs(context).getString("supabase_user_email", "").orEmpty()

    private fun saveSession(context: Context, json: JSONObject) {
        val user = json.optJSONObject("user")
        prefs(context).edit()
            .putString("supabase_access_token", json.optString("access_token"))
            .putString("supabase_refresh_token", json.optString("refresh_token"))
            .putString("supabase_user_email", user?.optString("email").orEmpty())
            .apply()
    }

    private fun request(path: String, body: JSONObject, bearer: String?): JSONObject {
        if (!configured()) {
            throw IllegalStateException("Supabase configuration missing. Set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY in GitHub Actions secrets and rebuild.")
        }
        val urlPath = if (path == "/auth/v1/recover") "$path?redirect_to=" + java.net.URLEncoder.encode(BuildConfig.SUPABASE_URL, "UTF-8") else path
        val connection = (URL(BuildConfig.SUPABASE_URL.trimEnd('/') + urlPath).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer ${bearer ?: BuildConfig.SUPABASE_PUBLISHABLE_KEY}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = try {
                    val error = JSONObject(text)
                    error.optString("msg").ifBlank { error.optString("message").ifBlank { error.optString("error_description").ifBlank { error.optString("error").ifBlank { text } } } }
                } catch (_: Exception) { text }
                throw IllegalStateException("Supabase ($code): ${message.take(220)}")
            }
            return if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
