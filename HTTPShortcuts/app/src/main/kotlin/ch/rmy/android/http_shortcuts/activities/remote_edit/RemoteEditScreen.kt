package ch.rmy.android.http_shortcuts.activities.remote_edit

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.components.ScreenScope
import ch.rmy.android.http_shortcuts.components.SimpleScaffold
import ch.rmy.android.http_shortcuts.components.ToolbarIcon
import ch.rmy.android.http_shortcuts.components.bindViewModel

@Composable
fun ScreenScope.RemoteEditScreen() {
    val (viewModel, state) = bindViewModel<Unit, RemoteEditViewState, RemoteEditViewModel>(Unit)

    SimpleScaffold(
        viewState = state,
        title = stringResource(R.string.settings_remote_edit),
        actions = {
            ToolbarIcon(
                Icons.Filled.Settings,
                contentDescription = stringResource(R.string.button_change_remote_server),
                onClick = viewModel::onChangeRemoteHostButtonClicked,
            )
        }
    ) { viewState ->
        RemoteEditContent(
            viewState,
            onPasswordChanged = viewModel::onPasswordChanged,
            onUploadButtonClicked = viewModel::onUploadButtonClicked,
            onDownloadButtonClicked = viewModel::onDownloadButtonClicked,
            onProgressDialogDismiss = viewModel::onDialogDismissalRequested,
            onServerUrlChange = viewModel::onServerUrlChange,
        )
    }
}
