package ch.rmy.android.http_shortcuts.utils

import android.graphics.Color
import androidx.annotation.ColorInt

object ColorUtil {
    @ColorInt
    fun String.hexStringToColorInt(): Int =
        if (length == 6) {
            val color = toIntOrNull(16) ?: Color.BLACK
            color + 0xff000000.toInt()
        } else Color.BLACK

    fun Int.colorIntToHexString(): String =
        String.format("%06x", this and 0xffffff)
}
