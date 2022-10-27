/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tiles.BluetoothTile;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.flamingo.systemui.qs.tiles.dialog.BluetoothDialogFactory;

import java.util.List;

import javax.inject.Inject;

/** Quick settings tile: Bluetooth **/
public final class FlamingoBluetoothTile extends BluetoothTile {

    private final Handler mHandler;
    private final BluetoothDialogFactory mBluetoothDialogFactory;

    @Inject
    public FlamingoBluetoothTile(
            QSHost host,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            BluetoothController bluetoothController,
            KeyguardStateController keyguardStateController,
	        BluetoothDialogFactory bluetoothDialogFactory
    ) {
        super(host, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger, bluetoothController, keyguardStateController);
        mHandler = mainHandler;
        mBluetoothDialogFactory = bluetoothDialogFactory;
    }

    @Override
    public BooleanState newTileState() {
        final BooleanState state = new BooleanState();
        state.forceExpandIcon = true;
        return state;
    }

    @Override
    protected void handleClick(@Nullable View view, boolean keyguardShowing) {
        if (checkKeyguard(view, keyguardShowing)) {
            return;
        }
	    mHandler.post(() -> mBluetoothDialogFactory.create(true, view));
    }
}
