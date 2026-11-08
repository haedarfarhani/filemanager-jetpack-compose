package ir.heydar.nexfolder.ui.components.icon

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import ir.heydar.nexfolder.ui.theme.Spacing

@Composable
fun AppIcon(
    @DrawableRes icon: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = Spacing.iconSizeSmall
) {
    Icon(
        painter = painterResource(id = icon),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint
    )
}
