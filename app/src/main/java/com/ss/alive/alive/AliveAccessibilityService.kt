package com.ss.alive.alive

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.view.accessibility.AccessibilityWindowInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Optional helper used only to find visible clickable UI bounds for screen-sitting.
 * It never reads node text, descriptions, input values, or stores app content.
 */
class AliveAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 400
        }
        refreshVisibleBounds()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        refreshVisibleBounds()
    }

    override fun onInterrupt() {
        AlivePlatformRegistry.update(emptyList())
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        AlivePlatformRegistry.update(emptyList())
        return super.onUnbind(intent)
    }

    private fun refreshVisibleBounds() {
        val found = mutableListOf<Rect>()
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val homePackage = packageManager.resolveActivity(
            homeIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName

        // Platforms are allowed only on the currently selected launcher
        // (the Android home screen), never inside ordinary applications.
        if (homePackage.isNullOrBlank()) {
            AlivePlatformRegistry.update(emptyList())
            return
        }

        val screenArea = resources.displayMetrics.widthPixels.toLong() *
            resources.displayMetrics.heightPixels.toLong()
        windows?.forEach { window ->
            if (window.type != AccessibilityWindowInfo.TYPE_APPLICATION) return@forEach
            val root = window.root ?: return@forEach
            if (root.packageName?.toString() != homePackage) return@forEach

            val windowBounds = Rect()
            window.getBoundsInScreen(windowBounds)
            if (windowBounds.isEmpty) return@forEach
            collectClickableBounds(root, found, screenArea, windowBounds, 0)
        }
        AlivePlatformRegistry.update(found)
    }

    private fun collectClickableBounds(
        node: AccessibilityNodeInfo,
        output: MutableList<Rect>,
        screenArea: Long,
        windowBounds: Rect,
        depth: Int
    ) {
        if (depth > 14 || output.size >= 180) return
        if (node.isVisibleToUser && node.isClickable) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            if (bounds.intersect(windowBounds)) {
                val area = bounds.width().toLong() * bounds.height().toLong()
                if (bounds.width() >= 48 && bounds.height() >= 24 &&
                    area > 0 && area < screenArea * 0.65
                ) {
                    output += bounds
                }
            }
        }
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child ->
                collectClickableBounds(child, output, screenArea, windowBounds, depth + 1)
            }
        }
    }
}
