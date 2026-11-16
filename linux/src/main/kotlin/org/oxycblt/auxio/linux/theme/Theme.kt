/*
 * Copyright (c) 2026 Auxio Project
 * Theme.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package org.oxycblt.auxio.linux.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.roundToInt

/** Where the colour scheme comes from. */
enum class ThemeMode(val label: String, val description: String) {
    DESKTOP("Desktop colours", "Use the accent colour your desktop reports"),
    MATUGEN("Matugen", "Generate the scheme from your wallpaper with matugen"),
    CUSTOM("Deep customization", "Choose the seed colour and palette yourself"),
}

/** Whether the window is light or dark, or follows the desktop. */
enum class Brightness(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

/** Everything the theme needs from the user, stored in the settings file. */
data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.DESKTOP,
    val brightness: Brightness = Brightness.SYSTEM,
    /** The custom seed, as `#rrggbb`; it is what the sRGB sliders and the hex field edit. */
    val seed: String = DEFAULT_SEED,
    val wallpaper: String = "",
    val type: SchemeType = SchemeType.TONAL_SPOT,
) {
    val seedColor: Color
        get() = parseColor(seed) ?: parseColor(DEFAULT_SEED) ?: Desktop.DEFAULT_ACCENT

    companion object {
        const val DEFAULT_SEED = "#6750a4"
    }
}

/**
 * Turns a [ThemeSettings] into a Material 3 scheme.
 *
 * matugen does the colour science when it is installed, for wallpapers, desktop accents and custom
 * seeds alike; the built-in generator takes over when it is not, so the port never falls back to
 * stock purple for want of a binary.
 */
object Themes {
    fun scheme(settings: ThemeSettings): ColorScheme =
        scheme(settings, Desktop.appearance(), Desktop.wallpaper())

    internal fun scheme(
        settings: ThemeSettings,
        appearance: Desktop.Appearance,
        wallpaper: Path?,
        cache: ThemeCache = ThemeCache(),
    ): ColorScheme {
        val accent = appearance.accent ?: Desktop.DEFAULT_ACCENT
        val image = wallpaperOf(settings, wallpaper)
        val dark = isDark(settings, appearance)

        return when (settings.mode) {
            ThemeMode.DESKTOP -> fromSeed(accent, settings, dark)
            ThemeMode.CUSTOM -> fromSeed(settings.seedColor, settings, dark)
            ThemeMode.MATUGEN -> {
                val computed = image?.let { Matugen.fromImage(it, settings.type.flag, dark) }
                if (image != null && computed != null) {
                    cache.store(cacheKey(settings.type, image, dark), computed)
                }
                computed?.let { schemeOf(it, dark) }
                    ?: image
                        ?.let { cache.roles(cacheKey(settings.type, it, dark)) }
                        ?.let { schemeOf(it, dark) }
                    ?: fromSeed(accent, settings, dark)
            }
        }
    }

    /**
     * The scheme the very first frame is drawn in, so a launch never shows the wrong colours.
     *
     * Desktop and custom seeds are cheap enough to build here (a single `matugen color hex` at
     * worst). A wallpaper is not: matugen has to decode the image, so the last result is read from
     * [cache] instead, and the real run happens once the window is up. Without a cached result the
     * accent scheme stands in, which is still the user's colour rather than the stock purple.
     */
    fun startingScheme(
        settings: ThemeSettings,
        appearance: Desktop.Appearance,
        wallpaper: Path?,
        cache: ThemeCache = ThemeCache(),
    ): ColorScheme {
        val accent = appearance.accent ?: Desktop.DEFAULT_ACCENT
        val dark = isDark(settings, appearance)

        return when (settings.mode) {
            ThemeMode.DESKTOP -> fromSeed(accent, settings, dark)
            ThemeMode.CUSTOM -> fromSeed(settings.seedColor, settings, dark)
            ThemeMode.MATUGEN -> {
                val image = wallpaperOf(settings, wallpaper)
                val cached = image?.let { cache.roles(cacheKey(settings.type, it, dark)) }
                cached?.let { schemeOf(it, dark) } ?: fromSeed(accent, settings, dark)
            }
        }
    }

    /** Dark unless the settings say otherwise; [Brightness.SYSTEM] asks the desktop. */
    internal fun isDark(settings: ThemeSettings, appearance: Desktop.Appearance): Boolean =
        when (settings.brightness) {
            Brightness.LIGHT -> false
            Brightness.DARK -> true
            Brightness.SYSTEM -> appearance.dark
        }

    /** The scheme matugen builds from [seed], or the built-in approximation when it is absent. */
    private fun fromSeed(seed: Color, settings: ThemeSettings, dark: Boolean): ColorScheme =
        (if (Matugen.isAvailable()) Matugen.fromColor(seed, settings.type.flag, dark) else null)
            ?.let { schemeOf(it, dark) } ?: Approximation.scheme(seed, settings.type, dark)

