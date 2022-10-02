/*
 * Copyright (C) 2015 The CyanogenMod Project
 * Copyright (C) 2018-2019 The LineageOS Project
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

import static com.android.internal.lineage.hardware.LiveDisplayManager.FEATURE_MANAGED_OUTDOOR_MODE;
import static com.android.internal.lineage.hardware.LiveDisplayManager.MODE_AUTO;
import static com.android.internal.lineage.hardware.LiveDisplayManager.MODE_DAY;
import static com.android.internal.lineage.hardware.LiveDisplayManager.MODE_NIGHT;
import static com.android.internal.lineage.hardware.LiveDisplayManager.MODE_OFF;
import static com.android.internal.lineage.hardware.LiveDisplayManager.MODE_OUTDOOR;

import android.annotation.Nullable;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.hardware.display.ColorDisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.view.View;

import com.android.internal.R;
import com.android.internal.lineage.hardware.LiveDisplayManager;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.ArrayUtils;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile.State;
import com.android.systemui.plugins.annotations.ProvidesInterface;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.util.settings.SystemSettings;
import com.flamingo.systemui.qs.tiles.LiveDisplayTile.LiveDisplayState;

import javax.inject.Inject;

/** Quick settings tile: LiveDisplay mode switcher **/
public final class LiveDisplayTile extends QSTileImpl<LiveDisplayState> {

    private static final Intent LIVEDISPLAY_SETTINGS =
            new Intent("com.android.settings.LIVEDISPLAY_SETTINGS");

    private static final int OFF_TEMPERATURE = 6500;

    private final SystemSettings mSystemSettings;
    private final LiveDisplayManager mLiveDisplay;
    private final LiveDisplayObserver mObserver;

    private final String[] mEntries;
    private final String[] mDescriptionEntries;
    private final String[] mValues;
    private final int[] mEntryIconRes;

    private final boolean mNightDisplayAvailable;

    private boolean mListening;
    private int mDayTemperature;

    @Inject
    public LiveDisplayTile(
        QSHost host,
        @Background Looper backgroundLooper,
        @Main Handler mainHandler,
        FalsingManager falsingManager,
        MetricsLogger metricsLogger,
        StatusBarStateController statusBarStateController,
        ActivityStarter activityStarter,
        QSLogger qsLogger,
        SystemSettings systemSettings
    ) {
        super(host, backgroundLooper, mainHandler, falsingManager, metricsLogger,
            statusBarStateController, activityStarter, qsLogger);
        mNightDisplayAvailable = ColorDisplayManager.isNightDisplayAvailable(mContext);

        final Resources res = mContext.getResources();

        TypedArray typedArray = res.obtainTypedArray(R.array.live_display_drawables);
        try {
            mEntryIconRes = new int[typedArray.length()];
            for (int i = 0; i < mEntryIconRes.length; i++) {
                mEntryIconRes[i] = typedArray.getResourceId(i, 0);
            }
        } finally {
            typedArray.recycle();
        }

        mEntries = res.getStringArray(R.array.live_display_entries);
        mDescriptionEntries = res.getStringArray(R.array.live_display_description);
        mValues = res.getStringArray(R.array.live_display_values);

        mLiveDisplay = LiveDisplayManager.getInstance(mContext);
        if (mLiveDisplay.getConfig() != null) {
            mDayTemperature = mLiveDisplay.getDayColorTemperature();
        } else {
            mDayTemperature = -1;
        }

        mSystemSettings = systemSettings;
        mObserver = new LiveDisplayObserver(mHandler);
    }

    private boolean isOutdoorModeAvailable() {
        return mLiveDisplay.getConfig() != null &&
            mLiveDisplay.getConfig().hasFeature(MODE_OUTDOOR) &&
            !mLiveDisplay.getConfig().hasFeature(FEATURE_MANAGED_OUTDOOR_MODE);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public LiveDisplayState newTileState() {
        final LiveDisplayState state = new LiveDisplayState();
        state.label = getTileLabel();
        state.state = Tile.STATE_UNAVAILABLE;
        return state;
    }

    @Override
    public void handleSetListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
        if (listening) {
            mObserver.startObserving();
        } else {
            mObserver.endObserving();
        }
    }

