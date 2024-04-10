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
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
 * Data for an action button.
 *
 * @param Receiver Type of the receiver.
 * @property context Context to launch the intent of the action button.
 * @property name Name of the action to perform.
 * @property title Label of the action button.
 * @property actionReceiver Class that handles the message of the action button.
 * @property icon Icon for the action button. Defaults to no icon.
 * @property requestCode Request code for the action.
 * @property flags Any flags for the action.
 */
data class NotificationActionButtonData<Receiver : BroadcastReceiver>(
    val context: Context,
    val name: String,
    val title: String,
    val actionReceiver: Class<Receiver>,
    val icon: Int = 0,
    val requestCode: Int = 0,
    val flags: Int = 0
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
class NotificationCreator(
    private val context: Context,
    private val channelData: NotificationChannelData
) {
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var notificationData: NotificationData? = null

    val notification: Notification?
        get() {
            if (notificationBuilder == null) return null
            return notificationBuilder!!.build()
        }

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
     * Builds a new notification.
     *
     * @param notificationData Data of the notification.
     * @return Builder for the notification.
     */
    fun buildNotification(notificationData: NotificationData) {
        this.notificationData = notificationData
        notificationBuilder = NotificationCompat.Builder(context, channelData.id)
            .setSmallIcon(notificationData.icon)
            .setContentTitle(notificationData.title)
            .setContentText(notificationData.text)
            .setPriority(notificationData.priority)
            .setOngoing(notificationData.isPersistent)
    }

    /**
     * Adds an action button to the notification.
     *
     * @param Receiver Type of the receiver.
     * @param actionButtonData Action button data.
     */
    fun <Receiver : BroadcastReceiver> addActionButton(
        actionButtonData: NotificationActionButtonData<Receiver>
    ) {
        if (notificationBuilder == null) {
            throw Error("Need to build notification first!")
        }

        val actionIntent = Intent(
            actionButtonData.context,
            actionButtonData.actionReceiver
        ).apply {
            action = actionButtonData.name
        }
        val actionPendingIntent = PendingIntent.getBroadcast(
            actionButtonData.context,
            actionButtonData.requestCode,
            actionIntent,
            actionButtonData.flags
        )

        notificationBuilder!!.addAction(
            actionButtonData.icon,
            actionButtonData.title,
            actionPendingIntent
        )
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
