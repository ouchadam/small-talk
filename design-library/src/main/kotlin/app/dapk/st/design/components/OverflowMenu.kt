package app.dapk.st.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Composable
fun OverflowMenu(content: @Composable () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(0.dp, (-72).dp),
            modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            content()
        }
        IconButton(onClick = {
            showMenu = !showMenu
        }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = null,
            )
        }
    }
}

@Composable
fun BooleanOption(value: Boolean, trueText: String, falseText: String, onClick: (Boolean) -> Unit) {
    val itemTextColor = MaterialTheme.colorScheme.onSecondaryContainer
    when (value) {
        true -> DropdownMenuItem(text = { Text(trueText, color = itemTextColor) }, onClick = { onClick(true) })
        false -> DropdownMenuItem(text = { Text(falseText, color = itemTextColor) }, onClick = { onClick(false) })
    }
}