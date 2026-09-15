package com.ridvan.target.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ridvan.target.data.local.BannerColor

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

// A softer, "subtle/modern" corner-radius scale than the Material3 stock defaults
// (extraSmall 4/small 8/medium 12/large 16/extraLarge 28dp) — not pill-shaped/bubbly,
// just noticeably less boxy. Button already renders stadium-shaped by default regardless
// of this scale, so it isn't listed here.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun TargetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    bannerColor: BannerColor = BannerColor.BLUE,
    // Dynamic color is available on Android 12+ — only used for the default Blue option, so
    // picking any other banner color is the one thing that opts out of Material You entirely.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (bannerColor != BannerColor.BLUE) {
        pinnedColorScheme(bannerColor.color, darkTheme)
    } else {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

private fun pinnedColorScheme(seed: Color, darkTheme: Boolean): ColorScheme {
    val onSeed = if (seed.luminance() >= 0.5f) Color.Black else Color.White
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = seed,
        onPrimary = onSeed,
        primaryContainer = seed,
        onPrimaryContainer = onSeed,
    )
}