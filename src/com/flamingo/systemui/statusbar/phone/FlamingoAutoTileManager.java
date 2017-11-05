/*
 * Copyright (C) 2016 The Android Open Source Project
 * Copyright (C) 2022 FlamingoOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.flamingo.systemui.statusbar.phone;

import static com.android.systemui.qs.dagger.QSFlagsModule.RBC_AVAILABLE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.NightDisplayListener;
import android.os.Handler;

import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.qs.AutoAddTracker;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.qs.ReduceBrightColorsController;
import com.android.systemui.statusbar.phone.AutoTileManager;
import com.android.systemui.statusbar.phone.ManagedProfileController;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.DeviceControlsController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.SafetyController;
import com.android.systemui.statusbar.policy.WalletController;
import com.android.systemui.util.settings.SecureSettings;

import java.util.List;

import javax.inject.Named;

public final class FlamingoAutoTileManager extends AutoTileManager {

    private static final String LIVEDISPLAY_INTENT = "lineageos.intent.action.INITIALIZE_LIVEDISPLAY";
    private static final List<String> LIVEDISPLAY_TILES = List.of("livedisplay");

    private final BroadcastReceiver mLiveDisplayReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LIVEDISPLAY_TILES.forEach(spec -> {
                if (mAutoTracker.isAdded(spec)) return;
                mHost.addTile(spec);
                mAutoTracker.setTileAdded(spec);
            });
        }
    };

    public FlamingoAutoTileManager(
        Context context,
        AutoAddTracker.Builder autoAddTrackerBuilder,
        QSTileHost host,
        @Background Handler handler,
        SecureSettings secureSettings,
        HotspotController hotspotController,
        DataSaverController dataSaverController,
        ManagedProfileController managedProfileController,
        NightDisplayListener nightDisplayListener,
        CastController castController,
        ReduceBrightColorsController reduceBrightColorsController,
        DeviceControlsController deviceControlsController,
        WalletController walletController,
        SafetyController safetyController,
        @Named(RBC_AVAILABLE) boolean isReduceBrightColorsAvailable
    ) {
        super(
            context,
            autoAddTrackerBuilder,
            host,
            handler,
            secureSettings,
            hotspotController,
            dataSaverController,
            managedProfileController,
            nightDisplayListener,
            castController,
            reduceBrightColorsController,
            deviceControlsController,
            walletController,
            safetyController,
            isReduceBrightColorsAvailable
        );
    }

    @Override
    protected void startControllersAndSettingsListeners() {
        super.startControllersAndSettingsListeners();
        mContext.registerReceiver(
            mLiveDisplayReceiver,
            new IntentFilter(LIVEDISPLAY_INTENT)
        );
    }

    @Override
    protected void stopListening() {
        mContext.unregisterReceiver(mLiveDisplayReceiver);
        super.stopListening();
    }
}
