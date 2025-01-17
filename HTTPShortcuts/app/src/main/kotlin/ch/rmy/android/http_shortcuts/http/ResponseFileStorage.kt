package ch.rmy.android.http_shortcuts.http

import android.content.Context
import android.net.Uri
import ch.rmy.android.framework.extensions.runIf
import ch.rmy.android.framework.utils.FileUtil
import okhttp3.Response
import java.io.File
import java.io.InputStream
import java.net.SocketTimeoutException
import java.util.zip.GZIPInputStream

class ResponseFileStorage(private val context: Context, private val sessionId: String) {

    private val file by lazy {
        File(context.cacheDir, "response_$sessionId")
    }

    fun store(response: Response, finishNormallyOnTimeout: Boolean): Uri {
        val fileUri = FileUtil.getUriFromFile(context, file)
        try {
            getStream(response).use { inStream ->
                context.contentResolver.openOutputStream(fileUri, "w")!!.use { outStream ->
                    inStream.copyTo(outStream)
                }
            }
        } catch (e: SocketTimeoutException) {
            if (!finishNormallyOnTimeout) {
                throw e
            }
        }
        return fileUri
    }

    private fun getStream(response: Response): InputStream =
        response.body!!.byteStream()
            .runIf(isGzipped(response)) {
                GZIPInputStream(this)
            }

    fun clear() {
        file.delete()
    }

    companion object {
        internal fun isGzipped(response: Response): Boolean =
            response.header(HttpHeaders.CONTENT_ENCODING) == "gzip"
    }
}
