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

package site.overwrite.encryptedfilesapp.ui.home.services

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import site.overwrite.encryptedfilesapp.data.Credentials
import site.overwrite.encryptedfilesapp.serializable
import site.overwrite.encryptedfilesapp.ui.NotificationActionButtonData
import site.overwrite.encryptedfilesapp.ui.NotificationChannelData
import site.overwrite.encryptedfilesapp.ui.NotificationCreator
import site.overwrite.encryptedfilesapp.ui.NotificationData
import site.overwrite.encryptedfilesapp.ui.home.HomeActivity

class CloseAppReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("CLOSE-APP-RECEIVER", "Received request to close app")

        val shutdownIntent = Intent(context, HomeActivity::class.java)
        shutdownIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        shutdownIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        shutdownIntent.putExtra("close_app",true)
        context.startActivity(shutdownIntent)
    }
}

class AppIsActiveService : Service() {
    private lateinit var persistentNotificationCreator: NotificationCreator

    override fun onCreate() {
        super.onCreate()
        Log.d("APP-IS-ACTIVE-SERVICE", "onCreate called")

        persistentNotificationCreator = NotificationCreator(
            this,
            NotificationChannelData(
                id = "Excelsior-AppIsActive",
                name = "App is Active",
                desc = "Notification channel for showing that Excelsior is active",
                importance = NotificationManager.IMPORTANCE_LOW
            ),
        )
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d("APP-IS-ACTIVE-SERVICE", "onStartCommand called")

        val credentials: Credentials = intent.serializable("credentials")!!
        val notificationData = NotificationData(
            id = 1,
            title = "Excelsior is running",
            text = "Connected to ${credentials.serverURL} as ${credentials.username}",
            isPersistent = true
        )
        persistentNotificationCreator.buildNotification(notificationData)
        persistentNotificationCreator.addActionButton(
            NotificationActionButtonData(
                this,
                "ACTION_LOGOUT",  // TODO: Is this correct?,
                "Logout",
                CloseAppReceiver::class.java,
            )
        )

        ServiceCompat.startForeground(
            this,
            notificationData.id,
            persistentNotificationCreator.notification!!,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.d("APP-IS-ACTIVE-SERVICE", "onDestroy called")
        persistentNotificationCreator.cancelNotification()
        persistentNotificationCreator.deleteChannel()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null  // We are not providing anything
    }
}