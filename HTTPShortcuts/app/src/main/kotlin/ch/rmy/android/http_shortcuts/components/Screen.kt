package ch.rmy.android.http_shortcuts.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.rmy.android.framework.viewmodel.BaseViewModel
import ch.rmy.android.framework.viewmodel.ViewModelEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen(
    primaryColor: Int,
    onEvent: (ViewModelEvent) -> Unit,
    content: @Composable ScreenScope.() -> Unit,
) {
    val topAppBarColors = TopAppBarDefaults.smallTopAppBarColors(
        containerColor = Color(primaryColor),
        navigationIconContentColor = Color.White,
        titleContentColor = Color.White,
        actionIconContentColor = Color.White,
    )

    AppTheme {
        content(ScreenScope(topAppBarColors, onEvent))
    }
}

@Composable
inline fun <D, VS, reified VM : BaseViewModel<D, VS>> ScreenScope.bindViewModel(initData: D): Pair<VM, VS?> {
    val viewModel = viewModel<VM>()
    val state by viewModel.viewState.collectAsState(initial = null)
    LaunchedEffect(Unit) {
        viewModel.initialize(initData)
        viewModel.events.collect { event ->
            onEvent(event)
        }
    }
    return Pair(viewModel, state)
}

@OptIn(ExperimentalMaterial3Api::class)
class ScreenScope(
    val topAppBarColors: TopAppBarColors,
    val onEvent: (ViewModelEvent) -> Unit,
)
