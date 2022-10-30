package app.dapk.st.design.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    onNavigate: (() -> Unit)? = null,
    title: String? = null,
    offset: (Density.() -> IntOffset)? = null,
    color: Color = MaterialTheme.colorScheme.background,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val navigationIcon = foo(onNavigate)
    TopAppBar(
        modifier = offset?.let { Modifier.offset(it) } ?: Modifier,
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = color,
        ),
        navigationIcon = navigationIcon,
        title = title?.let { { Text(it, maxLines = 2) } } ?: {},
        actions = actions,
    )
}

private fun foo(onNavigate: (() -> Unit)?): (@Composable () -> Unit) {
    return onNavigate?.let {
        { NavigationIcon(it) }
    } ?: {}
}

@Composable
private fun NavigationIcon(onNavigate: () -> Unit) {
    IconButton(onClick = { onNavigate.invoke() }) {
        Icon(Icons.Default.ArrowBack, contentDescription = null)
    }
}