/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.flamingo.systemui.flags;

import static com.android.systemui.flags.Flags.COMBINED_STATUS_BAR_SIGNAL_ICONS;

import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.GuardedBy;

import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.dump.DumpManager;
import com.android.systemui.flags.BooleanFlag;
import com.android.systemui.flags.FeatureFlagsRelease;
import com.android.systemui.flags.SystemPropertiesHelper;
import com.android.systemui.util.settings.SecureSettings;

import javax.inject.Inject;

@SysUISingleton
public class FlamingoFeatureFlags extends FeatureFlagsRelease {

    private final Object mLock = new Object();
    private final SecureSettings mSecureSettings;
    private final ContentObserver mSettingsObserver;

    @GuardedBy("mLock")
    private boolean mIsCombinedSignalIcons;

    @Inject
    public FlamingoFeatureFlags(
        @Main Resources resources,
        SystemPropertiesHelper systemProperties,
        DumpManager dumpManager,
        @Background Handler backgroundHandler,
        SecureSettings secureSettings
    ) {
        super(resources, systemProperties, dumpManager);
        mSecureSettings = secureSettings;
        mSettingsObserver = new ContentObserver(backgroundHandler) {
            @Override
            public void onChange(boolean selfChange) {
                updateSettings();
            }
        };
        backgroundHandler.post(this::updateSettings);
        mSecureSettings.registerContentObserverForUser(
            Settings.Secure.COMBINED_STATUS_BAR_SIGNAL_ICONS,
            mSettingsObserver,
            UserHandle.USER_ALL
        );
    }

    private void updateSettings() {
        final boolean enabled = mSecureSettings.getIntForUser(
            Settings.Secure.COMBINED_STATUS_BAR_SIGNAL_ICONS,
            0,
            UserHandle.USER_CURRENT
        ) == 1;
        synchronized(mLock) {
            mIsCombinedSignalIcons = enabled;
        }
    }

    @Override
    public boolean isEnabled(BooleanFlag flag) {
        if (flag == COMBINED_STATUS_BAR_SIGNAL_ICONS) {
            synchronized(mLock) {
                return mIsCombinedSignalIcons;
            }
        } else {
            return super.isEnabled(flag);
        }
    }
}
