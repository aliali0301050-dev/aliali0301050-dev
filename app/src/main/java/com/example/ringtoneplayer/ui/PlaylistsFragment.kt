package com.example.ringtoneplayer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ringtoneplayer.MainActivity
import com.example.ringtoneplayer.databinding.FragmentSongsBinding
import com.example.ringtoneplayer.models.Song

class PlaylistsFragment : Fragment(), SongAdapter.OnSongClickListener {

    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: SongAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = SongAdapter(mutableListOf(), this)
        binding.rvSongs.layoutManager = LinearLayoutManager(context)
        binding.rvSongs.adapter = adapter

        viewModel.songs.observe(viewLifecycleOwner) { list ->
            val favorites = list.filter { it.isFavorite }
            adapter.updateData(favorites)
            binding.tvEmpty.text = "لا توجد مفضلات"
            binding.tvEmpty.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onSongClick(song: Song, position: Int) { 
        viewModel.setCurrentSong(position) 
    }
    
    override fun onSongLongClick(song: Song, position: Int) {}
    
    override fun onMoreClick(song: Song, view: View) { 
        (activity as? MainActivity)?.showSongOptions(song, view) 
    }
    
    override fun onSelectionChanged(selectedCount: Int) { 
        (activity as? MainActivity)?.updateActionMode(selectedCount)
    }
    
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
