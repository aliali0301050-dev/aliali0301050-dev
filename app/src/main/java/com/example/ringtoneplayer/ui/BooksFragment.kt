package com.example.ringtoneplayer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.ringtoneplayer.databinding.FragmentSongsBinding

class BooksFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentSongsBinding.inflate(inflater, container, false)
        binding.tvEmpty.text = "لا توجد كتب صوتية"
        binding.tvEmpty.visibility = View.VISIBLE
        return binding.root
    }
}
