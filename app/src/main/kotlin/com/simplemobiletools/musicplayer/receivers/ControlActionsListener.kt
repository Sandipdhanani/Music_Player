package com.exmple.musicplayer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.exmple.musicplayer.extensions.sendIntent
import com.exmple.musicplayer.helpers.FINISH
import com.exmple.musicplayer.helpers.NEXT
import com.exmple.musicplayer.helpers.PLAYPAUSE
import com.exmple.musicplayer.helpers.PREVIOUS


class ControlActionsListener : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        when (action) {
            PREVIOUS, PLAYPAUSE, NEXT, FINISH -> context.sendIntent(action)
        }
    }
}
