/*
 * Copyright (c) 2023-2024 PhotonicGluon.
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

package site.overwrite.encryptedfilesapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class DataStoreManager(private val context: Context) {
    // Properties and keys
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

        val serverURLKey = stringPreferencesKey("server_url")
        val usernameKey = stringPreferencesKey("username")
    }

    // Helper methods
    /**
     * Gets the preferences from the data store.
     *
     * @return Preferences flow.
     */
    private fun getPreferences(): Flow<Preferences> {
        return context.dataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
    }

    // Getters
    /**
     * Gets the server URL from the data store.
     *
     * @return Server URL as a flow string.
     */
    fun getServerURL(): Flow<String> {
        return getPreferences().map { preferences ->
            val serverURL = preferences[serverURLKey] ?: ""
            serverURL
        }
    }

    /**
     * Gets the username from the data store.
     *
     * @return Username as a flow string.
     */
    fun getUsername(): Flow<String> {
        return getPreferences().map { preferences ->
            val username = preferences[usernameKey] ?: ""
            username
        }
    }

    // Setters
    /**
     * Sets the server URL in the data store.
     *
     * @param serverURL Server URL to set.
     */
    suspend fun setServerURL(serverURL: String) {
        context.dataStore.edit { preferences ->
            preferences[serverURLKey] = serverURL
        }
    }

    /**
     * Sets the username in the data store.
     *
     * @param username Username to set.
     */
    suspend fun setUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[usernameKey] = username
        }
    }
}