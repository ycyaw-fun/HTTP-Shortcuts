package ch.rmy.android.http_shortcuts.activities.variables.editor.types.select

import ch.rmy.android.http_shortcuts.data.models.VariableModel

data class SelectTypeViewState(
    val variables: List<VariableModel>? = null,
    val options: List<OptionItem>,
    val isMultiSelect: Boolean,
    val separator: String,
) {
    val separatorEnabled
        get() = isMultiSelect

    val isDraggingEnabled: Boolean
        get() = options.size > 1
}
