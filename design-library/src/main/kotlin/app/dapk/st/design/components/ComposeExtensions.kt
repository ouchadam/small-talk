package app.dapk.st.design.components

import android.content.res.Configuration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Configuration.percentOfHeight(float: Float): Dp {
    return (this.screenHeightDp * float).dp
}