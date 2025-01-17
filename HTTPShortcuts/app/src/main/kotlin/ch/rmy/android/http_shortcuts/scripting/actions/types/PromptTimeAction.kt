package ch.rmy.android.http_shortcuts.scripting.actions.types

import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import ch.rmy.android.framework.extensions.showOrElse
import ch.rmy.android.framework.extensions.takeUnlessEmpty
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.dagger.ApplicationComponent
import ch.rmy.android.http_shortcuts.exceptions.UserException
import ch.rmy.android.http_shortcuts.scripting.ExecutionContext
import ch.rmy.android.http_shortcuts.utils.ActivityProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PromptTimeAction(
    private val format: String?,
    private val initialTime: String?,
) : BaseAction() {

    @Inject
    lateinit var context: Context

    @Inject
    lateinit var activityProvider: ActivityProvider

    override fun inject(applicationComponent: ApplicationComponent) {
        applicationComponent.inject(this)
    }

    override suspend fun execute(executionContext: ExecutionContext): String? =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<String> { continuation ->
                val time = getInitialTime()
                val timePicker = TimePickerDialog(
                    activityProvider.getActivity(),
                    { _, hourOfDay, minute ->
                        val newTime = LocalTime.of(hourOfDay, minute)
                        val pattern = format ?: DEFAULT_FORMAT
                        try {
                            continuation.resume(
                                DateTimeFormatter.ofPattern(pattern, Locale.US)
                                    .format(newTime)
                            )
                        } catch (e: IllegalArgumentException) {
                            continuation.resumeWithException(
                                UserException.create {
                                    getString(R.string.error_invalid_time_format)
                                }
                            )
                        }
                    },
                    time.hour,
                    time.minute,
                    DateFormat.is24HourFormat(context),
                )
                timePicker.setCancelable(true)
                timePicker.setCanceledOnTouchOutside(true)

                timePicker.showOrElse {
                    continuation.cancel()
                }
                timePicker.setOnDismissListener {
                    if (continuation.isActive) {
                        continuation.resume("")
                    }
                }
            }
        }
            .takeUnlessEmpty()
            ?.removePrefix("-")

    private fun getInitialTime(): LocalTime =
        initialTime
            ?.takeUnlessEmpty()
            ?.let { timeString ->
                try {
                    LocalTime.parse(timeString, DateTimeFormatter.ofPattern(DEFAULT_FORMAT, Locale.US))
                } catch (e: DateTimeParseException) {
                    null
                }
            }
            ?: LocalTime.now()

    companion object {
        private const val DEFAULT_FORMAT = "HH:mm"
    }
}
