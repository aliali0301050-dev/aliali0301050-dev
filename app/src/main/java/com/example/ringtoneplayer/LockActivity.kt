package com.example.ringtoneplayer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class LockActivity : AppCompatActivity() {
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)

        val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val isLockEnabled = prefs.getBoolean("app_lock_enabled", false)
        
        if (!isLockEnabled) {
            startMainActivity()
            return
        }

        setupBiometric()

        findViewById<Button>(R.id.btnRetryBiometric).setOnClickListener {
            checkAndShowBiometric()
        }

        checkAndShowBiometric()
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    startMainActivity()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@LockActivity, "فشل التحقق: $errString", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("تطبيق الأغاني مقفل")
            .setSubtitle("استخدم بصمة الإصبع للدخول")
            .setNegativeButtonText("خروج")
            .build()
    }

    private fun checkAndShowBiometric() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> biometricPrompt.authenticate(promptInfo)
            else -> {
                Toast.makeText(this, "البصمة غير متوفرة في جهازك", Toast.LENGTH_LONG).show()
                startMainActivity() // إذا لم يوجد بصمة في الجهاز، ندخل مباشرة
            }
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
