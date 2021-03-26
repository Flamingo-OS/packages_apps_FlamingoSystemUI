/*
 * Copyright (C) 2016 The CyanogenMod Project
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

package com.flamingo.systemui.qs.tiles;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.service.quicksettings.Tile;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;

import javax.inject.Inject;

/** Quick settings tile: Caffeine **/
public final class CaffeineTile extends QSTileImpl<BooleanState> {

    private static final Intent DISPLAY_INTENT = new Intent("com.android.settings.DISPLAY_SETTINGS");
    private static final Icon sIcon = ResourceIcon.get(R.drawable.ic_qs_caffeine);

    private final PowerManager.WakeLock mWakeLock;
    private final WakefulnessLifecycle mWakefulnessLifecycle;
    private final WakefulnessLifecycle.Observer mWakefulnessObserver =
        new WakefulnessLifecycle.Observer() {
            @Override
            public void onStartedGoingToSleep() {
                // disable caffeine if user force off (power button)
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
                refreshState();
            }
        };

    @Inject
    public CaffeineTile(
            QSHost host,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            WakefulnessLifecycle wakefulnessLifecycle
    ) {
        super(host, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger);
        mWakeLock = mContext.getSystemService(PowerManager.class).newWakeLock(
                PowerManager.FULL_WAKE_LOCK, CaffeineTile.class.getSimpleName());
        mWakefulnessLifecycle = wakefulnessLifecycle;
    }

    @Override
    public BooleanState newTileState() {
        final BooleanState state = new BooleanState();
        state.icon = sIcon;
        state.slash = new SlashState();
        state.label = getTileLabel();
        return state;
    }

    @Override
    protected void handleInitialize() {
        super.handleInitialize();
        mWakefulnessLifecycle.addObserver(mWakefulnessObserver);
    }

    @Override
    protected void handleDestroy() {
        mWakefulnessLifecycle.removeObserver(mWakefulnessObserver);
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        super.handleDestroy();
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_caffeine_label);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FLAMINGO;
    }

    @Override
    public void handleSetListening(boolean listening) {}

    @Override
    public void handleClick(@Nullable View view) {
        // toggle
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        } else {
            mWakeLock.acquire();
        }
        refreshState();
    }

    @Override
    public Intent getLongClickIntent() {
        return DISPLAY_INTENT;
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.value = mWakeLock.isHeld();
        if (state.value) {
            state.slash.isSlashed = false;
            state.contentDescription =  mContext.getString(
                    R.string.accessibility_quick_settings_caffeine_on);
            state.state = Tile.STATE_ACTIVE;
        } else {
            state.slash.isSlashed = true;
            state.contentDescription =  mContext.getString(
                    R.string.accessibility_quick_settings_caffeine_off);
            state.state = Tile.STATE_INACTIVE;
        }
    }
}