    @Override
    protected void handleClick(@Nullable View view) {
        changeToNextMode();
    }

    @Override
    protected void handleUpdateState(LiveDisplayState state, Object arg) {
        state.mode = arg == null ? getCurrentModeIndex() : (Integer) arg;
        state.secondaryLabel = mEntries[state.mode];
        state.icon = ResourceIcon.get(mEntryIconRes[state.mode]);
        state.contentDescription = mDescriptionEntries[state.mode];
        if (!mNightDisplayAvailable || isOutdoorModeAvailable()) {
            state.state = mLiveDisplay.getMode() != MODE_OFF ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
        } else {
            state.state = Tile.STATE_UNAVAILABLE;
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FLAMINGO;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.live_display_title);
    }

    @Override
    public Intent getLongClickIntent() {
        return LIVEDISPLAY_SETTINGS;
    }

    private int getCurrentModeIndex() {
        String currentLiveDisplayMode = null;
        try {
            currentLiveDisplayMode = String.valueOf(mLiveDisplay.getMode());
        } catch (NullPointerException e) {
            currentLiveDisplayMode = String.valueOf(MODE_AUTO);
        } finally {
            return ArrayUtils.indexOf(mValues, currentLiveDisplayMode);
        }
    }

    private void changeToNextMode() {
        int next = getCurrentModeIndex() + 1;

        if (next >= mValues.length) {
            next = 0;
        }

        int nextMode = 0;
        final boolean isOutdoorModeAvailable = isOutdoorModeAvailable();

        while (true) {
            nextMode = Integer.valueOf(mValues[next]);
            // Skip outdoor mode if it's unsupported, skip the day setting
            // if it's the same as the off setting, and skip night display
            // on HWC2
            if ((!isOutdoorModeAvailable && nextMode == MODE_OUTDOOR) ||
                    (mDayTemperature == OFF_TEMPERATURE && nextMode == MODE_DAY) ||
                    (mNightDisplayAvailable && (nextMode == MODE_DAY || nextMode == MODE_NIGHT))) {
                next++;
                if (next >= mValues.length) {
                    next = 0;
                }
            } else {
                break;
            }
        }

        mSystemSettings.putIntForUser(
            Settings.System.DISPLAY_TEMPERATURE_MODE, nextMode,
            UserHandle.USER_CURRENT);
    }

    private class LiveDisplayObserver extends ContentObserver {
        LiveDisplayObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            mDayTemperature = mLiveDisplay.getDayColorTemperature();
            refreshState(getCurrentModeIndex());
        }

        void startObserving() {
            mSystemSettings.registerContentObserverForUser(
                    Settings.System.DISPLAY_TEMPERATURE_MODE,
                    this, UserHandle.USER_ALL);
            mSystemSettings.registerContentObserverForUser(
                    Settings.System.DISPLAY_TEMPERATURE_DAY,
                    this, UserHandle.USER_ALL);
        }

        void endObserving() {
            mSystemSettings.unregisterContentObserver(this);
        }
    }

    @ProvidesInterface(version = LiveDisplayState.VERSION)
    public static class LiveDisplayState extends State {
        static final int VERSION = 1;
        int mode;

        @Override
        public boolean copyTo(State other) {
            final LiveDisplayState o = (LiveDisplayState) other;
            final boolean changed = mode != o.mode;
            return super.copyTo(other) || changed;
        }

        @Override
        protected StringBuilder toStringBuilder() {
            final StringBuilder rt = super.toStringBuilder();
            rt.insert(rt.length() - 1, ",mode=" + mode);
            return rt;
        }

        @Override
        public State copy() {
            final LiveDisplayState state = new LiveDisplayState();
            copyTo(state);
            return state;
        }
    }
}
