package site.overwrite.encryptedfilesapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import site.overwrite.encryptedfilesapp.src.Server
import site.overwrite.encryptedfilesapp.ui.theme.EncryptedFilesAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val server = Server(applicationContext, "192.168.80.142:5000")

        super.onCreate(savedInstanceState)
        setContent {
            EncryptedFilesAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ListFiles(server)
                }
            }
        }
    }
}

@Composable
fun ListFiles(server: Server?) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var filePath by remember { mutableStateOf("") }

//    Column {
//        OutlinedTextField(
//            value = text,
//            onValueChange = { text = it },
//            label = { Text("Server Address") })
//        Button(onClick = onClick) {
//            Text("List Files")
//        }
//    }
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier.padding(innerPadding),
        ) {
            OutlinedTextField(
                value = filePath,
                onValueChange = { filePath = it },
                label = { Text("File Path") })
            Button(onClick = {
                server?.getFile(
                    filePath,
                    { content -> Log.d("CONTENT", content.toString()) },
                    { status ->
                        run {
                            Log.d("FAILED REQUEST", status)
                            scope.launch { snackbarHostState.showSnackbar(status) }
                        }
                    },
                    { error ->
                        run {
                            Log.d("ERROR", error.toString())
                            scope.launch { snackbarHostState.showSnackbar(error.toString()) }
                        }
                    }
                )
            }) {
                Text("Get File")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    EncryptedFilesAppTheme {
        ListFiles(null)
    }
}