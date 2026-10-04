package com.toontalkai.app

import android.graphics.BitmapFactory
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("toon_talk_private", MODE_PRIVATE) }
    private lateinit var apiKeyInput: EditText
    private lateinit var authNameInput: EditText
    private lateinit var authEmailInput: EditText
    private lateinit var authPasswordInput: EditText
    private lateinit var authStatus: TextView
    private lateinit var signUpButton: Button
    private lateinit var signInButton: Button
    private lateinit var signOutButton: Button
    private val supabaseAuth by lazy { SupabaseAuthClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) }
    private lateinit var promptInput: EditText
    private lateinit var imageModelInput: Spinner
    private lateinit var videoModelInput: Spinner
    private lateinit var status: TextView
    private lateinit var resultImage: ImageView
    private lateinit var resultVideo: VideoView
    private lateinit var progress: ProgressBar
    private lateinit var connectButton: Button
    private lateinit var clearKeyButton: Button
    private lateinit var imageButton: Button
    private lateinit var videoButton: Button
    private var adView: AdView? = null
    private var generatedVideoFile: File? = null
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(23, 20, 43)
        window.navigationBarColor = Color.rgb(23, 20, 43)
        buildUi()
        apiKeyInput.setText(prefs.getString("pollinations_key", ""))
        val savedEmail = prefs.getString("supabase_email", null)
        authStatus.text = if (prefs.getString("supabase_access_token", null) != null) {
            "Signed in as ${savedEmail ?: "your account"} (saved session; sign in again if it expires)."
        } else {
            "Create an account or sign in to prepare your public profile."
        }
        signUpButton.setOnClickListener { performAuth(signUp = true) }
        signInButton.setOnClickListener { performAuth(signUp = false) }
        signOutButton.setOnClickListener {
            val token = prefs.getString("supabase_access_token", null)
            signOutButton.isEnabled = false
            executor.execute {
                var message = "Signed out on this device."
                try { if (token != null) supabaseAuth.signOut(token) } catch (e: Exception) {
                    message = "Local session cleared. Server sign-out: ${e.message ?: "unavailable"}"
                }
                prefs.edit().remove("supabase_access_token").remove("supabase_refresh_token")
                    .remove("supabase_email").apply()
                runOnUiThread {
                    signOutButton.isEnabled = true
                    authStatus.text = message
                }
            }
        }
        connectButton.setOnClickListener {
            val key = apiKeyInput.text.toString().trim()
            if (!key.startsWith("sk_")) {
                setStatus("Pollinations API key chahiye. Pollinations account se apni authorized key paste karo.")
            } else {
                prefs.edit().putString("pollinations_key", key).apply()
                setStatus("Key is device par save ho gayi. Generate button se AI connection test karo.")
            }
        }
        clearKeyButton.setOnClickListener {
            prefs.edit().remove("pollinations_key").apply()
            apiKeyInput.text.clear()
            setStatus("Saved API key is device se remove kar di gayi.")
        }
        imageButton.setOnClickListener { generateMedia(video = false) }
        videoButton.setOnClickListener { generateMedia(video = true) }
        try {
            MobileAds.initialize(this) {}
            adView = AdView(this).apply {
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                setAdSize(AdSize.BANNER)
                loadAd(AdRequest.Builder().build())
            }
            findViewById<LinearLayout>(R.id.adContainer).addView(
                adView,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )
        } catch (_: Exception) {
            setStatus("Ads abhi load nahi hue. Internet connection check karo.")
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 246, 252))
        }
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(18))
        }
        val title = TextView(this).apply {
            text = "Toon Talk AI"
            textSize = 29f
            setTextColor(Color.rgb(32, 26, 63))
            typeface = Typeface.DEFAULT_BOLD
        }
        val subtitle = TextView(this).apply {
            text = "Create cartoon images and short AI videos"
            textSize = 14f
            setTextColor(Color.rgb(91, 87, 110))
        }
        body.addView(title)
        body.addView(subtitle, marginParams(top = 4, bottom = 18))

        body.addView(label("Public account — Supabase"))
        authNameInput = EditText(this).apply {
            hint = "Display name (for new account)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setTextColor(Color.BLACK); setHintTextColor(Color.GRAY); setBackgroundColor(Color.WHITE)
        }
        body.addView(authNameInput, marginParams(top = 6))
        authEmailInput = EditText(this).apply {
            hint = "Email address"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setTextColor(Color.BLACK); setHintTextColor(Color.GRAY); setBackgroundColor(Color.WHITE)
        }
        body.addView(authEmailInput, marginParams(top = 6))
        authPasswordInput = EditText(this).apply {
            hint = "Password (at least 6 characters)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(Color.BLACK); setHintTextColor(Color.GRAY); setBackgroundColor(Color.WHITE)
        }
        body.addView(authPasswordInput, marginParams(top = 6))
        val authButtons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        signUpButton = Button(this).apply { text = "Create account"; isAllCaps = false }
        signInButton = Button(this).apply { text = "Sign in"; isAllCaps = false }
        authButtons.addView(signUpButton, LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginEnd = dp(4) })
        authButtons.addView(signInButton, LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginStart = dp(4) })
        body.addView(authButtons, marginParams(top = 6))
        signOutButton = Button(this).apply { text = "Sign out"; isAllCaps = false }
        body.addView(signOutButton, marginParams(top = 2))
        authStatus = TextView(this).apply {
            text = "Supabase account setup"
            textSize = 13f
            setTextColor(Color.rgb(66, 61, 84))
        }
        body.addView(authStatus, marginParams(top = 4, bottom = 18))
        body.addView(label("Pollinations connection"))
        apiKeyInput = EditText(this).apply {
            hint = "Paste your authorized sk_ API key"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.WHITE)
        }
        body.addView(apiKeyInput, marginParams(top = 6))
        connectButton = Button(this).apply {
            text = "Save API Key"
            isAllCaps = false
        }
        body.addView(connectButton, marginParams(top = 8))
        clearKeyButton = Button(this).apply {
            text = "Remove saved API key"
            isAllCaps = false
        }
        body.addView(clearKeyButton, marginParams(top = 2, bottom = 18))
        body.addView(label("Image quality / model"))
        imageModelInput = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    "Budget — FLUX Schnell",
                    "Balanced — FLUX",
                    "Premium — FLUX.2 Pro",
                    "Premium — GPT Image"
                )
            )
        }
        body.addView(imageModelInput, marginParams(top = 4, bottom = 12))
        body.addView(label("Video quality / model"))
        videoModelInput = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    "Budget — Seedance 1 Pro Fast",
                    "Balanced — Veo 3.1 Fast",
                    "Premium — Seedance 2.0"
                )
            )
        }
        body.addView(videoModelInput, marginParams(top = 4, bottom = 12))
        body.addView(label("Describe your scene"))
        promptInput = EditText(this).apply {
            hint = "Example: Debu, a young village boy in a blue shirt, cinematic 3D cartoon style..."
            minLines = 3
            maxLines = 5
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.WHITE)
        }
        body.addView(promptInput, marginParams(top = 6, bottom = 12))
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        imageButton = Button(this).apply { text = "Generate Image"; isAllCaps = false }
        videoButton = Button(this).apply { text = "Generate Video"; isAllCaps = false }
        buttons.addView(imageButton, LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginEnd = dp(5) })
        buttons.addView(videoButton, LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginStart = dp(5) })
        body.addView(buttons)
        progress = ProgressBar(this).apply { visibility = View.GONE }
        body.addView(progress, marginParams(top = 14))
        status = TextView(this).apply {
            text = "Connect AI first, then create your first scene."
            textSize = 14f
            setTextColor(Color.rgb(66, 61, 84))
        }
        body.addView(status, marginParams(top = 10, bottom = 12))
        resultImage = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = View.GONE
            setBackgroundColor(Color.WHITE)
        }
        body.addView(resultImage, marginParams(bottom = 12))
        resultVideo = VideoView(this).apply { visibility = View.GONE }
        body.addView(resultVideo, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(240)).apply {
            bottomMargin = dp(12)
        })
        saveButton = Button(this).apply {
            text = "Save generated media to phone"
            isAllCaps = false
            visibility = View.GONE
        }
        body.addView(saveButton, marginParams(bottom = 12))
        saveButton.setOnClickListener { saveGeneratedMedia() }
        val note = TextView(this).apply {
            text = "Model cost and availability can change. Check Pollinations model pricing before generating; premium models may use more balance. Ads currently use Google test IDs and do not earn revenue."
            textSize = 12f
            setTextColor(Color.rgb(105, 99, 125))
        }
        body.addView(note, marginParams(top = 8, bottom = 12))
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        val adContainer = LinearLayout(this).apply {
            id = R.id.adContainer
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
        }
        root.addView(adContainer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(root)
    }

    private fun performAuth(signUp: Boolean) {
        val email = authEmailInput.text.toString().trim()
        val password = authPasswordInput.text.toString()
        val displayName = authNameInput.text.toString().trim()
        if (!email.contains("@") || email.length < 5) {
            authStatus.text = "Valid email address likho."
            return
        }
        if (password.length < 6) {
            authStatus.text = "Password kam se kam 6 characters ka hona chahiye."
            return
        }
        if (signUp && displayName.isBlank()) {
            authStatus.text = "Account ke liye display name likho."
            return
        }
        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_PUBLISHABLE_KEY.isBlank()) {
            authStatus.text = "Supabase app config missing hai. Build workflow mein SUPABASE_URL aur SUPABASE_PUBLISHABLE_KEY set karo."
            return
        }
        signUpButton.isEnabled = false
        signInButton.isEnabled = false
        authStatus.text = if (signUp) "Account create ho raha hai..." else "Sign in ho raha hai..."
        executor.execute {
            try {
                val result = if (signUp) supabaseAuth.signUp(email, password, displayName)
                    else supabaseAuth.signIn(email, password)
                if (result.accessToken != null) {
                    prefs.edit()
                        .putString("supabase_access_token", result.accessToken)
                        .putString("supabase_refresh_token", result.refreshToken)
                        .putString("supabase_email", result.email ?: email)
                        .apply()
                }
                runOnUiThread { authStatus.text = result.message }
            } catch (e: Exception) {
                runOnUiThread { authStatus.text = "Account error: ${e.message ?: "network error"}" }
            } finally {
                runOnUiThread {
                    signUpButton.isEnabled = true
                    signInButton.isEnabled = true
                }
            }
        }
    }

    private fun generateMedia(video: Boolean) {
        val key = apiKeyInput.text.toString().trim().ifBlank { prefs.getString("pollinations_key", "").orEmpty() }
        val accessToken = prefs.getString("supabase_access_token", null)
        val useBackend = !accessToken.isNullOrBlank()
        val prompt = promptInput.text.toString().trim()
        if (!useBackend && !key.startsWith("sk_")) {
            setStatus("Public account se generate karne ke liye pehle sign in karo. Personal mode mein authorized Pollinations API key bhi use kar sakte ho.")
            return
        }
        if (prompt.length < 3) {
            setStatus("Scene ka description likho, phir generate karo.")
            return
        }
        if (!useBackend) prefs.edit().putString("pollinations_key", key).apply()
        val selectedImageModelPosition = imageModelInput.selectedItemPosition
        val selectedVideoModelPosition = videoModelInput.selectedItemPosition
        // Prevent saving stale output if the next generation fails.
        generatedVideoFile = null
        saveButton.visibility = View.GONE
        resultImage.visibility = View.GONE
        resultVideo.visibility = View.GONE
        setBusy(true, if (video) "AI video ban raha hai. Ismein kuch minute lag sakte hain..." else "AI image ban rahi hai...")
        executor.execute {
            try {
                var requestAccessToken = accessToken
                if (useBackend) {
                    val refreshToken = prefs.getString("supabase_refresh_token", null)
                    if (!refreshToken.isNullOrBlank()) {
                        val refreshed = supabaseAuth.refreshSession(refreshToken)
                        requestAccessToken = refreshed.accessToken
                        prefs.edit()
                            .putString("supabase_access_token", refreshed.accessToken)
                            .putString("supabase_refresh_token", refreshed.refreshToken ?: refreshToken)
                            .putString("supabase_email", refreshed.email ?: prefs.getString("supabase_email", ""))
                            .apply()
                    }
                }
                val encoded = URLEncoder.encode(prompt, "UTF-8").replace("+", "%20")
                val imageModel = when (selectedImageModelPosition) {
                    0 -> "black-forest-labs/flux.1-schnell"
                    1 -> "flux"
                    2 -> "black-forest-labs/flux.2-pro"
                    else -> "openai/gpt-image-1.5"
                }
                val videoModel = when (selectedVideoModelPosition) {
                    0 -> "bytedance/seedance-1-pro-fast"
                    1 -> "google/veo-3.1-fast"
                    else -> "bytedance/seedance-2.0"
                }
                val selectedModel = if (video) videoModel else imageModel
                val endpoint = if (useBackend) {
                    BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/generate"
                } else if (video) {
                    "https://gen.pollinations.ai/video/$encoded?model=${URLEncoder.encode(videoModel, "UTF-8")}&duration=4"
                } else {
                    "https://gen.pollinations.ai/image/$encoded?model=${URLEncoder.encode(imageModel, "UTF-8")}&width=1024&height=1024&safe=true"
                }
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = if (useBackend) "POST" else "GET"
                    connectTimeout = 30000
                    readTimeout = if (useBackend) 125000 else if (video) 240000 else 180000
                    setRequestProperty("Authorization", "Bearer ${if (useBackend) requestAccessToken else key}")
                    setRequestProperty("Accept", if (video) "video/mp4, application/json, */*" else "image/*, application/json")
                    if (useBackend) {
                        setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                    }
                }
                if (useBackend) {
                    val requestBody = JSONObject()
                        .put("kind", if (video) "video" else "image")
                        .put("model", selectedModel)
                        .put("prompt", prompt)
                        .put("idempotencyKey", java.util.UUID.randomUUID().toString())
                    connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(requestBody.toString()) }
                }
                try {
                    val code = connection.responseCode
                    if (code !in 200..299) {
                        val errorText = (connection.errorStream ?: connection.inputStream).bufferedReader().use { it.readText() }
                        throw IllegalStateException("AI service error ($code): ${errorText.take(240)}")
                    }
                    val contentType = connection.contentType.orEmpty()
                    if (video) {
                        val temp = File(cacheDir, "toon-talk-${System.currentTimeMillis()}.mp4")
                        if (contentType.contains("json", true)) {
                            val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                            val mediaUrl = extractMediaUrl(jsonText)
                                ?: throw IllegalStateException("Video service ne MP4 URL return nahi kiya: ${jsonText.take(180)}")
                            downloadVideo(mediaUrl, key, temp)
                        } else {
                            connection.inputStream.use { input -> temp.outputStream().use { output -> input.copyTo(output) } }
                        }
                        runOnUiThread {
                            setBusy(false, "Video ready! Play button dabao.")
                            resultImage.visibility = View.GONE
                            resultVideo.visibility = View.VISIBLE
                            generatedVideoFile = temp
                            saveButton.text = "Save video to phone"
                            saveButton.visibility = View.VISIBLE
                            resultVideo.setVideoURI(Uri.fromFile(temp))
                            resultVideo.setOnPreparedListener { it.isLooping = true; resultVideo.start() }
                        }
                    } else {
                        val bytes = connection.inputStream.use { it.readBytes() }
                        if (contentType.contains("json", true)) {
                            val text = String(bytes, Charsets.UTF_8)
                            throw IllegalStateException("Image service ne image ke bajay response diya: ${text.take(180)}")
                        }
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            ?: throw IllegalStateException("Image file open nahi hui. Dobara try karo.")
                        runOnUiThread {
                            setBusy(false, "Image ready! Long press karke save/share kar sakte ho.")
                            resultVideo.visibility = View.GONE
                            generatedVideoFile = null
                            saveButton.text = "Save image to phone"
                            saveButton.visibility = View.VISIBLE
                            resultImage.visibility = View.VISIBLE
                            resultImage.setImageBitmap(bitmap)
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    setBusy(false, "Generation failed: ${e.message ?: "network error"}. Key, balance aur internet check karo.")
                }
            }
        }
    }

    private fun saveGeneratedMedia() {
        try {
            val videoFile = generatedVideoFile
            if (videoFile != null && videoFile.exists()) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "ToonTalkAI_${System.currentTimeMillis()}.mp4")
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/ToonTalkAI")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Phone storage mein video save nahi ho paya.")
                contentResolver.openOutputStream(uri)?.use { output ->
                    videoFile.inputStream().use { input -> input.copyTo(output) }
                } ?: throw IllegalStateException("Video file open nahi ho payi.")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val completed = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                    contentResolver.update(uri, completed, null, null)
                }
                setStatus("Video phone ke Movies/ToonTalkAI folder mein save ho gaya.")
                return
            }

            if (resultImage.visibility != View.VISIBLE) {
                throw IllegalStateException("Pehle image generate karo.")
            }
            val drawable = resultImage.drawable ?: throw IllegalStateException("Pehle image generate karo.")
            val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                ?: throw IllegalStateException("Image save nahi ho payi. Dobara generate karo.")
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "ToonTalkAI_${System.currentTimeMillis()}.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/ToonTalkAI")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Phone storage mein image save nahi ho payi.")
            contentResolver.openOutputStream(uri)?.use { output ->
                if (!bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)) {
                    throw IllegalStateException("Image write nahi ho payi.")
                }
            } ?: throw IllegalStateException("Image file open nahi ho payi.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val completed = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                contentResolver.update(uri, completed, null, null)
            }
            setStatus("Image phone ke Pictures/ToonTalkAI folder mein save ho gayi.")
        } catch (e: Exception) {
            setStatus("Save failed: ${e.message ?: "phone storage error"}")
        }
    }

    private fun downloadVideo(mediaUrl: String, key: String, destination: File) {
        val conn = (URL(mediaUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30000
            readTimeout = 240000
            if (mediaUrl.startsWith("https://gen.pollinations.ai/")) setRequestProperty("Authorization", "Bearer $key")
        }
        try {
            if (conn.responseCode !in 200..299) throw IllegalStateException("Video download error: HTTP ${conn.responseCode}")
            conn.inputStream.use { input -> destination.outputStream().use { output -> input.copyTo(output) } }
            if (destination.length() < 1024) throw IllegalStateException("Video file incomplete hai.")
        } finally {
            conn.disconnect()
        }
    }

    private fun extractMediaUrl(jsonText: String): String? = try {
        val root = JSONObject(jsonText)
        root.optString("url").takeIf { it.startsWith("https://") }
            ?: root.optJSONArray("data")?.optJSONObject(0)?.optString("url")?.takeIf { it.startsWith("https://") }
    } catch (_: Exception) { null }

    private fun setBusy(busy: Boolean, message: String) {
        progress.visibility = if (busy) View.VISIBLE else View.GONE
        imageButton.isEnabled = !busy
        videoButton.isEnabled = !busy
        connectButton.isEnabled = !busy
        status.text = message
    }

    private fun setStatus(message: String) { status.text = message }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(43, 36, 77))
    }

    private fun marginParams(top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(top); bottomMargin = dp(bottom)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        adView?.destroy()
        executor.shutdownNow()
        super.onDestroy()
    }
}
