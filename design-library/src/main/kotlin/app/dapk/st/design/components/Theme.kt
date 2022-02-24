package app.dapk.st.design.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlin.math.absoluteValue

private object Palette {
    val brandPrimary = Color(0xFFb41cca)
}

private val DARK_COLOURS = darkColors(
    primary = Palette.brandPrimary,
    onPrimary = Color(0xDDFFFFFF),
)

private val LIGHT_COLOURS = DARK_COLOURS

private val DARK_EXTENDED = ExtendedColors(
    selfBubble = DARK_COLOURS.primary,
    onSelfBubble = DARK_COLOURS.onPrimary,
    othersBubble = Color(0x20EDEDED),
    onOthersBubble = Color(0xFF000000),
    missingImageColors = listOf(
        Color(0xFFf7c7f7) to Color(0xFFdf20de),
        Color(0xFFe5d7f6) to Color(0xFF7b30cf),
        Color(0xFFf6c8cb) to Color(0xFFda2535),
    )
)
private val LIGHT_EXTENDED = DARK_EXTENDED

@Immutable
data class ExtendedColors(
    val selfBubble: Color,
    val onSelfBubble: Color,
    val othersBubble: Color,
    val onOthersBubble: Color,
    val missingImageColors: List<Pair<Color, Color>>,
) {
    fun getMissingImageColor(key: String): Pair<Color, Color> {
        return missingImageColors[key.hashCode().absoluteValue % (missingImageColors.size)]
    }
}

private val LocalExtendedColors = staticCompositionLocalOf { LIGHT_EXTENDED }

@Composable
fun SmallTalkTheme(content: @Composable () -> Unit) {
    val systemUiController = rememberSystemUiController()
    val systemInDarkTheme = isSystemInDarkTheme()
    MaterialTheme(
        colors = if (systemInDarkTheme) DARK_COLOURS else LIGHT_COLOURS,
    ) {
        val backgroundColor = MaterialTheme.colors.background
        SideEffect {
            systemUiController.setSystemBarsColor(backgroundColor)
        }
        CompositionLocalProvider(LocalExtendedColors provides if (systemInDarkTheme) DARK_EXTENDED else LIGHT_EXTENDED) {
            content()
        }
    }
}

object SmallTalkTheme {
    val extendedColors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}