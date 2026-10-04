package com.toontalkai.app

import android.graphics.BitmapFactory
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
    private lateinit var promptInput: EditText
    private lateinit var status: TextView
    private lateinit var resultImage: ImageView
    private lateinit var resultVideo: VideoView
    private lateinit var progress: ProgressBar
    private lateinit var connectButton: Button
    private lateinit var imageButton: Button
    private lateinit var videoButton: Button
    private var adView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(23, 20, 43)
        window.navigationBarColor = Color.rgb(23, 20, 43)
        buildUi()
        apiKeyInput.setText(prefs.getString("pollinations_key", ""))
        connectButton.setOnClickListener {
            val key = apiKeyInput.text.toString().trim()
            if (!key.startsWith("sk_")) {
                setStatus("Pollinations API key chahiye. Pollinations account se apni authorized key paste karo.")
            } else {
                prefs.edit().putString("pollinations_key", key).apply()
                setStatus("Key is device par save ho gayi. Generate button se AI connection test karo.")
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
        body.addView(label("Pollinations connection"))
        apiKeyInput = EditText(this).apply {
            hint = "Paste your authorized sk_ API key"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            singleLine = true
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
        body.addView(connectButton, marginParams(top = 8, bottom = 18))
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
        val note = TextView(this).apply {
            text = "Note: AI generation can use your Pollinations balance. Ad space currently uses Google's test ad ID; real earnings require your own approved AdMob app and ad-unit IDs."
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

    private fun generateMedia(video: Boolean) {
        val key = apiKeyInput.text.toString().trim().ifBlank { prefs.getString("pollinations_key", "").orEmpty() }
        val prompt = promptInput.text.toString().trim()
        if (!key.startsWith("sk_")) {
            setStatus("Pehle authorized Pollinations API key paste karke Save / Connect AI dabao.")
            return
        }
        if (prompt.length < 3) {
            setStatus("Scene ka description likho, phir generate karo.")
            return
        }
        prefs.edit().putString("pollinations_key", key).apply()
        setBusy(true, if (video) "AI video ban raha hai. Ismein kuch minute lag sakte hain..." else "AI image ban rahi hai...")
        executor.execute {
            try {
                val encoded = URLEncoder.encode(prompt, "UTF-8").replace("+", "%20")
                val endpoint = if (video) {
                    "https://gen.pollinations.ai/video/$encoded?model=google%2Fveo-3.1-fast&duration=4"
                } else {
                    "https://gen.pollinations.ai/image/$encoded?model=flux&width=1024&height=1024&safe=true"
                }
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 30000
                    readTimeout = if (video) 240000 else 180000
                    setRequestProperty("Authorization", "Bearer $key")
                    setRequestProperty("Accept", if (video) "video/mp4, application/json, */*" else "image/*, application/json")
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
