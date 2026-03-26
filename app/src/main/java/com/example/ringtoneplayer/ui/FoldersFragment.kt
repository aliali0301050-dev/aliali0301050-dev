package com.example.ringtoneplayer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ringtoneplayer.MainActivity
import com.example.ringtoneplayer.databinding.FragmentSongsBinding

class FoldersFragment : Fragment() {

    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: FolderAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FolderAdapter(emptyList()) { folder ->
            // 1. استخدام فلتر المجلد الجديد بدلاً من نص البحث
            viewModel.filterByFolder(folder.path) 
            
            // 2. الانتقال لتبويب الأغاني لرؤية المحتوى
            (activity as? MainActivity)?.binding?.viewPager?.currentItem = 0
        }

        binding.rvSongs.layoutManager = GridLayoutManager(context, 2)
        binding.rvSongs.adapter = adapter

        viewModel.folders.observe(viewLifecycleOwner) { folderList ->
            if (folderList != null) {
                adapter.updateData(folderList)
                binding.tvEmpty.text = "لا توجد مجلدات"
                binding.tvEmpty.visibility = if (folderList.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
