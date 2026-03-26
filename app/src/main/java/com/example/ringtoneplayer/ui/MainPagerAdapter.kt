package com.example.ringtoneplayer.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 8

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SongsFragment()
            1 -> AlbumsFragment()
            2 -> ArtistsFragment()
            3 -> FoldersFragment()
            4 -> PlaylistsFragment()
            5 -> GenresFragment()
            6 -> BooksFragment()
            7 -> SettingsFragment()
            else -> SongsFragment()
        }
    }
}
