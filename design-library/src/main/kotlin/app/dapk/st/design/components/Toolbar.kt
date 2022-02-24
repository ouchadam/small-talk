package app.dapk.st.design.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ColumnScope.Toolbar(onNavigate: () -> Unit, title: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        modifier = Modifier.height(72.dp),
        backgroundColor = Color.Transparent,
        navigationIcon = {
            IconButton(onClick = { onNavigate() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
        },
        title = title?.let {
            { Text(it, maxLines = 2) }
        } ?: {},
        actions = actions,
        elevation = 0.dp
    )
    Divider(modifier = Modifier.fillMaxWidth(), color = Color.Black.copy(alpha = 0.2f), thickness = 0.5.dp)
}