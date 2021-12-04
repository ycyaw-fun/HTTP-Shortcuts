package ch.rmy.android.http_shortcuts.variables

import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.extensions.logException
import ch.rmy.android.http_shortcuts.extensions.showSoftKeyboard
import ch.rmy.android.http_shortcuts.extensions.showToast
import ch.rmy.android.http_shortcuts.extensions.toDestroyable
import ch.rmy.android.http_shortcuts.utils.Destroyable

object VariableViewUtils {

    fun bindVariableViews(editText: VariableEditText, button: VariableButton, variablePlaceholderProvider: VariablePlaceholderProvider): Destroyable {
        editText.variablePlaceholderProvider = variablePlaceholderProvider
        button.variablePlaceholderProvider = variablePlaceholderProvider
        return button.variableSource
            .subscribe(
                { variablePlaceholder ->
                    editText.insertVariablePlaceholder(variablePlaceholder)
                    editText.showSoftKeyboard()
                },
                { error ->
                    logException(error)
                    editText.context.showToast(R.string.error_generic)
                }
            )
            .toDestroyable()
    }
}
