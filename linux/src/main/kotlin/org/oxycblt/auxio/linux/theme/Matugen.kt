/*
 * Copyright (c) 2026 Auxio Project
 * Matugen.kt is part of Auxio.
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

import androidx.compose.ui.graphics.Color
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import org.oxycblt.auxio.linux.Storage

/**
 * The `matugen` command line, used as the colour engine when it is installed.
 *
 * matugen already knows how to build a Material 3 scheme from an image or a single colour, so the
 * port asks it rather than shipping its own HCT implementation. Its `--json hex` dump carries every
 * role the scheme needs; [parse] picks the dark one out of it.
 */
object Matugen {
    private val ROLE = Regex("\"([a-z_0-9]+)\"\\s*:\\s*\"(#[0-9a-fA-F]{6,8})\"")
    private val ROLE_BLOCK = Regex("\"([a-z_0-9]+)\"\\s*:\\s*\\{")
    private val COLOR = Regex("\"color\"\\s*:\\s*\"(#[0-9a-fA-F]{6,8})\"")

    /** Written to `~/.config/matugen/config.toml` when a user has none. */
    private val DEFAULT_CONFIG =
        """
        # Written by Auxio for Linux on first launch.
        # matugen reads this file for every run; add your own templates under [templates].

        [config]
        version_check = false

        [templates]
        """
            .trimIndent()
            .plus("\n")

    /** The path to `matugen`, or null when it is not installed. */
    fun executable(): String? {
        val path = System.getenv("PATH").orEmpty()
        val extensions =
            if (System.getProperty("os.name").startsWith("Windows")) listOf(".exe") else listOf("")

        return path
            .split(java.io.File.pathSeparator)
            .asSequence()
            .filter { it.isNotBlank() }
            .flatMap { directory ->
                extensions.asSequence().map { Path.of(directory, "matugen$it") }
            }
            .firstOrNull { Files.isExecutable(it) }
            ?.toString()
    }

    fun isAvailable(): Boolean = executable() != null

    /**
     * Writes a `config.toml` for matugen when the user does not have one yet, along with the
     * `~/.config/matugen` directory, so a first run does not fail on a missing configuration.
     *
     * matugen works without one for `--json`, but every other invocation reads it, and a config
     * that is missing is the usual reason a fresh install appears not to work at all.
     */
    fun ensureConfig(directory: Path = Storage.configPath("matugen")) {
        val file = directory.resolve("config.toml")
        if (Files.exists(file)) return

        runCatching {
            Files.createDirectories(directory)
            Files.writeString(file, DEFAULT_CONFIG)
        }
    }

    /** The roles of the scheme matugen derives from [image], or null if it could not run. */
    fun fromImage(image: Path, type: String, dark: Boolean = true): Map<String, String>? {
        if (!Files.isRegularFile(image)) return null

        return run(listOf("image", image.toAbsolutePath().toString()), type, dark)?.let {
            parse(it, dark)
        }
    }

    /** The roles of the scheme matugen derives from a single [seed] colour. */
    fun fromColor(seed: Color, type: String, dark: Boolean = true): Map<String, String>? =
        run(listOf("color", "hex", seed.toHex()), type, dark)?.let { parse(it, dark) }

    private fun run(source: List<String>, type: String, dark: Boolean): String? {
        val executable = executable() ?: return null
        val command =
            listOf(executable) +
                source +
                listOf(
                    "--mode",
                    if (dark) "dark" else "light",
                    "--type",
                    type,
                    "--json",
                    "hex",
                    "--quiet",
                )

        // Without an explicit index matugen asks which colour to use, on a terminal nobody is
        // reading; older releases do not know the flag, so it is dropped on a failed attempt.
        return runCommand(command + listOf("--source-color-index", "0")) ?: runCommand(command)
    }

    private fun runCommand(command: List<String>): String? =
        runCatching {
                val process = ProcessBuilder(command).redirectErrorStream(false).start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    null
                } else {
                    output.takeIf { process.exitValue() == 0 }
                }
            }
            .getOrNull()

    /**
     * The dark scheme out of a `--json hex` dump, keyed by snake_case role name.
     *
     * Two shapes are in the wild. matugen 4 keys every role itself, with a colour per mode
     * (`"primary": {"dark": {"color": "#…"}}`); earlier versions nested the schemes instead
     * (`"colors": {"dark": {"primary": "#…"}}`). Both are read, since a user's matugen is whatever
     * their distribution packages.
     */
    internal fun parse(json: String, dark: Boolean = true): Map<String, String>? {
        val colors = objectAt(json, "colors") ?: return null
        val mode = if (dark) "dark" else "light"

        // Read role by role first: a light dump may only carry the roles its author set, and
        // schemeOf falls back for the rest, so a partial map is still worth returning.
        perRole(colors, mode)?.let {
            return it
        }

        return objectAt(colors, mode)
            ?.let { scheme ->
                ROLE.findAll(scheme).associate { it.groupValues[1] to it.groupValues[2] }
            }
            ?.takeIf { it.isNotEmpty() }
    }

    /** The 4.x shape: a block per role, each holding `dark`/`default`/`light` colours. */
    private fun perRole(colors: String, mode: String): Map<String, String>? {
        val roles = mutableMapOf<String, String>()
        ROLE_BLOCK.findAll(colors).forEach { match ->
            val name = match.groupValues[1]
            val block = objectAt(colors, name) ?: return@forEach
            val variant = objectAt(block, mode) ?: return@forEach
            COLOR.find(variant)?.let { roles[name] = it.groupValues[1] }
        }
        return roles.takeIf { it.isNotEmpty() }
    }

    /** The `{…}` value of [key] in [json], sliced by brace depth so nested objects stay apart. */
    internal fun objectAt(json: String, key: String): String? {
        val keyIndex = json.indexOf("\"$key\"").takeIf { it >= 0 } ?: return null
        val start = json.indexOf('{', keyIndex).takeIf { it > keyIndex } ?: return null

        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until json.length) {
            val char = json[index]
            when {
                escaped -> escaped = false
                char == '\\' -> escaped = true
                char == '"' -> inString = !inString
                inString -> Unit
                char == '{' -> depth++
                char == '}' -> {
                    depth--
                    if (depth == 0) return json.substring(start, index + 1)
                }
            }
        }
        return null
    }

    private const val TIMEOUT_SECONDS = 20L
}

/** Which of matugen's palettes to build; the names are matugen's own `--type` values. */
enum class SchemeType(val label: String, val flag: String) {
    TONAL_SPOT("Tonal spot", "scheme-tonal-spot"),
    CONTENT("Content", "scheme-content"),
    VIBRANT("Vibrant", "scheme-vibrant"),
    EXPRESSIVE("Expressive", "scheme-expressive"),
    NEUTRAL("Neutral", "scheme-neutral"),
    MONOCHROME("Monochrome", "scheme-monochrome"),
}

private fun Color.toHex(): String {
    fun channel(value: Float): Int = (value.coerceIn(0f, 1f) * 255f).toInt()
    return "#%02x%02x%02x".format(channel(red), channel(green), channel(blue))
}
