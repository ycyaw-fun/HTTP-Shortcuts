package ch.rmy.android.http_shortcuts.activities.editor.response

import ch.rmy.android.framework.utils.localization.Localizable
import ch.rmy.android.framework.viewmodel.viewstate.DialogState
import ch.rmy.android.http_shortcuts.data.enums.ResponseDisplayAction
import ch.rmy.android.http_shortcuts.data.models.ResponseHandling

data class ResponseViewState(
    val dialogState: DialogState? = null,
    val successMessageHint: Localizable = Localizable.EMPTY,
    val responseUiType: String = "",
    val responseSuccessOutput: String = "",
    val responseFailureOutput: String = "",
    val includeMetaInformation: Boolean = false,
    val successMessage: String = "",
    val responseDisplayActions: List<ResponseDisplayAction> = emptyList(),
    val storeResponseIntoFile: Boolean = false,
    val storeFileName: String = "",
    val replaceFileIfExists: Boolean = false,
) {
    private val hasOutput
        get() = responseSuccessOutput != ResponseHandling.SUCCESS_OUTPUT_NONE ||
            responseFailureOutput != ResponseHandling.FAILURE_OUTPUT_NONE

    val responseUiTypeVisible
        get() = hasOutput

    val successMessageVisible
        get() = responseSuccessOutput == ResponseHandling.SUCCESS_OUTPUT_MESSAGE

    val includeMetaInformationVisible
        get() = responseUiType == ResponseHandling.UI_TYPE_WINDOW && hasOutput

    val dialogActionVisible
        get() = responseUiType == ResponseHandling.UI_TYPE_DIALOG && hasOutput

    val showToastInfo
        get() = responseUiType == ResponseHandling.UI_TYPE_TOAST

    val showActionButtonCheckboxes
        get() = responseUiType == ResponseHandling.UI_TYPE_WINDOW && hasOutput

    val showShareActionEnabled: Boolean
        get() = ResponseDisplayAction.SHARE in responseDisplayActions

    val showCopyActionEnabled: Boolean
        get() = ResponseDisplayAction.COPY in responseDisplayActions

    val showRerunActionEnabled: Boolean
        get() = ResponseDisplayAction.RERUN in responseDisplayActions

    val showSaveActionEnabled: Boolean
        get() = ResponseDisplayAction.SAVE in responseDisplayActions

    val dialogAction: ResponseDisplayAction?
        get() = responseDisplayActions.firstOrNull()
}
