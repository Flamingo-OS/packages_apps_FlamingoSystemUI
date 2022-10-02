/*
 * Copyright (C) 2022 FlamingoOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flamingo.systemui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

import com.android.systemui.broadcast.BroadcastDispatcher
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.qs.QSTileHost

import javax.inject.Inject

private const val ACTION = "lineageos.intent.action.INITIALIZE_LIVEDISPLAY"
private val LIVEDISPLAY_TILES = setOf("livedisplay", "anti_flicker")

@SysUISingleton
class LiveDisplayInitReceiver @Inject constructor(
    private val broadcastDispatcher: BroadcastDispatcher,
    private val qsTileHost: QSTileHost
) : BroadcastReceiver() {

    fun register() {
        broadcastDispatcher.registerReceiver(this, IntentFilter(ACTION))
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION) return
        qsTileHost.tiles.filter {
            LIVEDISPLAY_TILES.contains(it.tileSpec)
        }.forEach {
            it.refreshState()
        }
        broadcastDispatcher.unregisterReceiver(this)
    }
}