/*
 * Copyright (C) 2020-2021 The LineageOS Project
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

import static com.android.internal.lineage.hardware.LiveDisplayManager.FEATURE_ANTI_FLICKER;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.internal.lineage.hardware.LiveDisplayManager;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.util.settings.SystemSettings;

import javax.inject.Inject;

public final class AntiFlickerTile extends QSTileImpl<BooleanState> {

    private static final Intent LIVEDISPLAY_SETTINGS = new Intent("com.android.settings.LIVEDISPLAY_SETTINGS");
    private static final Icon sIcon = ResourceIcon.get(R.drawable.ic_qs_anti_flicker);

    private final LiveDisplayManager mLiveDisplay;
    private final SystemSettings mSystemSettings;

    @Inject
    public AntiFlickerTile(
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
        mLiveDisplay = LiveDisplayManager.getInstance(mContext);
        mSystemSettings = systemSettings;
    }

    @Override
    public BooleanState newTileState() {
        final BooleanState state = new BooleanState();
        state.icon = sIcon;
        state.label = getTileLabel();
        return state;
    }

    @Override
    protected void handleClick(@Nullable View view) {
        setEnabled(!mLiveDisplay.isAntiFlickerEnabled());
        refreshState();
    }

    private void setEnabled(boolean enabled) {
        mSystemSettings.putIntForUser(
            Settings.System.DISPLAY_ANTI_FLICKER,
            enabled ? 1 : 0,
            UserHandle.USER_CURRENT
        );
    }

    @Override
    public Intent getLongClickIntent() {
        return LIVEDISPLAY_SETTINGS;
    }

    @Override
    public boolean isAvailable() {
        if (mLiveDisplay.getConfig() == null) return false;
        return mLiveDisplay.getConfig().hasFeature(FEATURE_ANTI_FLICKER);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.value = mLiveDisplay.isAntiFlickerEnabled();
        state.contentDescription = mContext.getString(R.string.quick_settings_anti_flicker);
        state.state = (state.value ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_anti_flicker);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FLAMINGO;
    }
}
