package com.alq.bubbleoverlay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.alq.bubbleoverlay.R

object NotificationHelper {
    private const val CHANNEL_ID = "bubble_channel"
    private const val CHANNEL_NAME = "Burbujas Activas"

    fun createChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) { // <- Verificar si ya existe
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controla la visualización de burbujas flotantes"
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun buildNotification(context: Context): Notification =
        Notification.Builder(context, CHANNEL_ID)
            .setContentTitle("Toca para administrar burbujas")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
}
