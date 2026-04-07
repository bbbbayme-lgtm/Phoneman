package com.bot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class BotAccessibilityService : AccessibilityService() {

    // 🔧 Swap this in once Render gives you the URL
    private val BACKEND_URL = "https://YOUR_RENDER_URL.onrender.com"

    private val http = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return
        val screenText = extractText(rootNode)
        if (screenText.isBlank()) return

        scope.launch {
            try {
                val action = reasonAboutScreen(screenText)
                withContext(Dispatchers.Main) {
                    executeAction(action)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun extractText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        if (!node.text.isNullOrBlank()) sb.appendLine(node.text)
        if (!node.contentDescription.isNullOrBlank()) sb.appendLine(node.contentDescription)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { sb.append(extractText(it)) }
        }
        return sb.toString()
    }

    private fun reasonAboutScreen(screenText: String): JSONObject {
        val body = JSONObject().put("screen_text", screenText)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BACKEND_URL/reason")
            .post(body)
            .build()

        val response = http.newCall(request).execute()
        val raw = response.body?.string() ?: "{}"
        val outer = JSONObject(raw)
        return JSONObject(outer.getString("action"))
    }

    private fun executeAction(action: JSONObject) {
        when (action.optString("type")) {
            "click"  -> clickOnText(action.optString("target"))
            "type"   -> typeText(action.optString("value"))
            "scroll" -> scrollScreen()
            "swipe"  -> swipeScreen()
        }
    }

    private fun clickOnText(target: String) {
        val root = rootInActiveWindow ?: return
        root.findAccessibilityNodeInfosByText(target)
            ?.firstOrNull()
            ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun typeText(text: String) {
        val root = rootInActiveWindow ?: return
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return
        val args = Bundle()
        args.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text
        )
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun scrollScreen() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun swipeScreen() {
        val path = Path().apply {
            moveTo(500f, 1500f)
            lineTo(500f, 500f)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        dispatchGesture(gesture, null, null)
    }

    override fun onInterrupt() {
        scope.cancel()
    }
}
