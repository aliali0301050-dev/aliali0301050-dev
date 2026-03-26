package com.example.ringtoneplayer

import android.content.ContentValues
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ringtoneplayer.databinding.ActivityRingtoneCutterBinding
import com.example.ringtoneplayer.utils.AudioHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.nio.ByteBuffer

class RingtoneCutterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRingtoneCutterBinding
    private var songPath: String? = null
    private var durationUs: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRingtoneCutterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = 0xFF121212.toInt()
        binding.root.setBackgroundColor(0xFF121212.toInt())

        songPath = intent.getStringExtra("SONG_PATH")
        val songTitle = intent.getStringExtra("SONG_TITLE")
        binding.tvSongName.text = songTitle

        setupCutter()

        binding.btnSaveCut.setOnClickListener {
            val values = binding.rangeSlider.values
            val startMs = values[0].toLong()
            val endMs = values[1].toLong()
            processAudioCut(startMs * 1000, endMs * 1000, songTitle ?: "NewRingtone")
        }
    }

    private fun setupCutter() {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(songPath!!)
            val format = extractor.getTrackFormat(0)
            durationUs = format.getLong(MediaFormat.KEY_DURATION)
            val durationMs = durationUs / 1000
            
            binding.rangeSlider.valueFrom = 0f
            binding.rangeSlider.valueTo = durationMs.toFloat()
            binding.rangeSlider.values = listOf(0f, durationMs.toFloat())
            
            binding.rangeSlider.addOnChangeListener { slider, _, _ ->
                val values = slider.values
                binding.tvStartTime.text = AudioHelper.formatPreciseTime(values[0].toLong())
                binding.tvEndTime.text = AudioHelper.formatPreciseTime(values[1].toLong())
            }
            
            binding.tvStartTime.text = AudioHelper.formatPreciseTime(0)
            binding.tvEndTime.text = AudioHelper.formatPreciseTime(durationMs)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "خطأ في تحميل ملف الصوت", Toast.LENGTH_SHORT).show()
        } finally {
            extractor.release()
        }
    }

    private fun processAudioCut(startUs: Long, endUs: Long, title: String) {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("جاري المعالجة")
            .setMessage("يتم الآن إنشاء نغمتك الجديدة...")
            .setCancelable(false)
            .show()

        val outputFile = File(externalCacheDir, "cut_${System.currentTimeMillis()}.mp3")

        Thread {
            try {
                val extractor = MediaExtractor()
                extractor.setDataSource(songPath!!)
                extractor.selectTrack(0)
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                val format = extractor.getTrackFormat(0)
                val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val outTrackIndex = muxer.addTrack(format)
                muxer.start()

                val buffer = ByteBuffer.allocate(1024 * 1024)
                val bufferInfo = MediaCodec.BufferInfo()

                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    val presentationTimeUs = extractor.sampleTime
                    if (presentationTimeUs > endUs) break

                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = presentationTimeUs - startUs
                    // Map extractor flags to codec flags to avoid lint error
                    bufferInfo.flags = if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                        MediaCodec.BUFFER_FLAG_KEY_FRAME
                    } else {
                        0
                    }
                    
                    muxer.writeSampleData(outTrackIndex, buffer, bufferInfo)
                    extractor.advance()
                }

                muxer.stop()
                muxer.release()
                extractor.release()

                runOnUiThread {
                    dialog.dismiss()
                    exportToSystemRingtones(outputFile, title)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    dialog.dismiss()
                    Toast.makeText(this, "فشل في معالجة الملف", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun exportToSystemRingtones(file: File, title: String) {
        val fileName = "${title}_${System.currentTimeMillis()}.mp3"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.TITLE, title)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.IS_RINGTONE, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { os ->
                    file.inputStream().use { it.copyTo(os) }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(uri, values, null, null)
                }
                
                MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), null, null)
                RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE, uri)
                
                Toast.makeText(this, "تم الحفظ بنجاح وتعيين النغمة!", Toast.LENGTH_LONG).show()
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "حدث خطأ أثناء الحفظ النهائي", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
