package ch.rmy.android.http_shortcuts.variables.types

import android.graphics.Color
import ch.rmy.android.framework.extensions.showOrElse
import ch.rmy.android.framework.extensions.toLocalizable
import ch.rmy.android.http_shortcuts.dagger.ApplicationComponent
import ch.rmy.android.http_shortcuts.data.domains.variables.VariableRepository
import ch.rmy.android.http_shortcuts.data.models.Variable
import ch.rmy.android.http_shortcuts.utils.ActivityProvider
import ch.rmy.android.http_shortcuts.utils.ColorPickerFactory
import ch.rmy.android.http_shortcuts.utils.ColorUtil.colorIntToHexString
import ch.rmy.android.http_shortcuts.utils.ColorUtil.hexStringToColorInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume

class ColorType : BaseVariableType() {

    @Inject
    lateinit var variablesRepository: VariableRepository

    @Inject
    lateinit var activityProvider: ActivityProvider

    @Inject
    lateinit var colorPickerFactory: ColorPickerFactory

    override fun inject(applicationComponent: ApplicationComponent) {
        applicationComponent.inject(this)
    }

    override suspend fun resolveValue(variable: Variable): String {
        val value = withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<String> { continuation ->
                colorPickerFactory.createColorPicker(
                    onColorPicked = { color ->
                        continuation.resume(color.colorIntToHexString())
                    },
                    onDismissed = {
                        continuation.cancel()
                    },
                    title = variable.title.toLocalizable(),
                    initialColor = getInitialColor(variable),
                )
                    .showOrElse {
                        continuation.cancel()
                    }
            }
        }
        if (variable.rememberValue) {
            variablesRepository.setVariableValue(variable.id, value)
        }
        return value
    }

    private fun getInitialColor(variable: Variable): Int =
        variable.takeIf { it.rememberValue }?.value?.hexStringToColorInt()
            ?: Color.WHITE
}
