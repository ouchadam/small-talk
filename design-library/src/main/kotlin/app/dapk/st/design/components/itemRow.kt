package app.dapk.st.design.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TextRow(
    title: String,
    content: String? = null,
    includeDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    body: @Composable () -> Unit = {}
) {
    val verticalPadding = 24.dp
    val modifier = Modifier.padding(horizontal = 24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }) {
        Spacer(modifier = Modifier.height(verticalPadding))
        Column(modifier) {
            val textModifier = when (enabled) {
                true -> Modifier
                false -> Modifier.alpha(0.5f)
            }
            when (content) {
                null -> {
                    Text(text = title, fontSize = 18.sp, modifier = textModifier)
                }

                else -> {
                    Text(text = title, fontSize = 12.sp, modifier = textModifier)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = content, fontSize = 18.sp, modifier = textModifier)
                }
            }
            body()
            Spacer(modifier = Modifier.height(verticalPadding))
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
fun SettingsTextRow(title: String, subtitle: String?, onClick: (() -> Unit)?, enabled: Boolean) {
    TextRow(title = title, subtitle, includeDivider = false, onClick, enabled = enabled)
}

@Composable
fun SettingsToggleRow(title: String, subtitle: String?, state: Boolean, onToggle: () -> Unit) {
    Toggle(title, subtitle, state, onToggle)
}

@Composable
private fun Toggle(title: String, subtitle: String?, state: Boolean, onToggle: () -> Unit) {
    val verticalPadding = 16.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if  (subtitle == null) {
            Text(text = title)
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title)
                Spacer(Modifier.height(4.dp))
                Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
            }
        }
        Switch(
            modifier = Modifier.wrapContentWidth(),
            checked = state,
            onCheckedChange = { onToggle() }
        )
    }
}

