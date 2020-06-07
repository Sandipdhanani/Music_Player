package com.exmple.musicplayer.activities

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.exmple.musicplayer.R
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.exmple.musicplayer.dialogs.NewPlaylistDialog
import com.exmple.musicplayer.extensions.playlistChanged
import com.exmple.musicplayer.extensions.playlistDAO
import com.exmple.musicplayer.interfaces.RefreshPlaylistsListener
import com.exmple.musicplayer.models.Playlist

import kotlinx.android.synthetic.main.activity_playlists.*

class PlaylistsActivity : com.exmple.musicplayer.activities.SimpleActivity(), RefreshPlaylistsListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlists)
        getPlaylists()
        this.titleColor= Color.parseColor("#FFFFFF")
    }

    private fun getPlaylists() {
        ensureBackgroundThread {
            val playlists = playlistDAO.getAll() as ArrayList<Playlist>
            runOnUiThread {
                com.exmple.musicplayer.adapters.PlaylistsAdapter(this@PlaylistsActivity, playlists, this@PlaylistsActivity, playlists_list) {
                    getPlaylists()
                    playlistChanged((it as Playlist).id)
                }.apply {
                    playlists_list.adapter = this
                }
            }
        }
    }

    override fun refreshItems() {
        getPlaylists()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_playlists, menu)
        //updateMenuItemColors(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.create_playlist -> showCreatePlaylistFolder()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun showCreatePlaylistFolder() {
        NewPlaylistDialog(this) {
            getPlaylists()
        }
    }
}
