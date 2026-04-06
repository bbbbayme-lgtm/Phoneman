package com.bot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class BotAccessibilityService : AccessibilityService() {

    private val client = OkHttpClient()
    private val backendUrl = "https://your-backend.onrender.com"
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return
        val screenText = extractText(rootNode)

        scope.launch {
            val action = reasonAboutScreen(screenText)
            executeAction(action)
        }
    }

    private fun extractText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        if (node.text != null) sb.appendLine(node.text)
        if (node.contentDescription != null) sb.appendLine(node.contentDescription)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { sb.append(extractText(it)) }
        }
        return sb.toString()
    }

    private suspend fun reasonAboutScreen(screenText: String): JSONObject {
        val json = JSONObject().put("screen_text", screenText)
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("$backendUrl/reason").post(body).build()
        val response = client.newCall(request).execute()
        return JSONObject(response.body?.string() ?: "{}")
    }

    private fun executeAction(action: JSONObject) {
        val actionStr = action.optString("action", "")
        val parsed = runCatching { JSONObject(actionStr) }.getOrNull() ?: return

        when (parsed.optString("type")) {
            "click"  -> clickNode(parsed.optString("target"))
            "type"   -> typeText(parsed.optString("target"), parsed.optString("value"))
            "scroll" -> performScrollGesture()
            "swipe"  -> performSwipeGesture()
        }
    }

    private fun clickNode(targetText: String) {
        val root = rootInActiveWindow ?: return
        findNodeByText(root, targetText)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun typeText(targetText: String, value: String) {
        val root = rootInActiveWindow ?: return
        val node = findNodeByText(root, targetText) ?: return
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = android.os.Bundle().apply {
            putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true) return node
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { found -> findNodeByText(found, text)?.let { return it } }
        }
        return null
    }

    private fun performScrollGesture() {
        val path = Path().apply { moveTo(540f, 1500f); lineTo(540f, 500f) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 300)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    private fun performSwipeGesture() {
        val path = Path().apply { moveTo(900f, 1000f); lineTo(100f, 1000f) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 300)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    override fun onInterrupt() { scope.cancel() }
}
