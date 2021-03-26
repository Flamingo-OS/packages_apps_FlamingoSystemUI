/*
 * Copyright (C) 2015 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
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
import android.database.ContentObserver;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.view.View;
import android.widget.Switch;

import androidx.annotation.Nullable;

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
import com.android.systemui.util.settings.SecureSettings;

import javax.inject.Inject;

/** Quick settings tile: Ambient Display **/
public class AmbientDisplayTile extends QSTileImpl<BooleanState> {

    private static final Intent LOCK_SCREEN_SETTINGS = new Intent("android.settings.LOCK_SCREEN_SETTINGS");

    private final AmbientDisplayConfiguration mConfig;
    private final SecureSettings mSecureSettings;
    private final ContentObserver mSettingsObserver;
    private final Handler mMainHandler, mBgHandler;

    private boolean mEnabled = false;

    @Inject
    public AmbientDisplayTile(
            QSHost host,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            SecureSettings secureSettings,
            @Background Handler bgHandler
    ) {
        super(host, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger);
        mConfig = new AmbientDisplayConfiguration(mContext);
        mSecureSettings = secureSettings;
        mMainHandler = mainHandler;
        mBgHandler = bgHandler;
        mSettingsObserver = new ContentObserver(mBgHandler) {
            @Override
            public void onChange(boolean selfChange) {
                updateState();
            }
        };
        mBgHandler.post(() -> {
            updateState();
        });
    }

    @Override
    public boolean isAvailable() {
        return mConfig.pulseOnNotificationAvailable();
    }

    @Override
    public BooleanState newTileState() {
        final BooleanState state = new BooleanState();
        state.label = getTileLabel();
        state.icon = ResourceIcon.get(R.drawable.ic_qs_ambient_display);
        state.expandedAccessibilityClassName = Switch.class.getName();
        return state;
    }

    @Override
    protected void handleInitialize() {
        super.handleInitialize();
        mSecureSettings.registerContentObserverForUser(
            Settings.Secure.DOZE_ENABLED,
            mSettingsObserver,
            UserHandle.USER_ALL
        );
    }

    @Override
    protected void handleClick(@Nullable View view) {
        final boolean enabled = !mEnabled;
        mBgHandler.post(() -> {
            mSecureSettings.putIntForUser(
                Settings.Secure.DOZE_ENABLED,
                enabled ? 1 : 0,
                UserHandle.USER_CURRENT
            );
        });
    }

    @Override
    public Intent getLongClickIntent() {
        return LOCK_SCREEN_SETTINGS;
    }

    private void updateState() {
        final boolean enabled = mConfig.pulseOnNotificationEnabled(UserHandle.USER_CURRENT);
        mMainHandler.post(() -> {
            mEnabled = enabled;
            refreshState();
        });
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.value = mEnabled;
        if (mEnabled) {
            state.contentDescription =  mContext.getString(
                    R.string.accessibility_quick_settings_ambient_display_on);
            state.state = Tile.STATE_ACTIVE;
        } else {
            state.contentDescription =  mContext.getString(
                    R.string.accessibility_quick_settings_ambient_display_off);
            state.state = Tile.STATE_INACTIVE;
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_ambient_display_label);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FLAMINGO;
    }

    @Override
    protected void handleDestroy() {
        mSecureSettings.unregisterContentObserver(mSettingsObserver);
        super.handleDestroy();
    }
}
