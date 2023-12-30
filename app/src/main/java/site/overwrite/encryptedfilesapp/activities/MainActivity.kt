/*
 * Copyright (c) 2023 Kan Onn Kit.
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

package site.overwrite.encryptedfilesapp.activities

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.launch
import org.json.JSONArray
import site.overwrite.encryptedfilesapp.src.Cryptography
import site.overwrite.encryptedfilesapp.src.Server
import site.overwrite.encryptedfilesapp.src.decodeHex
import site.overwrite.encryptedfilesapp.ui.theme.EncryptedFilesAppTheme

// CONSTANTS
const val PREVIOUS_DIRECTORY_TEXT_LABEL = "Previous Directory"
const val PREVIOUS_DIRECTORY_TYPE = "prev-dir"

// MAIN ACTIVITY
class MainActivity : ComponentActivity() {
    // Properties
    private lateinit var server: Server
    private lateinit var loginIntent: Intent

    private lateinit var encryptionIV: String
    private lateinit var encryptionSalt: String
    private lateinit var encryptionKey: ByteArray

    // Overridden functions
    @OptIn(ExperimentalStdlibApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("MAIN", "Main activity onCreate")
        super.onCreate(savedInstanceState)

        // TODO: Remove and Uncomment Below
        val queue = Volley.newRequestQueue(applicationContext)
        server = Server(queue, "http://192.168.80.142:5000")
        encryptionIV = "encryptionIntVec"
        encryptionSalt = "someSalt12345678"
        encryptionKey = String(
            Cryptography.decryptAES(
                "UXMMpaGD1SJ3ZATBuJnt7I3MWYHzsVFURgo0tKDg5aOoP16mmDPal/8GmsqvXXkohZkf7SxRorWXe9qcIW+rmAA5niaqZeI2nvAuSrmztRg=",
                Cryptography.genAESKey("password", encryptionSalt),
                encryptionIV
            )
        ).decodeHex()
//        // We first need to ask for the login details, especially the encryption key
//        loginIntent = Intent(this, LoginActivity::class.java);
//        val getLoginCredentials =
//            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//                if (result.resultCode == Activity.RESULT_OK) {
//                    // Retrieve data from the result intent
//                    val resultIntent: Intent? = result.data
//                    val serverURL: String = resultIntent?.getStringExtra("server_url") ?: ""
//                    encryptionIV = resultIntent?.getStringExtra("iv") ?: ""
//                    encryptionSalt = resultIntent?.getStringExtra("salt") ?: ""
//                    encryptionKey =
//                        resultIntent?.getByteArrayExtra("encryption_key") ?: ByteArray(0)
//                    Log.d(
//                        "MAIN",
//                        "Got URL '$serverURL', IV '$encryptionIV', salt '$encryptionSalt', " +
//                                "and encryption key (as hex string) '${encryptionKey.toHexString()}'"
//                    )
//
//                    // Now initialize the things needed
//                    val queue = Volley.newRequestQueue(applicationContext)
//                    server = Server(queue, serverURL)
//                }
//            }
//        getLoginCredentials.launch(loginIntent)

        setContent {
            EncryptedFilesAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FilesList()
                }
            }
        }
    }

    // Composables
    @Composable
    fun TextInputDialog(
        onDismissRequest: () -> Unit,
        onConfirmation: (String) -> Unit,
        dialogTitle: String,
        textFieldLabel: String,
        textFieldPlaceholder: String = "",
        icon: ImageVector? = null,
        iconDesc: String? = null,
        singleLine: Boolean = true,
    ) {
        // Attributes
        var text by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }

        AlertDialog(
            icon = {
                if (icon != null) {
                    Icon(icon, iconDesc)
                }
            },
            title = {
                Text(text = dialogTitle)
            },
            text = {
                TextField(
                    modifier = Modifier.focusRequester(focusRequester),
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(textFieldLabel) },
                    placeholder = { Text(textFieldPlaceholder) },
                    singleLine = singleLine
                )
            },
            onDismissRequest = {
                onDismissRequest()
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmation(text)
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismissRequest()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    /**
     * List of files.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @Preview
    fun FilesList() {
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        var dirPath by remember { mutableStateOf("") }
        var prevDir by remember { mutableStateOf("") }
        var dirItems by remember { mutableStateOf(JSONArray()) }

        var isLoadingFiles by remember { mutableStateOf(false) }

        // Helper functions
        fun processSuccessfulResponse(encryptedFileContent: String) {
            // Decrypt the data
            val fileData =
                Cryptography.decryptAES(encryptedFileContent, encryptionKey, encryptionIV)

            // TODO: Continue
            Log.d("MAIN", "File data: ${String(fileData)}")
            scope.launch { snackbarHostState.showSnackbar(String(fileData)) }
        }

        fun getFile(rawPath: String) {
            val path = rawPath.trimStart('/')
            Log.d("MAIN", "Getting file: '$path'")
            server.getFile(
                path,
                { json ->
                    run {
                        val rawBase64Content = json.getString("content")
                        val innerContent = String(Base64.decode(rawBase64Content, Base64.DEFAULT))
                        Log.d("MAIN", "File content: $innerContent")
                        processSuccessfulResponse(innerContent)
                    }
                },
                { status, _ ->
                    run {
                        Log.d("MAIN", "Failed file request: $status")
                        scope.launch { snackbarHostState.showSnackbar(status) }
                    }
                },
                { error ->
                    run {
                        Log.d("MAIN", "File request had error: $error")
                        // Todo: allow retry
                        scope.launch { snackbarHostState.showSnackbar(error.message.toString()) }
                    }
                }
            )
        }

        fun getItemsInDir() {
            isLoadingFiles = true
            val dir = dirPath.trimStart('/')
            Log.d("MAIN", "Getting items in directory: '$dir'")
            server.listFiles(
                dir,
                { json ->
                    run {
                        val itemsInDirStr = json.getString("content")

                        // If the items is null, then the directory does not exist
                        if (itemsInDirStr == "null") {
                            dirItems = JSONArray()
                        } else {
                            dirItems = JSONArray(itemsInDirStr)
                            Log.d("MAIN", "Found ${dirItems.length()} items in directory")
                        }
                        isLoadingFiles = false
                    }
                },
                { status, _ ->
                    run {
                        Log.d("MAIN", "Failed to list items in directory")
                        scope.launch { snackbarHostState.showSnackbar("Failed to get things in directory: $status") }
                        isLoadingFiles = false
                    }
                },
                { error ->
                    run {
                        Log.d("MAIN", "Error when getting items in directory: $error")
                        dirItems = JSONArray()
                        // Todo: allow retry
                        scope.launch { snackbarHostState.showSnackbar(error.message.toString()) }
                        isLoadingFiles = false
                    }
                }
            )
        }

        // Helper composables
        /**
         * Creates a directory item on the screen.
         *
         * @param name Name of the item in the directory.
         * @param type Type of the item in the directory.
         * @param sizeString (Nicely processed) size of the item.
         */
        @Composable
        fun DirectoryItem(name: String, type: String, sizeString: String) {
            // Attributes
            val isPreviousDirectoryItem = type == PREVIOUS_DIRECTORY_TYPE

            val context = LocalContext.current
            var expanded by remember { mutableStateOf(false) }

            TextButton(
                shape = RoundedCornerShape(0),
                onClick = {
                    Log.d("MAIN", "Clicked on $type named '$name'")
                    if (type == "file") {
                        getFile("$dirPath/$name")
                    } else {
                        if (isPreviousDirectoryItem) {
                            dirPath = prevDir
                            val split = prevDir.split("/")
                            prevDir = split.subList(0, split.size - 1).joinToString("/")
                        } else {
                            prevDir = dirPath
                            dirPath = "$dirPath/$name"
                        }
                        Log.d("MAIN", "Dir path: '$dirPath'; Prev dir: '$prevDir'")
                        getItemsInDir()
                    }
                }
            ) {
                // Get the correct icon and description to display
                val icon: ImageVector
                val description: String
                when (type) {
                    "file" -> {
                        icon = Icons.Filled.InsertDriveFile
                        description = "File"
                    }

                    "directory" -> {
                        icon = Icons.Filled.Folder
                        description = "Folder"
                    }

                    PREVIOUS_DIRECTORY_TYPE -> {
                        icon = Icons.Filled.ArrowBack
                        description = "Back"
                    }

                    else -> {
                        icon = Icons.Filled.QuestionMark
                        description = "Unknown"
                    }
                }

                Row {
                    if (isPreviousDirectoryItem) {
                        Spacer(Modifier.size(24.dp))
                    } else {
                        // TODO: Implement syncing
                        Icon(Icons.Outlined.Cloud, "Unsynced", modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.size(10.dp))
                    Icon(icon, description)
                    Spacer(Modifier.size(4.dp))
                    Text(name)
                    Spacer(Modifier.weight(1f))
                    Text(sizeString)
                    Spacer(Modifier.size(4.dp))
                    Box {
                        if (isPreviousDirectoryItem) {
                            Spacer(Modifier.size(24.dp))
                        } else {
                            IconButton(
                                modifier = Modifier.size(24.dp),
                                onClick = { expanded = !expanded }
                            ) {
                                Icon(Icons.Filled.MoreVert, "More")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sync") },
                                    onClick = {
                                        Toast.makeText(context, "Sync", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete From Server") },
                                    onClick = {
                                        Toast.makeText(
                                            context,
                                            "Delete From Server",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        /**
         * Adds the item action button.
         */
        @Composable
        fun AddItemActionButton() {
            // Attributes
            val context = LocalContext.current
            var expanded by remember { mutableStateOf(false) }

            var showFolderNameInputDialog by remember { mutableStateOf(false) }

            if (!isLoadingFiles) {
                FloatingActionButton(
                    modifier = Modifier.padding(all = 16.dp),
                    onClick = { expanded = !expanded },
                ) {
                    Icon(Icons.Filled.Add, "Add File/Folder")
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add File") },
                            onClick = {
                                Toast.makeText(context, "Add File", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Folder") },
                            onClick = { showFolderNameInputDialog = true }
                        )
                    }
                }

                if (showFolderNameInputDialog) {
                    TextInputDialog(
                        onDismissRequest = { showFolderNameInputDialog = false },
                        onConfirmation = { folderName ->
                            run {
                                Log.d("MAIN", "Request for new folder: $folderName")
                                showFolderNameInputDialog = false

                                val fullFolderPath = "$dirPath/$folderName".trimStart('/')

                                server.createFolder(
                                    fullFolderPath,
                                    { _ ->
                                        run {
                                            Log.d("MAIN", "New folder created: $fullFolderPath")
                                            scope.launch { snackbarHostState.showSnackbar("Directory created") }
                                            getItemsInDir()
                                        }
                                    },
                                    { _, json ->
                                        run {
                                            Log.d("MAIN", "Failed to create folder")
                                            val reason = json.getString("message")
                                            scope.launch { snackbarHostState.showSnackbar("Failed to create folder: $reason") }
                                        }
                                    },
                                    { error ->
                                        run {
                                            Log.d("MAIN", "Error when making folder: $error")
                                            scope.launch { snackbarHostState.showSnackbar(error.message.toString()) }
                                        }
                                    }
                                )
                            }
                        },
                        dialogTitle = "Enter Folder Name",
                        textFieldLabel = "Name",
                        textFieldPlaceholder = "Name of the folder"
                    )
                }
            }
        }

        // Main UI
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text("Files (${if (dirPath != "") dirPath else "/"})")
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            floatingActionButton = { AddItemActionButton() },
            floatingActionButtonPosition = FabPosition.End
        ) { innerPadding ->
            if (isLoadingFiles) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(48.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            } else {
                Column(modifier = Modifier.padding(innerPadding)) {
                    if (dirPath != "") {
                        DirectoryItem(PREVIOUS_DIRECTORY_TEXT_LABEL, PREVIOUS_DIRECTORY_TYPE, "")
                    }
                    for (i in 0..<dirItems.length()) {
                        // Get the specific item
                        val item = dirItems.getJSONObject(i)
                        val name = item.getString("name")
                        val type = item.getString("type")
                        val size = item.getString("size")

                        // Create a button with that icon
                        DirectoryItem(name, type, size)
                    }
                }
            }
        }

        // Now display the main directory
        getItemsInDir()
    }
}
