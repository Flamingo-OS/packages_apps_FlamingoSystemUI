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
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
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

    private static final Icon sIcon = ResourceIcon.get(R.drawable.ic_qs_caffeine);

    private static final int[] DURATIONS = new int[] {
        5 * 60,   // 5 min
        10 * 60,  // 10 min
        30 * 60,  // 30 min
        -1,       // infinity
    };
    private static final int EGG_DURATION = 5 * 60 + 45; // 5 min 45 secs. Perfect.

    private final PowerManager.WakeLock mWakeLock;
    private final WakefulnessLifecycle mWakefulnessLifecycle;
    private final WakefulnessLifecycle.Observer mWakefulnessObserver =
        new WakefulnessLifecycle.Observer() {
            @Override
            public void onStartedGoingToSleep() {
                stopCountDown();
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
                refreshState();
            }
        };

    private CountDownTimer mCountdownTimer;
    private int mSecondsRemaining;
    private int mDuration;
    private long mLastClickTime = -1;

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
        stopCountDown();
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        super.handleDestroy();
    }

    @Override
    public void handleClick(@Nullable View view) {
        // If last user clicks < 5 seconds
        // we cycle different duration
        // otherwise toggle on/off
        if (mWakeLock.isHeld() && (mLastClickTime != -1) &&
                (SystemClock.elapsedRealtime() - mLastClickTime < 5000)) {
            // cycle duration
            mDuration++;
            if (mDuration >= DURATIONS.length) {
                // all durations cycled, turn it off
                mDuration = -1;
                stopCountDown();
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
            } else {
                // change duration
                startCountDown(DURATIONS[mDuration]);
                if (!mWakeLock.isHeld()) {
                    mWakeLock.acquire();
                }
            }
        } else {
            // toggle
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
                stopCountDown();
            } else {
                mWakeLock.acquire();
                mDuration = 0;
                startCountDown(DURATIONS[mDuration]);
            }
        }
        mLastClickTime = SystemClock.elapsedRealtime();
        refreshState();
    }

    @Override
    protected void handleLongClick(@Nullable View view) {
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
            startCountDown(EGG_DURATION);
        } else {
            mWakeLock.release();
            stopCountDown();
            // turn it off
            mDuration = -1;
        }
        mLastClickTime = SystemClock.elapsedRealtime();
        refreshState();
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_caffeine_label);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FLAMINGO;
    }

    private void startCountDown(long duration) {
        stopCountDown();
        mSecondsRemaining = (int)duration;
        if (duration == -1) {
            // infinity timing, no need to start timer
            return;
        }
        mCountdownTimer = new CountDownTimer(duration * 1000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                mSecondsRemaining = (int) (millisUntilFinished / 1000);
                refreshState();
            }

            @Override
            public void onFinish() {
                if (mWakeLock.isHeld())
                    mWakeLock.release();
                refreshState();
            }

        }.start();
    }

    private void stopCountDown() {
        if (mCountdownTimer != null) {
            mCountdownTimer.cancel();
            mCountdownTimer = null;
        }
    }

    private String formatValueWithRemainingTime() {
        if (mSecondsRemaining == -1) {
            return "\u221E"; // infinity
        }
        return String.format("%02d:%02d",
                        mSecondsRemaining / 60 % 60, mSecondsRemaining % 60);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.value = mWakeLock.isHeld();
        if (state.value) {
            state.secondaryLabel = formatValueWithRemainingTime();
            state.contentDescription =  mContext.getString(
                    R.string.accessibility_quick_settings_caffeine_on);
            state.state = Tile.STATE_ACTIVE;
        } else {
            state.secondaryLabel = mContext.getString(R.string.quick_settings_caffeine_label_off);
            state.contentDescription =  mContext.getString(
                    R.string.accessibility_quick_settings_caffeine_off);
            state.state = Tile.STATE_INACTIVE;
        }
    }
}
