package com.example.ringtoneplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.ringtoneplayer.utils.PlayerPreferences
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class BackupActivity : AppCompatActivity() {

    private lateinit var btnBackup: Button
    private lateinit var btnRestore: Button
    private lateinit var tvStatus: TextView
    private lateinit var preferences: PlayerPreferences

    // أبسط صيغة ممكنة لتجنب خطأ المحلل البرمجي K2
    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            performRestore(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)

        preferences = PlayerPreferences(this)
        initViews()

        btnBackup.setOnClickListener { performBackup() }
        btnRestore.setOnClickListener { importLauncher.launch("application/json") }
        
        updateLastBackupText()
    }

    private fun initViews() {
        btnBackup = findViewById(R.id.btnBackup)
        btnRestore = findViewById(R.id.btnRestore)
        tvStatus = findViewById(R.id.lastBackupStatus)
    }

    private fun updateLastBackupText() {
        val lastTime = preferences.getString("last_backup_time", "لم يتم الرفع بعد")
        tvStatus.text = "آخر مزامنة: $lastTime"
    }

    private fun performBackup() {
        val uri = preferences.exportBackup(this)
        if (uri != null) {
            val currentTime = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
            preferences.putString("last_backup_time", currentTime)
            updateLastBackupText()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "حفظ النسخة الاحتياطية في..."))
            showStyledSnackbar("تم إنشاء نسخة احتياطية بنجاح ✅")
        } else {
            Toast.makeText(this, "فشل إنشاء النسخة الاحتياطية", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performRestore(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                if (preferences.importBackup(jsonString)) {
                    showStyledSnackbar("تمت استعادة البيانات بنجاح 🔄")
                } else {
                    Toast.makeText(this, "الملف غير صالح", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في قراءة الملف", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showStyledSnackbar(message: String) {
        val view = findViewById<View>(android.R.id.content)
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
                .setTextColor(ContextCompat.getColor(this, android.R.color.white))
                .show()
        }
    }
}
