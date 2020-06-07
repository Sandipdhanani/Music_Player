package com.exmple.musicplayer.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import com.exmple.musicplayer.R
import com.exmple.musicplayer.databases.SongsDatabase
import com.exmple.musicplayer.helpers.*
import com.exmple.musicplayer.interfaces.PlaylistsDao
import com.exmple.musicplayer.interfaces.SongsDao
import com.exmple.musicplayer.models.Playlist
import com.exmple.musicplayer.models.Song
import com.exmple.musicplayer.services.MusicService
import com.simplemobiletools.commons.helpers.isOreoPlus
import com.simplemobiletools.commons.helpers.isQPlus
import java.io.File

@SuppressLint("NewApi")
fun Context.sendIntent(action: String) {
    Intent(this, MusicService::class.java).apply {
        this.action = action
        try {
            if (isOreoPlus()) {
                startForegroundService(this)
            } else {
                startService(this)
            }
        } catch (ignored: Exception) {
        }
    }
}

val Context.config: Config get() = Config.newInstance(applicationContext)

val Context.playlistDAO: PlaylistsDao get() = getSongsDB().PlaylistsDao()

val Context.songsDAO: SongsDao get() = getSongsDB().SongsDao()

fun Context.playlistChanged(newID: Int, callSetup: Boolean = true) {
    config.currentPlaylist = newID
    sendIntent(PAUSE)
    Intent(this, MusicService::class.java).apply {
        putExtra(CALL_SETUP_AFTER, callSetup)
        action = REFRESH_LIST
        startService(this)
    }
}

fun Context.getActionBarHeight(): Int {
    val textSizeAttr = intArrayOf(R.attr.actionBarSize)
    val attrs = obtainStyledAttributes(TypedValue().data, textSizeAttr)
    val actionBarSize = attrs.getDimensionPixelSize(0, -1)
    attrs.recycle()
    return actionBarSize
}

fun Context.getSongsDB() = SongsDatabase.getInstance(this)

fun Context.getPlaylistIdWithTitle(title: String) = playlistDAO.getPlaylistWithTitle(title)?.id ?: -1

fun Context.getPlaylistSongs(playlistId: Int): ArrayList<Song> {
    val validSongs = ArrayList<Song>()
    if (isQPlus()) {
        validSongs.addAll(songsDAO.getSongsFromPlaylist(playlistId))
    } else {
        val invalidSongs = ArrayList<Song>()
        val songs = songsDAO.getSongsFromPlaylist(playlistId)
        val showFilename = config.showFilename
        songs.forEach {
            it.title = it.getProperTitle(showFilename)

            if (File(it.path).exists() || it.path.startsWith("content://")) {
                validSongs.add(it)
            } else {
                invalidSongs.add(it)
            }
        }

        getSongsDB().runInTransaction {
            invalidSongs.forEach {
                songsDAO.removeSongPath(it.path)
            }
        }
    }

    return validSongs
}

fun Context.deletePlaylists(playlists: ArrayList<Playlist>) {
    playlistDAO.deletePlaylists(playlists)
    playlists.forEach {
        songsDAO.removePlaylistSongs(it.id)
    }
}

fun Context.broadcastUpdateWidgetSong(newSong: Song?) {
    Intent(this, MyWidgetProvider::class.java).apply {
        putExtra(NEW_SONG, newSong)
        action = SONG_CHANGED
        sendBroadcast(this)
    }
}

fun Context.broadcastUpdateWidgetSongState(isPlaying: Boolean) {
    Intent(this, MyWidgetProvider::class.java).apply {
        putExtra(IS_PLAYING, isPlaying)
        action = SONG_STATE_CHANGED
        sendBroadcast(this)
    }
}
