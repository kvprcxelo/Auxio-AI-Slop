package org.oxycblt.auxio.linux.playback

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

/** Thin JNA surface over the parts of the libmpv C API the player needs. */
internal interface MpvApi : Library {
    fun mpv_create(): Pointer?

    fun mpv_initialize(ctx: Pointer): Int

    fun mpv_terminate_destroy(ctx: Pointer)

    fun mpv_set_option_string(ctx: Pointer, name: String, data: String): Int

    fun mpv_command_string(ctx: Pointer, args: String): Int

    fun mpv_get_property(ctx: Pointer, name: String, format: Int, data: Pointer): Int

    fun mpv_set_property(ctx: Pointer, name: String, format: Int, data: Pointer): Int

    fun mpv_error_string(error: Int): String?

    fun mpv_free(data: Pointer)
}

/** Resolves libmpv, tolerating a system that only ships the versioned library. */
internal object Mpv {
    private val NAMES = listOf("mpv", "libmpv.so.2", "libmpv.so.1", "libmpv.so")

    fun load(): MpvApi {
        var last: Throwable? = null
        for (name in NAMES) {
            try {
                return Native.load(name, MpvApi::class.java)
            } catch (error: Throwable) {
                last = error
            }
        }
        throw IllegalStateException(
            "libmpv was not found. Install it with your package manager " +
                "(for example `mpv-libs` or `libmpv2`).",
            last,
        )
    }
}

internal const val MPV_FORMAT_STRING: Int = 1
internal const val MPV_FORMAT_FLAG: Int = 3
internal const val MPV_FORMAT_DOUBLE: Int = 5
