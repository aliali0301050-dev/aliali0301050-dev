package com.example.ringtoneplayer.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.ringtoneplayer.R
import com.example.ringtoneplayer.RingtoneCutterActivity
import com.example.ringtoneplayer.databinding.DialogSongOptionsBinding
import com.example.ringtoneplayer.models.Song
import com.example.ringtoneplayer.services.MusicPlayerService
import com.example.ringtoneplayer.utils.AudioHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Locale

class DialogHelper(private val context: Context) {

    fun showSongOptions(
        song: Song,
        viewModel: MainViewModel,
        coverPickerLauncher: ActivityResultLauncher<String>,
        deleteLauncher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        val optionsDialog = BottomSheetDialog(context, R.style.CustomDialogTheme)
        val dialogBinding = DialogSongOptionsBinding.inflate(LayoutInflater.from(context))
        optionsDialog.setContentView(dialogBinding.root)

        dialogBinding.tvOptionTitle.text = song.title
        val sizeFormatted = String.format(Locale.getDefault(), "%.2f", song.size / (1024.0 * 1024.0))
        dialogBinding.tvOptionInfo.text = "${AudioHelper.formatTime(song.duration.toInt())} | $sizeFormatted MB"
        Glide.with(context).load(AudioHelper.getAlbumArtUri(song.albumId)).placeholder(R.drawable.ic_music_note).into(dialogBinding.ivMiniArt)

        dialogBinding.btnOptionFavorite.setImageResource(if (song.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border)
        dialogBinding.btnOptionFavorite.setOnClickListener {
            viewModel.toggleFavorite(song)
            dialogBinding.btnOptionFavorite.setImageResource(if (song.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border)
        }

        dialogBinding.btnOptionInfo.setOnClickListener { showDetailsDialog(song); optionsDialog.dismiss() }
        dialogBinding.optCutRingtone.setOnClickListener {
            val intent = Intent(context, RingtoneCutterActivity::class.java).apply { putExtra("song_path", song.path) }
            context.startActivity(intent); optionsDialog.dismiss()
        }
        dialogBinding.optEditCover.setOnClickListener { coverPickerLauncher.launch("image/*"); optionsDialog.dismiss() }
        dialogBinding.optRename.setOnClickListener { showRenameDialog(song); optionsDialog.dismiss() }
        dialogBinding.optShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply { 
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, song.uri) 
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share")); optionsDialog.dismiss()
        }
        dialogBinding.optDelete.setOnClickListener {
            AlertDialog.Builder(context, R.style.CustomDialogTheme)
                .setTitle("Delete")
                .setMessage("Are you sure you want to delete ${song.title}?")
                .setPositiveButton("OK") { _, _ -> performRealDeletion(song, deleteLauncher) }
                .setNegativeButton("Cancel", null).show()
            optionsDialog.dismiss()
        }
        optionsDialog.show()
    }

    private fun showDetailsDialog(song: Song) {
        val details = "Title: ${song.title}\nArtist: ${song.artist}\nPath: ${song.path}"
        AlertDialog.Builder(context, R.style.CustomDialogTheme).setTitle("Details").setMessage(details).setPositiveButton("OK", null).show()
    }

    private fun performRealDeletion(song: Song, deleteLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(song.uri))
            deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
        } else { 
            try { 
                context.contentResolver.delete(song.uri, null, null)
            } catch (e: SecurityException) {} 
        }
    }

    private fun showRenameDialog(song: Song) {
        val input = EditText(context).apply { setText(song.title) }
        AlertDialog.Builder(context, R.style.CustomDialogTheme).setTitle("Rename").setView(input)
            .setPositiveButton("Save") { _, _ -> Toast.makeText(context, "Renamed", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("Cancel", null).show()
    }

    fun showSleepTimerDialog(musicService: MusicPlayerService?) {
        val times = arrayOf("Off", "5 min", "15 min", "30 min", "60 min")
        val values = intArrayOf(0, 5, 15, 30, 60)
        AlertDialog.Builder(context, R.style.CustomDialogTheme)
            .setTitle("Sleep Timer")
            .setItems(times) { _, which -> musicService?.setSleepTimer(values[which]) }.create().show()
    }

    fun showSortDialog(viewModel: MainViewModel) {
        val options = arrayOf(
            context.getString(R.string.sort_name),
            context.getString(R.string.sort_date),
            context.getString(R.string.sort_size)
        )
        val keys = arrayOf("title", "date", "size")
        
        AlertDialog.Builder(context, R.style.CustomDialogTheme)
            .setTitle(R.string.sort)
            .setItems(options) { _, which ->
                viewModel.updateSort(keys[which], viewModel.isAscending)
            }
            .show()
    }
}
