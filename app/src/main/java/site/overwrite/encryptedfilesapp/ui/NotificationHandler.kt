/*
 * Copyright (c) 2024 PhotonicGluon.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package site.overwrite.encryptedfilesapp.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import site.overwrite.encryptedfilesapp.R

/**
 * Data for a notification channel.
 *
 * @property id Channel to create the notification for.
 * @property name Name for the notifications channel.
 * @property desc Description of the notification channel.
 * @property importance Importance level of the channel.
 */
data class NotificationChannelData(
    val id: String,
    val name: String,
    val desc: String,
    val importance: Int = NotificationManager.IMPORTANCE_DEFAULT
)

/**
 * Data for a notification.
 *
 * @param id Notification ID.
 * @param title Title of the notification.
 * @param text Content text inside the notification.
 * @param icon Icon to show in the notification.
 * @param priority Priority of the notification.
 * @param isPersistent Whether the notification is persistent or not.
 */
// TODO: Also add actions that can be performed when the notification is tapped/expanded
data class NotificationData(
    val id: Int,
    val title: String,
    val text: String,
    val icon: Int = R.drawable.ic_launcher_foreground,  // TODO: Use proper notification icon
    val priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    val isPersistent: Boolean = false
)

/**
 * Class for creating a notification within a notification channel.
 *
 * @property context Context of the notification.
 * @property channelData Notification channel data.
 */
class NotificationHandler(
    private val context: Context,
    private val channelData: NotificationChannelData
) {
    private var notificationData: NotificationData? = null

    init {
        val channel = NotificationChannel(
            channelData.id,
            channelData.name,
            channelData.importance
        ).apply {
            description = channelData.desc
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        Log.d("NOTIFICATIONS", "Created new channel '${channelData.name}' (id: ${channelData.id})")
    }

    /**
     * Creates a new notification.
     *
     * @param notificationData Data of the notification.
     * @return The built notification.
     */
    fun createNotification(notificationData: NotificationData): Notification {
        this.notificationData = notificationData

        val builder = NotificationCompat.Builder(context, channelData.id)
            .setSmallIcon(notificationData.icon)
            .setContentTitle(notificationData.title)
            .setContentText(notificationData.text)
            .setPriority(notificationData.priority)
            .setOngoing(notificationData.isPersistent)
        return builder.build()
    }

    fun cancelNotification() {
        if (notificationData != null) {
            NotificationManagerCompat.from(context).cancel(notificationData!!.id)
        }
        notificationData = null
    }

    fun deleteChannel() {
        NotificationManagerCompat.from(context).deleteNotificationChannel(channelData.id)
        Log.d("NOTIFICATIONS", "Deleted channel '${channelData.name}' (id: ${channelData.id})")
    }
}
