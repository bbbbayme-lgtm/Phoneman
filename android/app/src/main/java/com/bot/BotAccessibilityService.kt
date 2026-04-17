package com.bot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class BotAccessibilityService : AccessibilityService() {

    private val httpClient = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val renderUrl = "https://phone-man-idi3.onrender.com"
    var currentTask: String = "Monitor and fix screen issues"

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return
        val screenText = buildString {
            fun scrape(node: AccessibilityNodeInfo) {
                if (!node.text.isNullOrBlank()) appendLine(node.text)
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { scrape(it) }
                }
            }
            scrape(rootNode)
        }

        scope.launch {
            try {
                val body = JSONObject().apply {
                    put("screen_text", screenText)
                    put("task", currentTask)
                }.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$renderUrl/reason")
                    .post(body)
                    .build()

                val resp = httpClient.newCall(request).execute()
                val responseBody = resp.body?.string() ?: return@launch
                val actionJson = JSONObject(JSONObject(responseBody).getString("action"))

                withContext(Dispatchers.Main) {
                    when (actionJson.getString("type")) {
                        "tap" -> performTap(actionJson.getDouble("x").toFloat(), actionJson.getDouble("y").toFloat())
                        "type" -> performType(actionJson.getString("text"))
                        "call" -> makeCall(actionJson.getString("number"))
                        "sms" -> sendSms(actionJson.getString("number"), actionJson.getString("text"))
                    }
                }
            } catch (e: Exception) {
                Log.e("bizzy", "Processing loop error: ${e.message}")
            }
        }
    }

    private fun performTap(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun performType(text: String) {
        val args = Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        rootInActiveWindow?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun makeCall(number: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$number")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun sendSms(number: String, message: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$number")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}