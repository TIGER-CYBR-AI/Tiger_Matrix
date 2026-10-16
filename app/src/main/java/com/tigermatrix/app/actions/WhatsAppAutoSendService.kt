package com.tigermatrix.app.actions

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * خدمة إمكانية الوصول (اختيارية).
 * - محصورة بحزم واتساب فقط (بملف accessibility_config.xml).
 * - ما بتعمل شي إلا إذا AutoSendBridge مسلَّح (يعني أنت أمرت المساعد بإرسال رسالة هلق).
 * - كل شغلتها: تلاقي زر الإرسال وتضغطه. ما بتقرأ أي نص من المحادثات.
 */
class WhatsAppAutoSendService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var scheduled = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (!AutoSendBridge.isArmedFor(pkg)) return
        if (scheduled) return
        scheduled = true
        handler.postDelayed({
            scheduled = false
            trySend(pkg)
        }, 700L)
    }

    override fun onInterrupt() {}

    private fun trySend(pkg: String) {
        if (!AutoSendBridge.isArmedFor(pkg)) return
        try {
            val root = rootInActiveWindow ?: return
            if (root.packageName?.toString() != pkg) return
            val button = findSendButton(root, pkg) ?: return
            var target: AccessibilityNodeInfo? = button
            while (target != null && !target.isClickable) {
                target = target.parent
            }
            if (target != null && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                AutoSendBridge.markSent()
            }
        } catch (e: Exception) {
            // تجاهل
        }
    }

    private fun findSendButton(root: AccessibilityNodeInfo, pkg: String): AccessibilityNodeInfo? {
        val byId = root.findAccessibilityNodeInfosByViewId("$pkg:id/send")
        val enabled = byId?.firstOrNull { it.isEnabled }
        if (enabled != null) return enabled
        return findByDescription(root, 0)
    }

    private fun findByDescription(node: AccessibilityNodeInfo?, depth: Int): AccessibilityNodeInfo? {
        if (node == null || depth > 25) return null
        val d = node.contentDescription?.toString()?.trim()?.lowercase()
        if (d != null && (d == "send" || d == "إرسال" || d == "ارسال")) return node
        for (i in 0 until node.childCount) {
            val found = findByDescription(node.getChild(i), depth + 1)
            if (found != null) return found
        }
        return null
    }
}
