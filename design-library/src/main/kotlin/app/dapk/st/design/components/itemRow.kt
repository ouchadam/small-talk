package app.dapk.st.design.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TextRow(title: String, content: String? = null, includeDivider: Boolean = true, onClick: (() -> Unit)? = null, body: @Composable () -> Unit = {}) {
    val modifier = Modifier.padding(horizontal = 24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }) {
        Spacer(modifier = Modifier.height(24.dp))
        Column(modifier) {
            when (content) {
                null -> {
                    Text(text = title, fontSize = 18.sp)
                }
                else -> {
                    Text(text = title, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = content, fontSize = 18.sp)
                }
            }
            body()
            Spacer(modifier = Modifier.height(24.dp))
        }
        if (includeDivider) {
            Divider(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun IconRow(icon: ImageVector, title: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, fontSize = 18.sp)
    }
}

@Composable
fun SettingsTextRow(title: String, subtitle: String?, onClick: (() -> Unit)?) {
    TextRow(title = title, subtitle, includeDivider = false, onClick)
}
