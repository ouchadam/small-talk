package app.dapk.st.design.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlin.math.absoluteValue

private object Palette {
    val brandPrimary = Color(0xFFb41cca)
}

private val DARK_COLOURS = darkColorScheme(
    primary = Palette.brandPrimary,
    onPrimary = Color(0xDDFFFFFF),
    secondaryContainer = Color(0xFF363639),
    onSecondaryContainer = Color(0xDDFFFFFF),
)

private val LIGHT_COLOURS = lightColorScheme(
    primary = Palette.brandPrimary,
    onPrimary = Color(0xDDFFFFFF),
    secondaryContainer = Color(0xFFf1f0f1),
    onSecondaryContainer = Color(0xFF000000),
)

private fun createExtended(scheme: ColorScheme) = ExtendedColors(
    selfBubble = scheme.primary,
    onSelfBubble = scheme.onPrimary,
    othersBubble = scheme.secondaryContainer,
    onOthersBubble = scheme.onSecondaryContainer,
    selfBubbleReplyBackground = scheme.primary.copy(alpha = 0.2f),
    otherBubbleReplyBackground = scheme.primary.copy(alpha = 0.2f),
    missingImageColors = listOf(
        Color(0xFFf7c7f7) to Color(0xFFdf20de),
        Color(0xFFe5d7f6) to Color(0xFF7b30cf),
        Color(0xFFf6c8cb) to Color(0xFFda2535),
    )
)

@Immutable
data class ExtendedColors(
    val selfBubble: Color,
    val onSelfBubble: Color,
    val othersBubble: Color,
    val onOthersBubble: Color,
    val selfBubbleReplyBackground: Color,
    val otherBubbleReplyBackground: Color,
    val missingImageColors: List<Pair<Color, Color>>,
) {
    fun getMissingImageColor(key: String): Pair<Color, Color> {
        return missingImageColors[key.hashCode().absoluteValue % (missingImageColors.size)]
    }
}

private val LocalExtendedColors = staticCompositionLocalOf<ExtendedColors> { throw IllegalAccessError() }

@Composable
fun SmallTalkTheme(themeConfig: ThemeConfig, content: @Composable () -> Unit) {
    val systemUiController = rememberSystemUiController()
    val systemInDarkTheme = isSystemInDarkTheme()

    val colorScheme = when {
        themeConfig.useDynamicTheme -> {
            when (systemInDarkTheme) {
                true -> dynamicDarkColorScheme(LocalContext.current)
                false -> dynamicLightColorScheme(LocalContext.current)
            }
        }

        else -> {
            when (systemInDarkTheme) {
                true -> DARK_COLOURS
                false -> LIGHT_COLOURS
            }
        }
    }
    MaterialTheme(colorScheme = colorScheme) {
        val backgroundColor = MaterialTheme.colorScheme.background
        SideEffect {
            systemUiController.setSystemBarsColor(backgroundColor)
        }
        CompositionLocalProvider(LocalExtendedColors provides createExtended(colorScheme)) {
            content()
        }
    }
}

object SmallTalkTheme {
    val extendedColors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}

data class ThemeConfig(
    val useDynamicTheme: Boolean,
)