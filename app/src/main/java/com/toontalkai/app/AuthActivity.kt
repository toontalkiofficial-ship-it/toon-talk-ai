package com.toontalkai.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors

class AuthActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var status: TextView
    private lateinit var signInButton: Button
    private lateinit var signUpButton: Button
    private lateinit var resetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(23, 20, 43)
        window.navigationBarColor = Color.rgb(23, 20, 43)
        if (SupabaseAuth.hasSession(this)) { openGenerator(); return }
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(Color.rgb(247, 246, 252))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(TextView(this).apply {
            text = "Toon Talk AI"; textSize = 30f
            setTextColor(Color.rgb(32, 26, 63)); typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        content.addView(TextView(this).apply {
            text = "Create your account to continue"; textSize = 15f
            setTextColor(Color.rgb(91, 87, 110))
        }, params(top = 4, bottom = 20))
        if (!SupabaseAuth.configured()) {
            content.addView(TextView(this).apply {
                text = "Supabase setup is not finished. Add SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY in GitHub Actions secrets, then rebuild this app."
                textSize = 14f; setTextColor(Color.rgb(150, 65, 25))
            }, params(bottom = 16))
        }
        email = EditText(this).apply {
            hint = "Email address"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setTextColor(Color.BLACK); setBackgroundColor(Color.WHITE)
        }
        password = EditText(this).apply {
            hint = "Password (at least 6 characters)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(Color.BLACK); setBackgroundColor(Color.WHITE)
        }
        content.addView(email, params(bottom = 10))
        content.addView(password, params(bottom = 12))
        signInButton = button("Sign in") {
            runAuth { address, pass -> SupabaseAuth.signIn(this, address, pass); "Signed in successfully." }
        }
        signUpButton = button("Create account") {
            runAuth { address, pass -> SupabaseAuth.signUp(this, address, pass) }
        }
        resetButton = button("Reset password") {
            val address = email.text.toString().trim()
            if (address.isBlank() || !address.contains("@")) showStatus("Enter your email address first.")
            else runAuth(requirePassword = false) { addressValue, _ ->
                SupabaseAuth.resetPassword(this, addressValue)
                "If the account exists, a password reset email has been requested."
            }
        }
        content.addView(signInButton, params(bottom = 6))
        content.addView(signUpButton, params(bottom = 6))
        content.addView(resetButton, params(bottom = 14))
        status = TextView(this).apply {
            text = if (SupabaseAuth.configured()) "Sign in or create a new account." else "Waiting for project configuration."
            textSize = 14f; setTextColor(Color.rgb(66, 61, 84))
        }
        content.addView(status)
        scroll.addView(content); root.addView(scroll); setContentView(root)
        val enabled = SupabaseAuth.configured()
        signInButton.isEnabled = enabled; signUpButton.isEnabled = enabled; resetButton.isEnabled = enabled
    }

    private fun button(title: String, action: () -> Unit) = Button(this).apply {
        text = title; isAllCaps = false; setOnClickListener { action() }
    }

    private fun runAuth(requirePassword: Boolean = true, action: (String, String) -> String) {
        val address = email.text.toString().trim()
        val pass = password.text.toString()
        if (address.isBlank() || !address.contains("@")) { showStatus("Enter a valid email address."); return }
        if (requirePassword && pass.length < 6) { showStatus("Password must be at least 6 characters."); return }
        setBusy(true)
        executor.execute {
            try {
                val message = action(address, pass)
                runOnUiThread {
                    setBusy(false); showStatus(message)
                    if (SupabaseAuth.hasSession(this)) openGenerator()
                }
            } catch (e: Exception) {
                runOnUiThread { setBusy(false); showStatus(e.message ?: "Could not contact Supabase.") }
            }
        }
    }

    private fun setBusy(busy: Boolean) {
        val enabled = !busy && SupabaseAuth.configured()
        signInButton.isEnabled = enabled; signUpButton.isEnabled = enabled; resetButton.isEnabled = enabled
    }
    private fun showStatus(message: String) { status.text = message }
    private fun openGenerator() { startActivity(android.content.Intent(this, MainActivity::class.java)); finish() }
    private fun params(top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(top); bottomMargin = dp(bottom)
        }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }
}
