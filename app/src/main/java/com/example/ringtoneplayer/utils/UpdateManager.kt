package com.example.ringtoneplayer.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class UpdateManager(private val context: Context) {

    // [FIXED] استخدام الرابط الدائم (بدون Hash) لضمان قراءة التحديثات الجديدة دائماً
    private val UPDATE_URL = "https://gist.githubusercontent.com/aliali0301050-dev/9ad3a81529d5972b3659632d65458a67/raw/update.json"

    suspend fun checkForUpdates() {
        try {
            val result = withContext(Dispatchers.IO) {
                URL(UPDATE_URL).readText()
            }
            val json = JSONObject(result)
            val latestVersion = json.getString("version_name")
            val downloadUrl = json.getString("download_url")
            val releaseNotes = json.optString("notes", "إصدار جديد متوفر بلمسات عصرية وتحسينات شاملة.")

            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersion = pInfo.versionName ?: "1.0.0"

            if (isNewerVersion(latestVersion, currentVersion)) {
                withContext(Dispatchers.Main) {
                    showUpdateDialog(latestVersion, releaseNotes, downloadUrl)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val l = latest.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        
        for (i in 0 until minOf(l.size, c.size)) {
            if (l[i] > c[i]) return true
            if (l[i] < c[i]) return false
        }
        return l.size > c.size
    }

    private fun showUpdateDialog(version: String, notes: String, url: String) {
        MaterialAlertDialogBuilder(context)
            .setTitle("تحديث جديد متوفر ($version)")
            .setMessage(notes)
            .setCancelable(false)
            .setPositiveButton("تحديث الآن") { _, _ ->
                try {
                    // فتح الرابط المباشر في المتصفح للبدء الفوري بالتحميل
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(browserIntent)
                }
            }
            .setNegativeButton("لاحقاً", null)
            .show()
    }
}