    /** Reads matugen's snake_case roles into Compose's camelCase scheme. */
    internal fun schemeOf(roles: Map<String, String>, dark: Boolean): ColorScheme {
        val base = if (dark) darkColorScheme() else lightColorScheme()

        fun role(name: String, fallback: Color): Color = roles[name]?.let(::colorOf) ?: fallback

        return base.copy(
            primary = role("primary", base.primary),
            onPrimary = role("on_primary", base.onPrimary),
            primaryContainer = role("primary_container", base.primaryContainer),
            onPrimaryContainer = role("on_primary_container", base.onPrimaryContainer),
            inversePrimary = role("inverse_primary", base.inversePrimary),
            secondary = role("secondary", base.secondary),
            onSecondary = role("on_secondary", base.onSecondary),
            secondaryContainer = role("secondary_container", base.secondaryContainer),
            onSecondaryContainer = role("on_secondary_container", base.onSecondaryContainer),
            tertiary = role("tertiary", base.tertiary),
            onTertiary = role("on_tertiary", base.onTertiary),
            tertiaryContainer = role("tertiary_container", base.tertiaryContainer),
            onTertiaryContainer = role("on_tertiary_container", base.onTertiaryContainer),
            error = role("error", base.error),
            onError = role("on_error", base.onError),
            errorContainer = role("error_container", base.errorContainer),
            onErrorContainer = role("on_error_container", base.onErrorContainer),
            background = role("background", base.background),
            onBackground = role("on_background", base.onBackground),
            surface = role("surface", base.surface),
            onSurface = role("on_surface", base.onSurface),
            surfaceVariant = role("surface_variant", base.surfaceVariant),
            onSurfaceVariant = role("on_surface_variant", base.onSurfaceVariant),
            surfaceTint = role("primary", base.surfaceTint),
            inverseSurface = role("inverse_surface", base.inverseSurface),
            inverseOnSurface = role("inverse_on_surface", base.inverseOnSurface),
            outline = role("outline", base.outline),
            outlineVariant = role("outline_variant", base.outlineVariant),
            scrim = role("scrim", base.scrim),
        )
    }

    internal fun colorOf(hex: String): Color? = parseColor(hex)

    private fun wallpaperOf(settings: ThemeSettings, wallpaper: Path?): Path? =
        settings.wallpaper.trim().takeIf { it.isNotEmpty() }?.let(Path::of) ?: wallpaper

    /**
     * Identifies a wallpaper palette, so an edited wallpaper or another palette misses the cache.
     */
    internal fun cacheKey(type: SchemeType, image: Path, dark: Boolean): String {
        val size = runCatching { Files.size(image) }.getOrDefault(-1L)
        val modified = runCatching { Files.getLastModifiedTime(image).toMillis() }.getOrDefault(-1L)
        val light = if (dark) "dark" else "light"
        return "$light|${type.flag}|$size|$modified|${image.toAbsolutePath().normalize()}"
    }

    /**
     * Stands in for matugen when it is not installed: the seed's hue carries the primary and
     * containers, chroma decides how far the greys lean into it, and every on-colour is set by
     * luminance rather than assumed, so a bright seed does not produce unreadable text.
     *
     * The palette types matugen offers are approximated too, by shifting and scaling the seed's hue
     * and chroma, so picking Vibrant or Monochrome changes the scheme either way.
     */
    internal object Approximation {
        /** How a palette type leans the seed: hue offsets in degrees, chroma as a multiplier. */
        private data class Palette(
            val primaryHue: Float = 0f,
            val secondaryHue: Float = 0f,
            val tertiaryHue: Float = 60f,
            val chroma: Float = 0.85f,
        )

        private fun palette(type: SchemeType): Palette =
            when (type) {
                SchemeType.TONAL_SPOT -> Palette()
                SchemeType.CONTENT -> Palette(tertiaryHue = 55f, chroma = 1.1f)
                SchemeType.VIBRANT -> Palette(secondaryHue = -10f, tertiaryHue = 70f, chroma = 1.7f)
                SchemeType.EXPRESSIVE ->
                    Palette(primaryHue = 15f, secondaryHue = 45f, tertiaryHue = 95f, chroma = 1.3f)
                SchemeType.NEUTRAL -> Palette(tertiaryHue = 40f, chroma = 0.16f)
                SchemeType.MONOCHROME -> Palette(secondaryHue = 0f, tertiaryHue = 0f, chroma = 0f)
            }

