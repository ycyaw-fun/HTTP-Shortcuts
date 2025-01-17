package ch.rmy.android.http_shortcuts.activities.editor.response

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import ch.rmy.android.framework.utils.localization.Localizable
import ch.rmy.android.framework.utils.localization.StringResLocalizable
import ch.rmy.android.framework.viewmodel.BaseViewModel
import ch.rmy.android.framework.viewmodel.WithDialog
import ch.rmy.android.framework.viewmodel.viewstate.DialogState
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.variables.VariablesActivity
import ch.rmy.android.http_shortcuts.dagger.getApplicationComponent
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.TemporaryShortcutRepository
import ch.rmy.android.http_shortcuts.data.enums.ResponseDisplayAction
import ch.rmy.android.http_shortcuts.data.models.ResponseHandling
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.usecases.GetVariablePlaceholderPickerDialogUseCase
import ch.rmy.android.http_shortcuts.usecases.KeepVariablePlaceholderProviderUpdatedUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

class ResponseViewModel(application: Application) : BaseViewModel<Unit, ResponseViewState>(application), WithDialog {

    @Inject
    lateinit var temporaryShortcutRepository: TemporaryShortcutRepository

    @Inject
    lateinit var keepVariablePlaceholderProviderUpdated: KeepVariablePlaceholderProviderUpdatedUseCase

    @Inject
    lateinit var getVariablePlaceholderPickerDialog: GetVariablePlaceholderPickerDialogUseCase

    init {
        getApplicationComponent().inject(this)
    }

    override var dialogState: DialogState?
        get() = currentViewState?.dialogState
        set(value) {
            updateViewState {
                copy(dialogState = value)
            }
        }

    override fun onInitializationStarted(data: Unit) {
        finalizeInitialization(silent = true)
    }

    override fun initViewState() = ResponseViewState()

    override fun onInitialized() {
        viewModelScope.launch {
            try {
                val temporaryShortcut = temporaryShortcutRepository.getTemporaryShortcut()
                initViewStateFromShortcut(temporaryShortcut)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onInitializationError(e)
            }
        }

        viewModelScope.launch {
            keepVariablePlaceholderProviderUpdated(::emitCurrentViewState)
        }
    }

    private fun initViewStateFromShortcut(shortcut: Shortcut) {
        val responseHandling = shortcut.responseHandling!!
        updateViewState {
            copy(
                successMessageHint = getSuccessMessageHint(shortcut),
                responseUiType = responseHandling.uiType,
                responseSuccessOutput = responseHandling.successOutput,
                responseFailureOutput = responseHandling.failureOutput,
                includeMetaInformation = responseHandling.includeMetaInfo,
                successMessage = responseHandling.successMessage,
                responseDisplayActions = responseHandling.displayActions,
                storeResponseIntoFile = responseHandling.storeDirectory != null,
                storeFileName = responseHandling.storeFileName.orEmpty(),
                replaceFileIfExists = responseHandling.replaceFileIfExists,
            )
        }
    }

    private fun getSuccessMessageHint(shortcut: Shortcut): Localizable =
        StringResLocalizable(
            R.string.executed,
            Localizable.create { context ->
                shortcut.name.ifEmpty { context.getString(R.string.shortcut_safe_name) }
            },
        )

    private fun onInitializationError(error: Throwable) {
        handleUnexpectedError(error)
        finish()
    }

    fun onResponseUiTypeChanged(responseUiType: String) {
        updateViewState {
            copy(responseUiType = responseUiType)
        }
        launchWithProgressTracking {
            temporaryShortcutRepository.setResponseUiType(responseUiType)
        }
    }

    fun onResponseSuccessOutputChanged(responseSuccessOutput: String) {
        updateViewState {
            copy(responseSuccessOutput = responseSuccessOutput)
        }
        launchWithProgressTracking {
            temporaryShortcutRepository.setResponseSuccessOutput(responseSuccessOutput)
        }
    }

    fun onResponseFailureOutputChanged(responseFailureOutput: String) {
        updateViewState {
            copy(responseFailureOutput = responseFailureOutput)
        }
        launchWithProgressTracking {
            temporaryShortcutRepository.setResponseFailureOutput(responseFailureOutput)
        }
    }

