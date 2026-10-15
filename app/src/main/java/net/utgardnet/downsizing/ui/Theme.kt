package net.utgardnet.downsizing.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B65), onPrimary = Color.White,
    primaryContainer = Color(0xFFB2F1E9), onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFF386B66), onSecondary = Color.White,
    secondaryContainer = Color(0xFFC5EEE8), onSecondaryContainer = Color(0xFF123530),
    tertiary = Color(0xFF006B65),
    background = Color.White, onBackground = Color(0xFF182321),
    surface = Color.White, onSurface = Color(0xFF182321),
    surfaceVariant = Color(0xFFE5EFED), onSurfaceVariant = Color(0xFF3E4946),
    surfaceContainer = Color(0xFFF0F7F5), surfaceContainerLow = Color(0xFFF6FAF9),
    surfaceContainerHigh = Color(0xFFE8F2F0), surfaceContainerHighest = Color(0xFFE0EBE8),
    outline = Color(0xFF6F7976)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF62D9CC), onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF00504B), onPrimaryContainer = Color(0xFFB2F1E9),
    secondary = Color(0xFFA5D2CC), onSecondary = Color(0xFF073732),
    secondaryContainer = Color(0xFF284C47), onSecondaryContainer = Color(0xFFC5EEE8),
    tertiary = Color(0xFF62D9CC),
    background = Color(0xFF1B1F20), onBackground = Color(0xFFE2E8E6),
    surface = Color(0xFF1B1F20), onSurface = Color(0xFFE2E8E6),
    surfaceVariant = Color(0xFF3C4946), onSurfaceVariant = Color(0xFFBECBC6),
    surfaceContainer = Color(0xFF252B2B), surfaceContainerLow = Color(0xFF202626),
    surfaceContainerHigh = Color(0xFF2D3433), surfaceContainerHighest = Color(0xFF363D3C),
    outline = Color(0xFF899590)
)

@Composable
fun DownsizingTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