        fun scheme(
            seed: Color,
            type: SchemeType = SchemeType.TONAL_SPOT,
            dark: Boolean = true,
        ): ColorScheme {
            val hsl = seed.toHsl()
            val palette = palette(type)
            val hue = (hsl[0] + palette.primaryHue).mod(360f)
            val chroma = (hsl[1] * palette.chroma).coerceIn(0f, 1f)

            fun tone(lightness: Float, saturation: Float): Color =
                Color.hsl(hue, (chroma * saturation).coerceIn(0f, 1f), lightness)

            val surface = tone(if (dark) 0.07f else 0.98f, 0.08f)
            val surfaceVariant = tone(if (dark) 0.17f else 0.92f, 0.10f)
            val onSurface = tone(if (dark) 0.93f else 0.11f, 0.04f)
            val onSurfaceVariant = tone(if (dark) 0.78f else 0.30f, 0.07f)
            val primary = tone(if (dark) 0.76f else 0.42f, 0.55f)
            val primaryContainer = tone(if (dark) 0.30f else 0.90f, 0.45f)
            val secondaryHue = (hue + palette.secondaryHue).mod(360f)
            val secondary =
                Color.hsl(
                    secondaryHue,
                    (chroma * 0.30f).coerceIn(0f, 1f),
                    if (dark) 0.72f else 0.45f,
                )
            val secondaryContainer =
                Color.hsl(
                    secondaryHue,
                    (chroma * 0.24f).coerceIn(0f, 1f),
                    if (dark) 0.28f else 0.90f,
                )
            val tertiaryHue = (hue + palette.tertiaryHue).mod(360f)
            val tertiary =
                Color.hsl(
                    tertiaryHue,
                    (chroma * 0.45f).coerceIn(0f, 1f),
                    if (dark) 0.74f else 0.42f,
                )
            val tertiaryContainer =
                Color.hsl(
                    tertiaryHue,
                    (chroma * 0.35f).coerceIn(0f, 1f),
                    if (dark) 0.29f else 0.90f,
                )
            val error = Color.hsl(6f, 0.72f, if (dark) 0.74f else 0.42f)
            val errorContainer = Color.hsl(6f, 0.50f, if (dark) 0.30f else 0.90f)

            return (if (dark) darkColorScheme() else lightColorScheme()).copy(
                primary = primary,
                onPrimary = contrasting(primary, dark),
                primaryContainer = primaryContainer,
                onPrimaryContainer = contrasting(primaryContainer, dark),
                inversePrimary = tone(if (dark) 0.42f else 0.78f, 0.50f),
                secondary = secondary,
                onSecondary = contrasting(secondary, dark),
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = contrasting(secondaryContainer, dark),
                tertiary = tertiary,
                onTertiary = contrasting(tertiary, dark),
                tertiaryContainer = tertiaryContainer,
                onTertiaryContainer = contrasting(tertiaryContainer, dark),
                error = error,
                onError = contrasting(error, dark),
                errorContainer = errorContainer,
                onErrorContainer = contrasting(errorContainer, dark),
                background = surface,
                onBackground = onSurface,
                surface = surface,
                onSurface = onSurface,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = onSurfaceVariant,
                surfaceTint = primary,
                inverseSurface = onSurface,
                inverseOnSurface = surface,
                outline = tone(if (dark) 0.55f else 0.50f, 0.08f),
                outlineVariant = tone(if (dark) 0.30f else 0.78f, 0.10f),
                scrim = Color.Black,
            )
        }

        /** Dark text on a light fill, light text on a dark one, so labels stay readable. */
        private fun contrasting(fill: Color, @Suppress("UNUSED_PARAMETER") dark: Boolean): Color {
            val hue = fill.toHsl()[0]
            val lightFill = fill.luminance() > 0.45f
            return Color.hsl(hue, 0.25f, if (lightFill) 0.13f else 0.95f)
        }
    }
}

/** `#rrggbb`, for naming a colour in the settings. */
fun Color.toHexString(): String {
    fun channel(value: Float): Int = (value.coerceIn(0f, 1f) * 255f).roundToInt()
    return "#%02x%02x%02x".format(channel(red), channel(green), channel(blue))
}

/** Hue, saturation and lightness, the three numbers the approximation works in. */
internal fun Color.toHsl(): FloatArray {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val lightness = (max + min) / 2f
    if (max == min) return floatArrayOf(0f, 0f, lightness)

    val delta = max - min
    val saturation = if (lightness > 0.5f) delta / (2f - max - min) else delta / (max + min)
    val hue =
        when (max) {
            red -> 60f * (((green - blue) / delta).mod(6f))
            green -> 60f * ((blue - red) / delta + 2f)
            else -> 60f * ((red - green) / delta + 4f)
        }
    return floatArrayOf(hue.mod(360f), saturation.coerceIn(0f, 1f), lightness)
}

/** `#rrggbb` or `rrggbb`; null when the text is not a six-digit colour. */
internal fun parseColor(text: String): Color? {
    val digits = text.trim().removePrefix("#")
    if (digits.length != 6) return null
    val value = digits.toLongOrNull(16) ?: return null
    return Color(0xFF000000L or value)
}
