/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.flamingo.systemui.dagger;

import com.android.systemui.dagger.DefaultComponentBinder;
import com.android.systemui.dagger.DependencyProvider;
import com.android.systemui.dagger.SysUIComponent;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.SystemUIBinder;
import com.flamingo.systemui.LiveDisplayInitReceiver;
import com.google.android.systemui.smartspace.KeyguardSmartspaceController;

import dagger.Subcomponent;

@SysUISingleton
@Subcomponent(modules = {
        DefaultComponentBinder.class,
        DependencyProvider.class,
        SystemUIBinder.class,
        FlamingoServiceBinder.class,
        FlamingoSystemUIModule.class,
        FlamingoSystemUICoreStartableModule.class,
        FlamingoReferenceSystemUIModule.class})
public interface FlamingoSysUIComponent extends SysUIComponent {

    @SysUISingleton
    @Subcomponent.Builder
    interface Builder extends SysUIComponent.Builder {
        @Override
        FlamingoSysUIComponent build();
    }

    /**
     * Creates a KeyguardSmartspaceController.
     */
    @SysUISingleton
    KeyguardSmartspaceController createKeyguardSmartspaceController();

    @SysUISingleton
    LiveDisplayInitReceiver getLiveDisplayInitReceiver();
}