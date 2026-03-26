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

class GenresFragment : Fragment() {

    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: FolderAdapter // Use FolderAdapter for similar look

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FolderAdapter(emptyList()) { genre ->
            viewModel.filterByGenre(genre.name)
            (activity as? MainActivity)?.binding?.viewPager?.currentItem = 0
        }

        binding.rvSongs.layoutManager = GridLayoutManager(context, 2)
        binding.rvSongs.adapter = adapter

        viewModel.songs.observe(viewLifecycleOwner) { songList ->
            val genres = songList.groupBy { it.album }.map { (name, songs) ->
                Folder(name, "", songs.size)
            }
            adapter.updateData(genres)
            binding.tvEmpty.text = "لا توجد أنواع"
            binding.tvEmpty.visibility = if (genres.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