    fun onSuccessMessageChanged(successMessage: String) {
        updateViewState {
            copy(successMessage = successMessage)
        }
        launchWithProgressTracking {
            temporaryShortcutRepository.setResponseSuccessMessage(successMessage)
        }
    }

    fun onIncludeMetaInformationChanged(includeMetaInformation: Boolean) {
        updateViewState {
            copy(includeMetaInformation = includeMetaInformation)
        }
        launchWithProgressTracking {
            temporaryShortcutRepository.setResponseIncludeMetaInfo(includeMetaInformation)
        }
    }

    fun onShowActionButtonChanged(action: ResponseDisplayAction, show: Boolean) {
        doWithViewState { viewState ->
            if (viewState.responseUiType != ResponseHandling.UI_TYPE_WINDOW) {
                return@doWithViewState
            }
            val actions = listOf(
                ResponseDisplayAction.RERUN,
                ResponseDisplayAction.SHARE,
                ResponseDisplayAction.COPY,
                ResponseDisplayAction.SAVE,
            )
                .filter {
                    (it != action && it in viewState.responseDisplayActions) || (it == action && show)
                }
            updateViewState {
                copy(responseDisplayActions = actions)
            }
            launchWithProgressTracking {
                temporaryShortcutRepository.setDisplayActions(actions)
            }
        }
    }

    fun onSuccessMessageVariableButtonClicked() {
        dialogState = getVariablePlaceholderPickerDialog.invoke(
            onVariableSelected = {
                emitEvent(ResponseEvent.InsertVariablePlaceholderIntoSuccessMessage(it))
            },
            onEditVariableButtonClicked = {
                openActivity(
                    VariablesActivity.IntentBuilder()
                )
            },
        )
    }

    fun onDialogActionChanged(action: ResponseDisplayAction?) {
        doWithViewState { viewState ->
            if (viewState.responseUiType != ResponseHandling.UI_TYPE_DIALOG) {
                return@doWithViewState
            }
            val actions = action?.let(::listOf) ?: emptyList()
            updateViewState {
                copy(responseDisplayActions = actions)
            }
            launchWithProgressTracking {
                temporaryShortcutRepository.setDisplayActions(actions)
            }
        }
    }

    fun onBackPressed() {
        viewModelScope.launch {
            waitForOperationsToFinish()
            finish()
        }
    }

    fun onStoreIntoFileCheckboxChanged(enabled: Boolean) {
        doWithViewState { viewState ->
            if (enabled == viewState.storeResponseIntoFile) {
                return@doWithViewState
            }
            if (enabled) {
                emitEvent(ResponseEvent.PickDirectory)
            } else {
                updateViewState {
                    copy(storeResponseIntoFile = false)
                }
                launchWithProgressTracking {
                    temporaryShortcutRepository.setStoreDirectory(null)
                }
            }
        }
    }

    fun onStoreFileNameVariableButtonClicked() {
        dialogState = getVariablePlaceholderPickerDialog.invoke(
            onVariableSelected = {
                emitEvent(ResponseEvent.InsertVariablePlaceholderIntoFileName(it))
            },
            onEditVariableButtonClicked = {
                openActivity(
                    VariablesActivity.IntentBuilder()
                )
            },
        )
    }

    fun onStoreFileNameChanged(storeFileName: String) {
        updateViewState {
            copy(storeFileName = storeFileName)
        }
        launchWithProgressTracking {
            temporaryShortcutRepository.setStoreFileName(storeFileName)
        }
    }

    fun onStoreFileDirectoryPicked(directoryUri: Uri?) {
        updateViewState {
            copy(storeResponseIntoFile = directoryUri != null)
        }
        launchWithProgressTracking {
            temporaryShortcutRepository.setStoreDirectory(directoryUri)
        }
    }

    fun onStoreFileOverwriteChanged(enabled: Boolean) {
        updateViewState {
            copy(replaceFileIfExists = enabled)
        }
        launchWithProgressTracking {
            temporaryShortcutRepository.setStoreReplaceIfExists(enabled)
        }
    }
}
