package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = BanyanGreen,
    onPrimary = Color.White,
    primaryContainer = BanyanGreenContainer,
    onPrimaryContainer = OnBanyanGreenContainer,
    secondary = TerracottaSaffron,
    onSecondary = Color.White,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = OnTerracottaContainer,
    tertiary = HarvestGold,
    onTertiary = Color.White,
    tertiaryContainer = HarvestGoldContainer,
    onTertiaryContainer = OnHarvestGoldContainer,
    background = WarmCream,
    onBackground = TextPrimaryLight,
    surface = WarmSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = WarmOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    background = DarkBackground,
    onBackground = Color(0xFFE7ECE6),
    surface = DarkSurface,
    onSurface = Color(0xFFE7ECE6),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFBFC8BC),
    outline = DarkOutline
)

val VillageShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyVillageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = VillageShapes,
        content = content
    )
}
