package app.dapk.st.design.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GenericError(message: String = "Something went wrong...", label: String = "Retry", cause: Throwable? = null, action: () -> Unit) {
    val moreDetails = cause?.let { "${it::class.java.simpleName}: ${it.message}" }

    val openDetailsDialog = remember { mutableStateOf(false) }
    if (openDetailsDialog.value) {
        AlertDialog(
            onDismissRequest = { openDetailsDialog.value = false },
            confirmButton = {
                Button(onClick = { openDetailsDialog.value = false }) {
                    Text("OK")
                }
            },
            title = { Text("Details") },
            text = {
                Text(moreDetails!!)
            }
        )
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message)
            if (moreDetails != null) {
                Text("Tap for more details".uppercase(), fontSize = 12.sp, modifier = Modifier.clickable { openDetailsDialog.value = true }.padding(12.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { action() }) {
                Text(label.uppercase())
            }
        }
    }
}