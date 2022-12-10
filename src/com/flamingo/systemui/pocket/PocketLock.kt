/*
 * Copyright (C) 2016 The ParanoidAndroid Project
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

package com.flamingo.systemui.pocket

import android.animation.Animator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams

import com.android.systemui.R

/**
 * This class provides a fullscreen overlays view, displaying itself
 * even on top of lock screen. While this view is displaying touch
 * inputs are not passed to the the views below.
 * @see android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
 * @author Carlo Savignano
 */
class PocketLock(context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val view = LayoutInflater.from(context).inflate(R.layout.pocket_lock_view, null)
    private val lp = LayoutParams(
            LayoutParams.MATCH_PARENT /* width */,
            LayoutParams.MATCH_PARENT /* heiight */,
            LayoutParams.TYPE_SYSTEM_ERROR /* type */,
            /* flags */
            LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                LayoutParams.FLAG_HARDWARE_ACCELERATED or
                LayoutParams.FLAG_FULLSCREEN or
                LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT /* format */
        ).apply {
            gravity = Gravity.CENTER
            layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    
    private var isAttached = false
    private var isAnimating = false

    fun show(animate: Boolean) {
        handler.post({ showInternal(animate) })
    }

    private fun showInternal(animate: Boolean) {
        if (isAttached) {
            return
        }

        if (isAnimating) {
            view.animate().cancel()
        }

        if (!animate) {
            initAndAddView()
            return
        }

        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        view.animate()
            .alpha(1f)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animator: Animator) {
                    isAnimating = true
                }

                override fun onAnimationEnd(animator: Animator) {
                    view.setLayerType(View.LAYER_TYPE_NONE, null)
                    isAnimating = false
                }

                override fun onAnimationCancel(animator: Animator) {}
                override fun onAnimationRepeat(animator: Animator) {}
            })
            .withStartAction(::initAndAddView)
            .start()
    }

    private fun initAndAddView() {
        if (isAttached) {
            return
        }
        windowManager.addView(view, lp)
        view.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        isAttached = true
    }

    fun hide(animate: Boolean) {
        handler.post({ hideInternal(animate) })
    }

    private fun hideInternal(animate: Boolean) {
        if (!isAttached) {
            return
        }

        if (isAnimating) {
            view.animate().cancel()
        }

        if (!animate) {
            view.visibility = View.GONE
            view.alpha = 0f
            removeView()
            return
        }

        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        view.animate()
            .alpha(0f)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animator: Animator) {
                    isAnimating = true
                }

                override fun onAnimationEnd(animator: Animator) {
                    view.visibility = View.GONE
                    view.setLayerType(View.LAYER_TYPE_NONE, null)
                    isAnimating = false
                    removeView()
                }

                override fun onAnimationCancel(animator: Animator) {}
                override fun onAnimationRepeat(animator: Animator) {}
            })
            .start()
    }

    private fun removeView() {
        if (!isAttached) {
            return
        }
        if (isAnimating) {
            view.animate().cancel()
        }
        view.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        windowManager.removeView(view)
        isAnimating = false
        isAttached = false
    }
}
