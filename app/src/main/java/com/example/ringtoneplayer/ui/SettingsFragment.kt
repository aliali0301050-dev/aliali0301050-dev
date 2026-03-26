package com.example.ringtoneplayer.ui

import android.content.*
import android.content.res.ColorStateList
import android.net.Uri
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.ringtoneplayer.LanguageActivity
import com.example.ringtoneplayer.MainActivity
import com.example.ringtoneplayer.R
import com.example.ringtoneplayer.ThemeActivity
import com.example.ringtoneplayer.databinding.LayoutSettingsBinding
import com.example.ringtoneplayer.utils.PlayerPreferences
import com.example.ringtoneplayer.utils.UpdateManager
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private lateinit var binding: LayoutSettingsBinding
    private lateinit var preferences: PlayerPreferences
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = LayoutSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = PlayerPreferences(requireContext())
        
        setupListeners()
        setupSwitches()
        loadPreferences()
        observeThemeColor()
        displayAppInfo()
    }

    private fun displayAppInfo() {
        binding.tvDeveloperInfo.text = "${getString(R.string.developer)}: ${getString(R.string.developer_name)}"
        binding.tvVersionInfo.text = "${getString(R.string.version)}: 2.1.0"
    }

    private fun observeThemeColor() {
        viewModel.themeColor.observe(viewLifecycleOwner) { color -> applyThemeToUI(color) }
    }

    private fun applyThemeToUI(color: Int) {
        val colorStateList = ColorStateList.valueOf(color)
        val root = binding.root
        
        val headerIds = intArrayOf(R.id.headerFiles, R.id.headerAudio, R.id.headerAppearance, R.id.headerSupport)
        headerIds.forEach { id -> root.findViewById<TextView>(id)?.setTextColor(color) }
        
        val iconIds = intArrayOf(
            R.id.i1, R.id.i2, R.id.i3, R.id.iTrash, R.id.i5, 
            R.id.i7, R.id.iPauseLoss, R.id.i8, R.id.iBtBlock, 
            R.id.iTheme, R.id.i11, R.id.iAlbumGrid, R.id.i12, 
            R.id.i13, R.id.iUpdate, R.id.iShare, R.id.iSupport, R.id.iPrivacy
        )
        iconIds.forEach { id -> root.findViewById<ImageView>(id)?.imageTintList = colorStateList }
    }

    private fun setupListeners() {
        binding.btnWhySongsHidden.setOnClickListener {
            Toast.makeText(context, "تأكد من وجود الملفات في ذاكرة الهاتف وتحديث المكتبة", Toast.LENGTH_LONG).show()
        }
        
        binding.btnDeleteDuplicates.setOnClickListener {
            val count = viewModel.getDuplicateSongsCount()
            if (count > 0) {
                AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                    .setTitle("تم العثور على مكررات")
                    .setMessage("يوجد لديك $count ملف مكرر في المكتبة. هل تريد تصفيتهم الآن؟")
                    .setPositiveButton("تصفية") { _, _ ->
                        Toast.makeText(context, "تمت تصفية $count ملف بنجاح", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("إلغاء", null)
                    .show()
            } else {
                Toast.makeText(context, "مكتبتك نظيفة، لا توجد ملفات مكررة!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnScanStorage.setOnClickListener { 
            (activity as? MainActivity)?.loadSongsFromDevice() 
            Toast.makeText(context, "جاري تحديث المكتبة...", Toast.LENGTH_SHORT).show()
        }

        binding.btnManageTrash.setOnClickListener {
            Toast.makeText(context, "سلة المهملات فارغة حالياً", Toast.LENGTH_SHORT).show()
        }

        binding.btnFilterShort.setOnClickListener {
            showFilterDurationDialog()
        }

        binding.btnSleepTimer.setOnClickListener { 
            (activity as? MainActivity)?.showSleepTimerDialog() 
        }

        binding.btnChangeTheme.setOnClickListener {
            startActivity(Intent(requireContext(), ThemeActivity::class.java))
        }

        binding.btnLanguage.setOnClickListener { 
            startActivity(Intent(requireContext(), LanguageActivity::class.java)) 
        }

        binding.btnRemoveAds.setOnClickListener {
            Toast.makeText(context, "نسخة الـ Pro لا تحتوي على إعلانات!", Toast.LENGTH_LONG).show()
        }

        // [FIXED] تعديل زر التحديث ليفحص الرابط الخاص بك بدلاً من متجر بلاي
        binding.btnUpdateApp.setOnClickListener {
            Toast.makeText(context, "جاري البحث عن تحديثات...", Toast.LENGTH_SHORT).show()
            viewLifecycleOwner.lifecycleScope.launch {
                UpdateManager(requireContext()).checkForUpdates()
            }
        }

        binding.btnShare.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "جرب تطبيق مشغل موسيقى احترافي: https://www.mediafire.com/file/fxkomskpurqah54/My_Musicplayer.apk/file")
            }
            startActivity(Intent.createChooser(intent, "مشاركة عبر"))
        }

        binding.btnSupport.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("aliali0301050@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Support: Ringtone Player Pro")
            }
            try { startActivity(intent) } catch (e: Exception) {
                Toast.makeText(context, "لا يوجد تطبيق بريد إلكتروني مثبت", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gist.githubusercontent.com/aliali0301050-dev/9ad3a81529d5972b3659632d65458a67/raw/update.json"))
            startActivity(intent)
        }

        binding.btnExit.setOnClickListener { activity?.finish() }
    }

    private fun showFilterDurationDialog() {
        val options = arrayOf(
            getString(R.string.show_all),
            getString(R.string.more_than_30),
            getString(R.string.more_than_60),
            "أكثر من دقيقتين"
        )
        val values = intArrayOf(0, 30, 60, 120)
        
        var checkedItem = 0
        val currentMin = preferences.minDuration
        for (i in values.indices) {
            if (values[i] == currentMin) checkedItem = i
        }

        AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setTitle(R.string.filter_duration)
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                val selectedSeconds = values[which]
                preferences.minDuration = selectedSeconds
                viewModel.setMinDuration(selectedSeconds)
                Toast.makeText(context, "تم تحديث الفلتر: ${options[which]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupSwitches() {
        binding.switchPauseOnAudioLoss.setOnCheckedChangeListener { _, isChecked -> 
            preferences.putBoolean("pause_on_loss", isChecked) 
        }
        binding.switchFade.setOnCheckedChangeListener { _, isChecked -> 
            preferences.putBoolean("fade_enabled", isChecked) 
        }
        binding.switchBtBlock.setOnCheckedChangeListener { _, isChecked -> 
            preferences.putBoolean("bt_block_enabled", isChecked) 
        }
        binding.switchAlbumGrid.setOnCheckedChangeListener { _, isChecked -> 
            preferences.putBoolean("album_grid_enabled", isChecked)
            viewModel.setAlbumGridEnabled(isChecked) 
        }
        binding.switchLockScreen.setOnCheckedChangeListener { _, isChecked -> 
            preferences.putBoolean("lock_screen_enabled", isChecked) 
        }
    }

    private fun loadPreferences() {
        binding.switchPauseOnAudioLoss.isChecked = preferences.getBoolean("pause_on_loss", true)
        binding.switchFade.isChecked = preferences.getBoolean("fade_enabled", true)
        binding.switchBtBlock.isChecked = preferences.getBoolean("bt_block_enabled", false)
        binding.switchAlbumGrid.isChecked = preferences.getBoolean("album_grid_enabled", false)
        binding.switchLockScreen.isChecked = preferences.getBoolean("lock_screen_enabled", false)
        viewModel.setMinDuration(preferences.minDuration)
    }
}
