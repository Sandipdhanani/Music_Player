package com.exmple.musicplayer.activities

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import com.duolingo.open.rtlviewpager.BuildConfig
import com.exmple.musicplayer.R
import com.exmple.musicplayer.dialogs.ChangeSortingDialog
import com.exmple.musicplayer.dialogs.NewPlaylistDialog
import com.exmple.musicplayer.dialogs.RemovePlaylistDialog
import com.exmple.musicplayer.dialogs.SleepTimerCustomDialog
import com.exmple.musicplayer.extensions.*
import com.exmple.musicplayer.helpers.*
import com.exmple.musicplayer.interfaces.MainActivityInterface
import com.exmple.musicplayer.models.Events
import com.exmple.musicplayer.models.Playlist
import com.exmple.musicplayer.models.Song
import com.exmple.musicplayer.services.MusicService
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.REAL_FILE_PATH
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.models.RadioItem
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_songs.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.util.*

class MainActivity : com.exmple.musicplayer.activities.SimpleActivity(), MainActivityInterface {
    private var isThirdPartyIntent = false
    private var isSearchOpen = false
    private var wasInitialPlaylistSet = false
    private var lastFilePickerPath = ""

    private var searchMenuItem: MenuItem? = null
    private var bus: EventBus? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched(BuildConfig.APPLICATION_ID)
        isThirdPartyIntent = intent.action == Intent.ACTION_VIEW

        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                initActivity()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
        volumeControlStream = AudioManager.STREAM_MUSIC

        checkAppOnSDCard()

