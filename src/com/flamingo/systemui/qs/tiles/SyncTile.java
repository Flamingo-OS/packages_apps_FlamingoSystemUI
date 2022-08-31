/*
 * Copyright (C) 2015 The CyanogenMod Project
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

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.view.View;
import android.widget.Switch;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;

import javax.inject.Inject;

/** Quick settings tile: Sync **/
public final class SyncTile extends QSTileImpl<BooleanState> {

    private static final Intent SYNC_SETTINGS = new Intent("android.settings.SYNC_SETTINGS")
        .addCategory(Intent.CATEGORY_DEFAULT);

    private final SyncStatusObserver mSyncObserver;

    private Object mSyncObserverHandle = null;
    private boolean mListening = false;

    @Inject
    public SyncTile(
            QSHost host,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger
    ) {
        super(host, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger);
        mSyncObserver = new SyncStatusObserver() {
            @Override
            public void onStatusChanged(int which) {
                mainHandler.post(() -> refreshState());
            }
        };
    }

    @Override
    public BooleanState newTileState() {
        final BooleanState state = new BooleanState();
        state.label = getTileLabel();
        state.expandedAccessibilityClassName = Switch.class.getName();
        return state;
    }

    @Override
    protected void handleClick(@Nullable View view) {
        ContentResolver.setMasterSyncAutomatically(!mState.value);
        refreshState();
    }

    @Override
    public Intent getLongClickIntent() {
        return SYNC_SETTINGS;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_sync_label);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FLAMINGO;
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.value = ContentResolver.getMasterSyncAutomatically();
        if (state.value) {
            state.icon = ResourceIcon.get(R.drawable.ic_qs_sync_on);
            state.contentDescription =  mContext.getString(
                    R.string.accessibility_quick_settings_sync_on);
            state.state = Tile.STATE_ACTIVE;
        } else {
            state.icon = ResourceIcon.get(R.drawable.ic_qs_sync_off);
            state.contentDescription =  mContext.getString(
                    R.string.accessibility_quick_settings_sync_off);
            state.state = Tile.STATE_INACTIVE;
        }
    }

    @Override
    public void handleSetListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
        if (mListening) {
            mSyncObserverHandle = ContentResolver.addStatusChangeListener(
                    ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS, mSyncObserver);
        } else {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }
}
