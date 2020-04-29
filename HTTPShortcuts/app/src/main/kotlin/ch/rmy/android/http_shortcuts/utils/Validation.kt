package ch.rmy.android.http_shortcuts.utils

import android.net.Uri
import ch.rmy.android.http_shortcuts.extensions.consume
import ch.rmy.android.http_shortcuts.variables.Variables.VARIABLE_ID_REGEX
import java.net.URI
import java.net.URISyntaxException

object Validation {

    fun isAcceptableUrl(url: String) =
        url.matches("^(http(s?)://.+)|((h(t(t(p(s)?)?)?)?)?\\{\\{$VARIABLE_ID_REGEX\\}\\}.*)".toRegex(RegexOption.IGNORE_CASE))

    fun isValidUrl(uri: Uri) =
        (uri.scheme?.let { scheme ->
            scheme.equals("http", ignoreCase = true)
                || scheme.equals("https", ignoreCase = true)
        } ?: false)
            && uri.host?.isNotEmpty() == true
            && try {
            consume { URI(uri.toString()) }
        } catch (e: URISyntaxException) {
            false
        }

}
