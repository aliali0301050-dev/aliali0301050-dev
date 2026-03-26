package com.example.ringtoneplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ringtoneplayer.databinding.ActivityRecordBinding

class RecordActivity : BaseActivity() {

    private lateinit var binding: ActivityRecordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnStartRecording.setOnClickListener {
            // TODO: Implement start recording logic
        }

        binding.btnStopRecording.setOnClickListener {
            // TODO: Implement stop recording logic
        }

        binding.btnPlayRecording.setOnClickListener {
            // TODO: Implement play recording logic
        }
    }
}