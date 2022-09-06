/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.flamingo.systemui.flags.dagger

import com.android.systemui.flags.FeatureFlags
import com.flamingo.systemui.flags.FlamingoFeatureFlags

import dagger.Binds
import dagger.Module

@Module
abstract class FlamingoFlagsModule {
    @Binds
    abstract fun bindsFlamingoFeatureFlag(impl: FlamingoFeatureFlags): FeatureFlags
}