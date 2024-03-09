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

package site.overwrite.encryptedfilesapp.ui.login

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.Dispatchers
import site.overwrite.encryptedfilesapp.data.DataStoreManager
import site.overwrite.encryptedfilesapp.ui.theme.EncryptedFilesAppTheme

class LoginActivity : ComponentActivity() {
    // Properties
    private val thisActivity = this

    private lateinit var dataStoreManager: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get things from the data store
        var serverURL by mutableStateOf("")
        var gotServerURL by mutableStateOf(false)

        var username by mutableStateOf("")
        var gotUsername by mutableStateOf(false)

        dataStoreManager = DataStoreManager(applicationContext)
        dataStoreManager.getServerURL().asLiveData(Dispatchers.Main)
            .observe(thisActivity) {
                serverURL = it
                gotServerURL = true
            }
        dataStoreManager.getUsername().asLiveData(Dispatchers.Main)
            .observe(thisActivity) {
                username = it
                gotUsername = true
            }

        // Then set the content
        setContent {
            EncryptedFilesAppTheme {
                if (gotUsername && gotServerURL) {
                    LoginForm(
                        serverURL = serverURL,
                        username = username
                    )
                }
            }
        }
    }
}
