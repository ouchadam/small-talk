package app.dapk.st.design.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    onNavigate: (() -> Unit)? = null,
    title: String? = null,
    offset: (Density.() -> IntOffset)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val navigationIcon = foo(onNavigate)

    SmallTopAppBar(
        modifier = Modifier.height(72.dp).run {
            if (offset == null) {
                this
            } else {
                this.offset(offset)
            }
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        navigationIcon = navigationIcon ?: {},
        title = title?.let {
            { Text(it, maxLines = 2) }
        } ?: {},
        actions = actions,
    )
    Divider(modifier = Modifier.fillMaxWidth(), color = Color.Black.copy(alpha = 0.2f), thickness = 0.5.dp)
}


private fun foo(onNavigate: (() -> Unit)?): (@Composable () -> Unit)? {
    return onNavigate?.let {
        { NavigationIcon(it) }
    }
}

@Composable
private fun NavigationIcon(onNavigate: () -> Unit) {
    IconButton(onClick = { onNavigate.invoke() }) {
        Icon(Icons.Default.ArrowBack, contentDescription = null)
    }
}