        // notify some users about the Dialer, SMS Messenger and Voice Recorder apps
        if (!config.wasMessengerRecorderShown && config.appRunCount > 35) {
            // NewAppsIconsDialog(this)
            config.wasMessengerRecorderShown = true
        }



    }

    override fun onResume() {
        super.onResume()
        updateTextColors(main_holder)
        getCurrentFragment()?.onResume()
        sleep_timer_holder.background = ColorDrawable(config.backgroundColor)
        sleep_timer_stop.applyColorFilter(config.textColor)
        invalidateOptionsMenu()
        this.titleColor=Color.parseColor("#FFFFFF")
    }

    override fun onPause() {
        super.onPause()
        getCurrentFragment()?.onPause()
    }

    override fun onStop() {
        super.onStop()
        searchMenuItem?.collapseActionView()
    }

    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)

        if (isThirdPartyIntent && !isChangingConfigurations) {
            sendIntent(FINISH_IF_NOT_PLAYING)
        }
    }

    @SuppressLint("ResourceType")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.menu_main, menu)
        setupSearch(menu)

        val autoplay = menu.findItem(R.id.toggle_autoplay)
        autoplay.title = getString(if (config.autoplay) R.string.disable_autoplay else R.string.enable_autoplay)

        menu.apply {
            findItem(R.id.sort).isVisible = !isThirdPartyIntent
            findItem(R.id.sort).isVisible = !isThirdPartyIntent
            findItem(R.id.open_playlist).isVisible = !isThirdPartyIntent
            findItem(R.id.add_folder_to_playlist).isVisible = !isThirdPartyIntent
            findItem(R.id.add_file_to_playlist).isVisible = !isThirdPartyIntent
            findItem(R.id.remove_playlist).isVisible = !isThirdPartyIntent
        }

        // updateMenuItemColors(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val isSongSelected = MusicService.mCurrSong != null

        menu.apply {
            findItem(R.id.remove_current).isVisible = !isThirdPartyIntent && isSongSelected
            findItem(R.id.delete_current).isVisible = !isThirdPartyIntent && isSongSelected
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.sort -> showSortingDialog()
            R.id.remove_current -> getCurrentFragment()?.getSongsAdapter()?.removeCurrentSongFromPlaylist()
            R.id.delete_current -> getCurrentFragment()?.getSongsAdapter()?.deleteCurrentSong()
            R.id.sleep_timer -> showSleepTimer()
            R.id.open_playlist -> openPlaylist()
            R.id.toggle_autoplay -> toggleAutoplay()
            R.id.add_folder_to_playlist -> addFolderToPlaylist()
            R.id.add_file_to_playlist -> addFileToPlaylist()
            R.id.create_playlist_from_folder -> createPlaylistFromFolder()
            R.id.remove_playlist -> removePlaylist()
            R.id.settings -> launchSettings()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_VIEW) {
            setIntent(intent)
            initThirdPartyIntent()
        }
    }

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchMenuItem = menu.findItem(R.id.search)
        (searchMenuItem!!.actionView as SearchView).apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (isSearchOpen) {
                        getCurrentFragment()?.searchQueryChanged(newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                getCurrentFragment()?.searchOpened()
                isSearchOpen = true
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                getCurrentFragment()?.searchClosed()
                isSearchOpen = false
                return true
            }
        })
    }

    private fun initActivity() {
        bus = EventBus.getDefault()
        bus!!.register(this)
        sleep_timer_stop.setOnClickListener { stopSleepTimer() }

        initFragments()
        initializePlayer()
    }

    private fun initFragments() {
        viewpager.adapter = com.exmple.musicplayer.adapters.ViewPagerAdapter(this)
    }

    private fun getCurrentFragment() = songs_fragment_holder

    private fun showSortingDialog() {
        ChangeSortingDialog(this) {
            sendIntent(REFRESH_LIST)
        }
    }

    private fun toggleAutoplay() {
        config.autoplay = !config.autoplay
        invalidateOptionsMenu()
        toast(if (config.autoplay) R.string.autoplay_enabled else R.string.autoplay_disabled)
    }

    private fun showSleepTimer() {
        val minutes = getString(R.string.minutes_raw)
        val hour = resources.getQuantityString(R.plurals.hours, 1, 1)

        val items = arrayListOf(
                RadioItem(5 * 60, "5 $minutes"),
                RadioItem(10 * 60, "10 $minutes"),
                RadioItem(20 * 60, "20 $minutes"),
                RadioItem(30 * 60, "30 $minutes"),
                RadioItem(60 * 60, hour))

        if (items.none { it.id == config.lastSleepTimerSeconds }) {
            val lastSleepTimerMinutes = config.lastSleepTimerSeconds / 60
            val text = resources.getQuantityString(R.plurals.minutes, lastSleepTimerMinutes, lastSleepTimerMinutes)
            items.add(RadioItem(config.lastSleepTimerSeconds, text))
        }

        items.sortBy { it.id }
        items.add(RadioItem(-1, getString(R.string.custom)))

        RadioGroupDialog(this, items, config.lastSleepTimerSeconds) {
            if (it as Int == -1) {
                SleepTimerCustomDialog(this) {
                    if (it > 0) {
                        pickedSleepTimer(it)
                    }
                }
            } else if (it > 0) {
                pickedSleepTimer(it)
            }
        }
    }

    private fun pickedSleepTimer(seconds: Int) {
        config.lastSleepTimerSeconds = seconds
        config.sleepInTS = System.currentTimeMillis() + seconds * 1000
        startSleepTimer()
    }

    private fun startSleepTimer() {
        sleep_timer_holder.beVisible()
        sendIntent(START_SLEEP_TIMER)
    }

    private fun stopSleepTimer() {
        sendIntent(STOP_SLEEP_TIMER)
        sleep_timer_holder.beGone()
    }

    private fun removePlaylist() {
        if (config.currentPlaylist == ALL_SONGS_PLAYLIST_ID) {
            toast(R.string.all_songs_cannot_be_deleted)
        } else {
            ensureBackgroundThread {
                val playlist = playlistDAO.getPlaylistWithId(config.currentPlaylist)
                runOnUiThread {
                    RemovePlaylistDialog(this, playlist) {
                        ensureBackgroundThread {
                            if (it) {
                                val paths = getPlaylistSongs(config.currentPlaylist).map { it.path }
                                val files = paths.map { FileDirItem(it, it.getFilenameFromPath()) } as ArrayList<FileDirItem>
                                paths.forEach {
                                    songsDAO.removeSongPath(it)
                                }
                                deleteFiles(files)
                            }

                            if (playlist != null) {
                                deletePlaylists(arrayListOf(playlist))
                            }
                            playlistChanged(ALL_SONGS_PLAYLIST_ID)
                        }
                    }
                }
            }
        }
    }

    private fun openPlaylist() {
        ensureBackgroundThread {
            val playlists = playlistDAO.getAll() as ArrayList<Playlist>
            runOnUiThread {
                showPlaylists(playlists)
            }
        }
    }

    private fun showPlaylists(playlists: ArrayList<Playlist>) {
        val items = arrayListOf<RadioItem>()
        playlists.mapTo(items) { RadioItem(it.id, it.title) }
        items.add(RadioItem(-1, getString(R.string.create_playlist)))

        RadioGroupDialog(this, items, config.currentPlaylist) {
            if (it == -1) {
                NewPlaylistDialog(this) {
                    wasInitialPlaylistSet = false
                    MusicService.mCurrSong = null
                    playlistChanged(it, false)
                    invalidateOptionsMenu()
                }
            } else {
                wasInitialPlaylistSet = false
                playlistChanged(it as Int)
                invalidateOptionsMenu()
            }
        }
    }

    override fun addFolderToPlaylist() {
        FilePickerDialog(this, getFilePickerInitialPath(), pickFile = false) {
            toast(R.string.fetching_songs)
            ensureBackgroundThread {
                val folderSongs = getFolderSongs(File(it))
                RoomHelper(applicationContext).addPathsToPlaylist(folderSongs)
                sendIntent(REFRESH_LIST)
            }
        }
    }

    private fun getFolderSongs(folder: File): ArrayList<String> {
        val songFiles = ArrayList<String>()
        val files = folder.listFiles() ?: return songFiles
        files.forEach {
            if (it.isDirectory) {
                songFiles.addAll(getFolderSongs(it))
                lastFilePickerPath = it.absolutePath
            } else if (it.isAudioFast()) {
                songFiles.add(it.absolutePath)
            }
        }
        return songFiles
    }

    private fun addFileToPlaylist() {
        FilePickerDialog(this, getFilePickerInitialPath()) {
            ensureBackgroundThread {
                lastFilePickerPath = it
                if (it.isAudioFast()) {
                    RoomHelper(applicationContext).addPathToPlaylist(it)
                    sendIntent(REFRESH_LIST)
                } else {
                    toast(R.string.invalid_file_format)
                }
            }
        }
    }

    private fun createPlaylistFromFolder() {
        FilePickerDialog(this, getFilePickerInitialPath(), pickFile = false) {
            ensureBackgroundThread {
                createPlaylistFrom(it)
            }
        }
    }

    private fun createPlaylistFrom(path: String) {
        val folderSongs = getFolderSongs(File(path))
        if (folderSongs.isEmpty()) {
            toast(R.string.folder_contains_no_audio)
            return
        }

        lastFilePickerPath = path
        val folderName = path.getFilenameFromPath()
        var playlistName = folderName
        var curIndex = 1
        val playlistIdWithTitle = getPlaylistIdWithTitle(folderName)
        if (playlistIdWithTitle != -1) {
            while (true) {
                playlistName = "${folderName}_$curIndex"
                if (getPlaylistIdWithTitle(playlistName) == -1) {
                    break
                }

                curIndex++
            }
        }

        val playlist = Playlist(0, playlistName)
        val newPlaylistId = playlistDAO.insert(playlist).toInt()
        RoomHelper(applicationContext).addPathsToPlaylist(folderSongs, newPlaylistId)
        playlistChanged(newPlaylistId)
    }

    private fun getFilePickerInitialPath(): String {
        if (lastFilePickerPath.isEmpty()) {
            lastFilePickerPath = getCurrentFragment()?.getDefaultFilePickerPath()
                    ?: config.internalStoragePath
        }

        return lastFilePickerPath
    }

    private fun initializePlayer() {
        if (isThirdPartyIntent) {
            initThirdPartyIntent()
        } else {
            sendIntent(INIT)
        }
    }

    private fun initThirdPartyIntent() {
        val realPath = intent.getStringExtra(REAL_FILE_PATH) ?: ""
        var fileUri = intent.data
        if (realPath.isNotEmpty()) {
            fileUri = Uri.fromFile(File(realPath))
        }

        Intent(this, MusicService::class.java).apply {
            data = fileUri
            action = INIT_PATH
            startService(this)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun songChangedEvent(event: Events.SongChanged) {
        if (wasInitialPlaylistSet) {
            getCurrentFragment()?.songChangedEvent(event.song)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun songStateChanged(event: Events.SongStateChanged) {
        getCurrentFragment()?.songStateChanged(event.isPlaying)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun playlistUpdated(event: Events.PlaylistUpdated) {
        wasInitialPlaylistSet = true
        getCurrentFragment()?.fillSongsListView(event.songs.clone() as ArrayList<Song>)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun progressUpdated(event: Events.ProgressUpdated) {
        getCurrentFragment()?.songProgressUpdated(event.progress)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun noStoragePermission(event: Events.NoStoragePermission) {
        toast(R.string.no_storage_permissions)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun sleepTimerChanged(event: Events.SleepTimerChanged) {
        sleep_timer_holder.beVisible()
        sleep_timer_value.text = event.seconds.getFormattedDuration()

        if (event.seconds == 0) {
            finish()
        }
    }

    override fun getIsSearchOpen() = isSearchOpen

    override fun getIsThirdPartyIntent() = isThirdPartyIntent

    private fun launchSettings() {
        startActivity(Intent(applicationContext, com.exmple.musicplayer.activities.SettingsActivity::class.java))
    }


}
