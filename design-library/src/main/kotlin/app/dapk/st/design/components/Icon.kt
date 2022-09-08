package app.dapk.st.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation

@OptIn(ExperimentalUnitApi::class)
@Composable
fun BoxScope.CircleishAvatar(avatarUrl: String?, fallbackLabel: String, size: Dp) {
    when (avatarUrl) {
        null -> {
            val colors = SmallTalkTheme.extendedColors.getMissingImageColor(fallbackLabel)
            Box(
                Modifier
                    .align(Alignment.Center)
                    .background(color = colors.first, shape = CircleShape)
                    .size(size),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fallbackLabel.uppercase().first().toString(),
                    color = colors.second,
                    fontWeight = FontWeight.Medium,
                    fontSize = TextUnit(size.value * 0.5f, TextUnitType.Sp)
                )
            }
        }
        else -> {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .transformations(CircleCropTransformation())
                        .build()
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
fun MissingAvatarIcon(displayName: String, displayImageSize: Dp) {
    val colors = SmallTalkTheme.extendedColors.getMissingImageColor(displayName)
    Box(
        Modifier
            .background(color = colors.first, shape = CircleShape)
            .size(displayImageSize), contentAlignment = Alignment.Center
    ) {
        Text(
            text = (displayName).first().toString().uppercase(),
            color = colors.second
        )
    }
}

@Composable
fun MessengerUrlIcon(avatarUrl: String, displayImageSize: Dp) {
    Image(
        painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUrl)
                .transformations(CircleCropTransformation())
                .build()
        ),
        contentDescription = null,
        modifier = Modifier.size(displayImageSize)
    )
}