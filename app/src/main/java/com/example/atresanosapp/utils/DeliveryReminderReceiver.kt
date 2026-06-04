package com.example.atresanosapp.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DeliveryReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pedidoId = intent.getStringExtra("pedidoId") ?: return
        
        NotificationHelper.createNotificationChannel(context)
        NotificationHelper.showNotification(
            context,
            "¡Tu entrega está cerca!",
            "Tu pedido #${pedidoId.takeLast(6).uppercase()} está programado para entregarse pronto.",
            pedidoId.hashCode()
        )
    }
}
