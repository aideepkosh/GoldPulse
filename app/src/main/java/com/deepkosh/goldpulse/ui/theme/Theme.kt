package com.deepkosh.goldpulse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF745B0C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDF91),
    onPrimaryContainer = Color(0xFF241A00),
    secondary = Color(0xFF695D3F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2E1BB),
    onSecondaryContainer = Color(0xFF231B04),
    tertiary = Color(0xFF47664A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC9ECC9),
    onTertiaryContainer = Color(0xFF04210B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F0),
    onBackground = Color(0xFF1F1B13),
    surface = Color(0xFFFFF8F0),
    onSurface = Color(0xFF1F1B13),
    surfaceVariant = Color(0xFFECE1CF),
    onSurfaceVariant = Color(0xFF4C4639),
    outline = Color(0xFF7E7667),
    outlineVariant = Color(0xFFCFC6B4),
    inverseSurface = Color(0xFF343027),
    inverseOnSurface = Color(0xFFF8EFE2),
    inversePrimary = Color(0xFFE4C36C),
    surfaceTint = Color(0xFF745B0C),
    surfaceDim = Color(0xFFE2D9CC),
    surfaceBright = Color(0xFFFFF8F0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCF2E5),
    surfaceContainer = Color(0xFFF6ECDF),
    surfaceContainerHigh = Color(0xFFF1E7D9),
    surfaceContainerHighest = Color(0xFFEBE1D4),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE4C36C),
    onPrimary = Color(0xFF3D2E00),
    primaryContainer = Color(0xFF594400),
    onPrimaryContainer = Color(0xFFFFDF91),
    secondary = Color(0xFFD6C5A0),
    onSecondary = Color(0xFF393015),
    secondaryContainer = Color(0xFF50462A),
    onSecondaryContainer = Color(0xFFF2E1BB),
    tertiary = Color(0xFFADCFAD),
    onTertiary = Color(0xFF19361F),
    tertiaryContainer = Color(0xFF304D34),
    onTertiaryContainer = Color(0xFFC9ECC9),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF17130B),
    onBackground = Color(0xFFEBE1D4),
    surface = Color(0xFF17130B),
    onSurface = Color(0xFFEBE1D4),
    surfaceVariant = Color(0xFF4C4639),
    onSurfaceVariant = Color(0xFFCFC6B4),
    outline = Color(0xFF989080),
    outlineVariant = Color(0xFF4C4639),
    inverseSurface = Color(0xFFEBE1D4),
    inverseOnSurface = Color(0xFF343027),
    inversePrimary = Color(0xFF745B0C),
    surfaceTint = Color(0xFFE4C36C),
    surfaceDim = Color(0xFF17130B),
    surfaceBright = Color(0xFF3E382F),
    surfaceContainerLowest = Color(0xFF110E07),
    surfaceContainerLow = Color(0xFF1F1B13),
    surfaceContainer = Color(0xFF231F17),
    surfaceContainerHigh = Color(0xFF2E2921),
    surfaceContainerHighest = Color(0xFF39342B),
)

/** Colors for price movement, which Material's scheme has no roles for. */
@Immutable
data class PriceColors(val up: Color, val down: Color)

private val LightPriceColors = PriceColors(up = Color(0xFF1E7B34), down = Color(0xFFBA1A1A))
private val DarkPriceColors = PriceColors(up = Color(0xFF7BD88F), down = Color(0xFFFFB4AB))

val LocalPriceColors = staticCompositionLocalOf { LightPriceColors }

/** The 24K hero card: a gold bar gradient that reads the same in light and dark mode. */
object GoldCard {
    val background = Brush.linearGradient(
        listOf(Color(0xFFFAE39A), Color(0xFFEBC15A), Color(0xFFC9952E)),
    )
    val content = Color(0xFF2B1D00)
    val up = Color(0xFF145A24)
    val down = Color(0xFF8C1D18)
}

@Composable
fun GoldPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalPriceColors provides if (darkTheme) DarkPriceColors else LightPriceColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content,
        )
    }
}
