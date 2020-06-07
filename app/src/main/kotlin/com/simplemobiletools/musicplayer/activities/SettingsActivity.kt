package com.exmple.musicplayer.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import com.exmple.musicplayer.R
import com.exmple.musicplayer.extensions.config
import com.exmple.musicplayer.extensions.sendIntent
import com.exmple.musicplayer.helpers.REFRESH_LIST
import com.exmple.musicplayer.helpers.SHOW_FILENAME_ALWAYS
import com.exmple.musicplayer.helpers.SHOW_FILENAME_IF_UNAVAILABLE
import com.exmple.musicplayer.helpers.SHOW_FILENAME_NEVER
import com.exmple.musicplayer.services.MusicService
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.models.RadioItem
import kotlinx.android.synthetic.main.activity_settings.*
import java.util.*

class SettingsActivity : com.exmple.musicplayer.activities.SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        this.titleColor= Color.parseColor("#FFFFFF")
    }

    override fun onResume() {
        super.onResume()
        setupManagePlaylists()
        setupUseEnglish()
        setupShowAlbumCover()
        setupSwapPrevNext()
        setupEqualizer()
        setupReplaceTitle()
        updateTextColors(settings_scrollview)
        invalidateOptionsMenu()
        this.titleColor=Color.parseColor("#FFFFFF")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return super.onCreateOptionsMenu(menu)
    }




    private fun setupUseEnglish() {
        settings_use_english_holder.beVisibleIf(config.wasUseEnglishToggled || Locale.getDefault().language != "en")
        settings_use_english.isChecked = config.useEnglish
        settings_use_english_holder.setOnClickListener {
            settings_use_english.toggle()
            config.useEnglish = settings_use_english.isChecked
            System.exit(0)
        }
    }

    private fun setupManagePlaylists() {
        settings_manage_playlists_holder.setOnClickListener {
            startActivity(Intent(this, com.exmple.musicplayer.activities.PlaylistsActivity::class.java))
        }
    }

    private fun setupShowAlbumCover() {
        settings_show_album_cover.isChecked = config.showAlbumCover
        settings_show_album_cover_holder.setOnClickListener {
            settings_show_album_cover.toggle()
            config.showAlbumCover = settings_show_album_cover.isChecked
        }
    }


    private fun setupSwapPrevNext() {
        settings_swap_prev_next.isChecked = config.swapPrevNext
        settings_swap_prev_next_holder.setOnClickListener {
            settings_swap_prev_next.toggle()
            config.swapPrevNext = settings_swap_prev_next.isChecked
        }
    }

    private fun setupEqualizer() {
        val equalizer = MusicService.mEqualizer ?: return
        val items = arrayListOf<RadioItem>()
        try {
            (0 until equalizer.numberOfPresets).mapTo(items) { RadioItem(it, equalizer.getPresetName(it.toShort())) }
        } catch (e: Exception) {
            settings_equalizer_holder.beGone()
            return
        }

        settings_equalizer.text = items[config.equalizer].title
        settings_equalizer_holder.setOnClickListener {
            RadioGroupDialog(this@SettingsActivity, items, config.equalizer) {
                config.equalizer = it as Int
                settings_equalizer.text = items[it].title
            }
        }
    }

    private fun setupReplaceTitle() {
        settings_show_filename.text = getShowFilenameText()
        settings_show_filename_holder.setOnClickListener {
            val items = arrayListOf(
                    RadioItem(SHOW_FILENAME_NEVER, getString(R.string.never)),
                    RadioItem(SHOW_FILENAME_IF_UNAVAILABLE, getString(R.string.title_is_not_available)),
                    RadioItem(SHOW_FILENAME_ALWAYS, getString(R.string.always)))

            RadioGroupDialog(this@SettingsActivity, items, config.showFilename) {
                config.showFilename = it as Int
                settings_show_filename.text = getShowFilenameText()
                sendIntent(REFRESH_LIST)
            }
        }
    }

    private fun getShowFilenameText() = getString(when (config.showFilename) {
        SHOW_FILENAME_NEVER -> R.string.never
        SHOW_FILENAME_IF_UNAVAILABLE -> R.string.title_is_not_available
        else -> R.string.always
    })
}
