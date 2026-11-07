package io.github.omrcat.catplan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandOrangeDeep,
    onPrimary = Color.White,
    secondary = OkGreen,
    background = Cream,
    onBackground = InkBrown,
    surface = SurfaceCream,
    onSurface = InkBrown,
    surfaceVariant = AccentSoft,
    onSurfaceVariant = MutedBrown,
    outline = BorderCream,
)

private val DarkColors = darkColorScheme(
    primary = BrandOrange,
    onPrimary = Color(0xFF241309),
    secondary = OkGreen,
    background = Color(0xFF191612),
    onBackground = Color(0xFFF2EBE1),
    surface = Color(0xFF242019),
    onSurface = Color(0xFFF2EBE1),
    surfaceVariant = Color(0xFF2E2921),
    onSurfaceVariant = Color(0xFFAB9D8D),
    outline = Color(0xFF38312A),
)

/**
 * 品牌主题：跟随系统深浅色，但不使用动态取色（Material You），
 * 保证猫计划的暖橘品牌色在各系统版本上一致。
 */
@Composable
fun CatPlanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
