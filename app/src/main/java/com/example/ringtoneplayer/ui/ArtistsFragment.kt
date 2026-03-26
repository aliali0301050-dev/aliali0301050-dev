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

class ArtistsFragment : Fragment() {

    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: ArtistAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ArtistAdapter(emptyList()) { artist ->
            // Filter songs by artist and jump to Songs tab
            viewModel.filterByArtist(artist.name)
            (activity as? MainActivity)?.binding?.viewPager?.currentItem = 0
        }

        binding.rvSongs.layoutManager = GridLayoutManager(context, 2)
        binding.rvSongs.adapter = adapter

        viewModel.artists.observe(viewLifecycleOwner) { artistList ->
            if (artistList != null) {
                adapter.updateData(artistList)
                binding.tvEmpty.text = "لا توجد فنانين"
                binding.tvEmpty.visibility = if (artistList.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
