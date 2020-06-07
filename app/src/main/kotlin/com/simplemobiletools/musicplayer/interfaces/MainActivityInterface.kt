package com.exmple.musicplayer.interfaces

interface MainActivityInterface {
    fun getIsSearchOpen(): Boolean

    fun getIsThirdPartyIntent(): Boolean

    fun addFolderToPlaylist()
}
