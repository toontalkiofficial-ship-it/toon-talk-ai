package com.toontalkai.app

import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal Supabase Auth REST client. Uses only the mobile-safe publishable key;
 * never put the Supabase service-role key in the Android app.
 */
class SupabaseAuthClient(
    private val projectUrl: String,
    private val publishableKey: String
) {
    data class AuthResult(
        val accessToken: String? = null,
        val refreshToken: String? = null,
        val email: String? = null,
        val message: String
    )

    fun signUp(email: String, password: String, displayName: String): AuthResult {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("data", JSONObject().put("display_name", displayName))
        val json = request("/auth/v1/signup", body, null)
        val session = json.optJSONObject("session")
        val user = json.optJSONObject("user") ?: json
        val access = session?.optString("access_token")?.takeIf { it.isNotBlank() }
        return AuthResult(
            accessToken = access,
            refreshToken = session?.optString("refresh_token")?.takeIf { it.isNotBlank() },
            email = user.optString("email").takeIf { it.isNotBlank() },
            message = if (access == null) {
                "Account request successful. Email confirmation link check karo, phir sign in karo."
            } else {
                "Account created aur sign in ho gaya."
            }
        )
    }

    fun signIn(email: String, password: String): AuthResult {
        val json = request("/auth/v1/token?grant_type=password", JSONObject()
            .put("email", email)
            .put("password", password), null)
        val access = json.optString("access_token").takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Login session nahi mila. Email confirm karke dobara try karo.")
        return AuthResult(
            accessToken = access,
            refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() },
            email = json.optJSONObject("user")?.optString("email"),
            message = "Sign in successful."
        )
    }

    fun refreshSession(refreshToken: String): AuthResult {
        val json = request("/auth/v1/token?grant_type=refresh_token",
            JSONObject().put("refresh_token", refreshToken), null)
        val access = json.optString("access_token").takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Session refresh nahi hua. Dobara sign in karo.")
        return AuthResult(
            accessToken = access,
            refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() },
            email = json.optJSONObject("user")?.optString("email"),
            message = "Session refreshed."
        )
    }

    fun signOut(accessToken: String) {
        request("/auth/v1/logout", JSONObject(), accessToken)
    }

    fun getMyProfile(accessToken: String): JSONObject =
        request("/functions/v1/me", null, accessToken)

    fun deleteAccount(accessToken: String) {
        request("/functions/v1/account-delete", JSONObject(), accessToken)
    }

    private fun request(path: String, body: JSONObject?, accessToken: String?): JSONObject {
        require(projectUrl.startsWith("https://")) { "Supabase Project URL app mein configured nahi hai." }
        require(publishableKey.isNotBlank()) { "Supabase publishable key app mein configured nahi hai." }
        val base = projectUrl.trimEnd('/')
        val conn = (URL(base + path).openConnection() as HttpURLConnection).apply {
            requestMethod = if (path.endsWith("/logout")) "POST" else if (path.startsWith("/functions/")) "GET" else "POST"
            connectTimeout = 20000
            readTimeout = 25000
            setRequestProperty("apikey", publishableKey)
            setRequestProperty("Accept", "application/json")
            if (accessToken != null) setRequestProperty("Authorization", "Bearer $accessToken")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            if (body != null) OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = try {
                    val error = JSONObject(text)
                    error.optString("msg").ifBlank {
                        error.optString("message").ifBlank {
                            error.optString("error_description").ifBlank { error.optString("error") }
                        }
                    }
                } catch (_: Exception) { "" }
                throw IllegalStateException(message.ifBlank { "Supabase request failed (HTTP $code)." })
            }
            return if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }
}
