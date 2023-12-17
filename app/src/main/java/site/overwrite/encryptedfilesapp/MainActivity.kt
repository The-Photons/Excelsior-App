package site.overwrite.encryptedfilesapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
//                    ListFiles({
//                        server.listFiles(
//                            "",
//                            { response -> Log.d("STATE", "Response is: $response") },
//                            { error -> Log.d("ERROR", "Error: $error") })
//                    })
                    ListFiles({
                        server.getFile(
                            "file1.txt",
                            { response -> Log.d("STATE", "Response is: $response") },
                            { error -> Log.d("ERROR", "Error: $error") }
                        )
                    })
                }
            }
        }
    }
}

@Composable
fun ListFiles(onClick: () -> Unit) {
//    var text by remember { mutableStateOf("") }
//
//    Column {
//        OutlinedTextField(
//            value = text,
//            onValueChange = { text = it },
//            label = { Text("Server Address") })
//        Button(onClick = onClick) {
//            Text("List Files")
//        }
//    }
    Button(onClick = onClick) {
        Text("Get File")
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    EncryptedFilesAppTheme {
        ListFiles({})
    }
}