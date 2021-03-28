/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.flamingo.systemui.qs.tiles;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.Global;
import android.service.quicksettings.Tile;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile.State;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.util.RingerModeTracker;

import javax.inject.Inject;

public final class SoundTile extends QSTileImpl<State> {

    private final ZenModeController mZenController;
    private final AudioManager mAudioManager;
    private final RingerModeTracker mRingerModeTracker;

    @Inject
    public SoundTile(
            QSHost host,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            ZenModeController zenModeController,
            RingerModeTracker ringerModeTracker
    ) {
        super(host, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger);
        mZenController = zenModeController;
        mAudioManager = mContext.getSystemService(AudioManager.class);
        mRingerModeTracker = ringerModeTracker;
    }

    @Override
    public State newTileState() {
        final State state = new State();
        state.label = getTileLabel();
        state.expandedAccessibilityClassName = Button.class.getName();
        return state;
    }

    @Override
    protected void handleInitialize() {
        super.handleInitialize();
        mUiHandler.post(() -> {
            mRingerModeTracker.getRingerModeInternal().observe(this, (mode) -> refreshState(mode));
        });
    }

    @Override
    protected void handleClick(@Nullable View view) {
        updateState();
    }

    @Override
    public void handleLongClick(@Nullable View view) {
        mAudioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
    }

    @Override
    @Nullable
    public Intent getLongClickIntent() {
        return null;
    }

    private int getRingerModeInternal() {
        return mRingerModeTracker.getRingerModeInternal().getValue();
    }

    private void updateState() {
        final int mode = getRingerModeInternal();
        switch (mode) {
            case AudioManager.RINGER_MODE_NORMAL:
                mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE);
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT);
                mZenController.setZen(Global.ZEN_MODE_ALARMS, null, TAG);
                break;
            case AudioManager.RINGER_MODE_SILENT:
                mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
                mZenController.setZen(Global.ZEN_MODE_OFF, null, TAG);
                break;
            default:
                Log.e(TAG, "Unknown ringer mode " + mode);
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_sound_label);
    }

    @Override
    protected void handleUpdateState(State state, Object arg) {
        final int mode = arg != null ? ((Integer) arg) : getRingerModeInternal();
        switch (mode) {
            case AudioManager.RINGER_MODE_NORMAL:
                state.icon = ResourceIcon.get(R.drawable.ic_volume_ringer);
                state.secondaryLabel = mContext.getString(R.string.quick_settings_sound_ring);
                state.contentDescription =  mContext.getString(
                        R.string.quick_settings_sound_ring);
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                state.icon = ResourceIcon.get(R.drawable.ic_volume_ringer_vibrate);
                state.secondaryLabel = mContext.getString(R.string.quick_settings_sound_vibrate);
                state.contentDescription =  mContext.getString(
                        R.string.quick_settings_sound_vibrate);
                break;
            case AudioManager.RINGER_MODE_SILENT:
                state.icon = ResourceIcon.get(R.drawable.ic_volume_ringer_mute);
                state.secondaryLabel = mContext.getString(R.string.quick_settings_sound_dnd);
                state.contentDescription =  mContext.getString(
                        R.string.quick_settings_sound_dnd);
                break;
            default:
                Log.e(TAG, "Unknown ringer mode " + mode);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FLAMINGO;
    }
}
