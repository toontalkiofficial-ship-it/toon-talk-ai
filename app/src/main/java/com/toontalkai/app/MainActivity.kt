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
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("toon_talk_private", MODE_PRIVATE) }
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
    private lateinit var imageButton: Button
    private lateinit var videoButton: Button
    private var adView: AdView? = null
    private var generatedVideoFile: File? = null
    private lateinit var saveButton: Button
    private lateinit var creditBalanceText: TextView
    private lateinit var deleteAccountButton: Button
    private lateinit var privacyButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(23, 20, 43)
        window.navigationBarColor = Color.rgb(23, 20, 43)
        buildUi()
        val savedEmail = prefs.getString("supabase_email", null)
        authStatus.text = if (prefs.getString("supabase_access_token", null) != null) {
            "Signed in as ${savedEmail ?: "your account"}."
        } else {
            "Create an account or sign in to start generating."
        }
        refreshAccountState()
        signUpButton.setOnClickListener { performAuth(signUp = true) }
        signInButton.setOnClickListener { performAuth(signUp = false) }
        deleteAccountButton.setOnClickListener { confirmDeleteAccount() }
        privacyButton.setOnClickListener {
            try {
                startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://github.com/toontalkiofficial-ship-it/toon-talk-ai/blob/main/PRIVACY_POLICY.md")))
            } catch (_: Exception) { setStatus("Privacy policy link open nahi ho paya.") }
        }
        signOutButton.setOnClickListener {
            val token = prefs.getString("supabase_access_token", null)
            signOutButton.isEnabled = false
            executor.execute {
                var message = "Signed out on this device."
                try { if (token != null) supabaseAuth.signOut(token) } catch (e: Exception) {
                    message = "Local session cleared. Server sign-out: ${e.message ?: "unavailable"}"
                }
                prefs.edit().remove("supabase_access_token").remove("supabase_refresh_token").remove("supabase_email").apply()
                runOnUiThread {
                    signOutButton.isEnabled = true
                    authStatus.text = message
                    creditBalanceText.text = "Credits: —"
                    deleteAccountButton.isEnabled = false
                    imageButton.isEnabled = false
                    videoButton.isEnabled = false
                }
            }
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
        creditBalanceText = TextView(this).apply {
            text = "Credits: —"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(38, 31, 76))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.WHITE)
        }
        body.addView(creditBalanceText, marginParams(top = 4, bottom = 8))
        val accountActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        deleteAccountButton = Button(this).apply {
            text = "Delete account"
            isAllCaps = false
            isEnabled = false
        }
        privacyButton = Button(this).apply {
            text = "Privacy policy"
            isAllCaps = false
        }
        accountActions.addView(deleteAccountButton, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(4) })
        accountActions.addView(privacyButton, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(4) })
        body.addView(accountActions, marginParams(bottom = 18))
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
            text = "Sign in, choose a model, then create your first scene."
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
            text = "Credits are charged server-side. Model prices and availability can change. Ads use test IDs until production AdMob IDs are configured."
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
                runOnUiThread { authStatus.text = result.message; refreshAccountState() }
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
        val accessToken = prefs.getString("supabase_access_token", null)
        val prompt = promptInput.text.toString().trim()
        if (accessToken.isNullOrBlank()) { setStatus("Pehle apne account mein sign in karo."); return }
        if (prompt.length < 3) { setStatus("Scene ka description likho, phir generate karo."); return }
        if (prompt.length > 2000) { setStatus("Prompt 2000 characters se chhota rakho."); return }
        val selectedImageModelPosition = imageModelInput.selectedItemPosition
        val selectedVideoModelPosition = videoModelInput.selectedItemPosition
        generatedVideoFile = null
        saveButton.visibility = View.GONE
        resultImage.visibility = View.GONE
        resultVideo.visibility = View.GONE
        setBusy(true, if (video) "AI video ban raha hai. Thoda time lag sakta hai..." else "AI image ban rahi hai...")
        executor.execute {
            try {
                var requestAccessToken = accessToken
                val refreshToken = prefs.getString("supabase_refresh_token", null)
                if (!refreshToken.isNullOrBlank()) {
                    try {
                        val refreshed = supabaseAuth.refreshSession(refreshToken)
                        requestAccessToken = refreshed.accessToken
                        prefs.edit().putString("supabase_access_token", refreshed.accessToken)
                            .putString("supabase_refresh_token", refreshed.refreshToken ?: refreshToken)
                            .putString("supabase_email", refreshed.email ?: prefs.getString("supabase_email", ""))
                            .apply()
                    } catch (_: Exception) { }
                }
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
                val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/generate"
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 30000
                    readTimeout = 125000
                    setRequestProperty("Authorization", "Bearer $requestAccessToken")
                    setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                    setRequestProperty("Accept", if (video) "video/mp4, application/json, */*" else "image/*, application/json")
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                val requestBody = JSONObject().put("kind", if (video) "video" else "image")
                    .put("model", selectedModel).put("prompt", prompt)
                    .put("idempotencyKey", java.util.UUID.randomUUID().toString())
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(requestBody.toString()) }
                try {
                    val code = connection.responseCode
                    if (code !in 200..299) {
                        val errorText = (connection.errorStream ?: connection.inputStream).bufferedReader().use { it.readText() }
                        val safeMessage = try { JSONObject(errorText).optString("error") } catch (_: Exception) { "" }
                        val message = when {
                            code == 401 -> "Session expire ho gaya. Dobara sign in karo."
                            code == 402 || safeMessage == "insufficient_credits" -> "Credits kam hain. Generation ke liye credits chahiye."
                            code == 409 -> "Ye request already process ho rahi hai. Dobara submit mat karo."
                            code == 503 -> "AI backend abhi configure nahi hua. Thodi der baad try karo."
                            else -> "AI service error ($code). Dobara try karo."
                        }
                        if (code == 401) prefs.edit().remove("supabase_access_token").remove("supabase_refresh_token").apply()
                        throw IllegalStateException(message)
                    }
                    val contentType = connection.contentType.orEmpty()
                    if (video) {
                        val temp = File(cacheDir, "toon-talk-${System.currentTimeMillis()}.mp4")
                        if (contentType.contains("json", true)) {
                            val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                            val mediaUrl = extractMediaUrl(jsonText) ?: throw IllegalStateException("Video response invalid hai. Dobara try karo.")
                            downloadVideo(mediaUrl, temp)
                        } else {
                            connection.inputStream.use { input -> temp.outputStream().use { output -> input.copyTo(output) } }
                        }
                        if (temp.length() < 1024) throw IllegalStateException("Video file incomplete hai.")
                        runOnUiThread {
                            setBusy(false, "Video ready! Play button dabao.")
                            resultImage.visibility = View.GONE
                            resultVideo.visibility = View.VISIBLE
                            generatedVideoFile = temp
                            saveButton.text = "Save video to phone"
                            saveButton.visibility = View.VISIBLE
                            resultVideo.setVideoURI(Uri.fromFile(temp))
                            resultVideo.setOnPreparedListener { it.isLooping = true; resultVideo.start() }
                            refreshAccountState()
                        }
                    } else {
                        val bytes = connection.inputStream.use { it.readBytes() }
                        if (contentType.contains("json", true)) throw IllegalStateException("Image response invalid hai. Dobara try karo.")
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw IllegalStateException("Image file open nahi hui. Dobara try karo.")
                        runOnUiThread {
                            setBusy(false, "Image ready! Save button se phone mein save karo.")
                            resultVideo.visibility = View.GONE
                            generatedVideoFile = null
                            saveButton.text = "Save image to phone"
                            saveButton.visibility = View.VISIBLE
                            resultImage.visibility = View.VISIBLE
                            resultImage.setImageBitmap(bitmap)
                            refreshAccountState()
                        }
                    }
                } finally { connection.disconnect() }
            } catch (e: Exception) {
                runOnUiThread { setBusy(false, e.message ?: "Generation failed. Dobara try karo."); refreshAccountState() }
            }
        }
    }
    private fun refreshAccountState() {
        val token = prefs.getString("supabase_access_token", null)
        if (token.isNullOrBlank()) {
            creditBalanceText.text = "Credits: —"
            deleteAccountButton.isEnabled = false
            imageButton.isEnabled = false
            videoButton.isEnabled = false
            return
        }
        executor.execute {
            try {
                val profile = supabaseAuth.getMyProfile(token)
                val balance = profile.optLong("creditBalance", 0L)
                val displayName = profile.optJSONObject("profile")?.optString("display_name").orEmpty()
                runOnUiThread {
                    creditBalanceText.text = "Credits: $balance"
                    deleteAccountButton.isEnabled = true
                    imageButton.isEnabled = true
                    videoButton.isEnabled = true
                    if (displayName.isNotBlank()) authStatus.text = "Signed in as $displayName"
                }
            } catch (_: Exception) {
                runOnUiThread {
                    creditBalanceText.text = "Credits: unavailable"
                    deleteAccountButton.isEnabled = false
                    imageButton.isEnabled = false
                    videoButton.isEnabled = false
                    authStatus.text = "Account status load nahi hua. Dobara sign in karo."
                }
            }
        }
    }

    private fun confirmDeleteAccount() {
        val token = prefs.getString("supabase_access_token", null) ?: return
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete account?")
            .setMessage("Account, profile aur server-side generation/credit records delete ho jayenge. Ye action undo nahi kiya ja sakta.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteAccountButton.isEnabled = false
                setStatus("Account delete ho raha hai...")
                executor.execute {
                    try {
                        supabaseAuth.deleteAccount(token)
                        prefs.edit().clear().apply()
                        runOnUiThread {
                            authStatus.text = "Account delete ho gaya."
                            creditBalanceText.text = "Credits: —"
                            imageButton.isEnabled = false
                            videoButton.isEnabled = false
                            setStatus("Account delete complete.")
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            deleteAccountButton.isEnabled = true
                            setStatus("Account delete failed: " + (e.message ?: "server error"))
                        }
                    }
                }
            }
            .show()
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

    private fun downloadVideo(mediaUrl: String, destination: File) {
        val conn = (URL(mediaUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30000
            readTimeout = 240000
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
        val signedIn = !prefs.getString("supabase_access_token", null).isNullOrBlank()
        imageButton.isEnabled = !busy && signedIn
        videoButton.isEnabled = !busy && signedIn
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
