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

package site.overwrite.encryptedfilesapp.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import site.overwrite.encryptedfilesapp.io.Server

@Composable
fun HomeScreen(
    server: Server? = null,
    username: String = "",
    password: String = "",
    homeViewModel: HomeViewModel = viewModel()
) {
    val homeViewUIState by homeViewModel.uiState.collectAsState()

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp)
    ) {
        Text("Hello ${homeViewUIState.username}!")
        Text("Using server ${homeViewUIState.server.serverURL}.")
        if (homeViewModel.loggedIn) {
            Text("You are logged in!")
        } else {
            Text("Logging you in...")
        }
    }

    // On first showing, login
    LaunchedEffect(Unit) {
        if (server != null) {
            homeViewModel.loginToServer(
                server = server,
                username = username,
                password = password
            )
        }
    }
}
