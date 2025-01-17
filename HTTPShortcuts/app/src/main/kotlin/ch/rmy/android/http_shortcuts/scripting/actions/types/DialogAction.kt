package ch.rmy.android.http_shortcuts.scripting.actions.types

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.dagger.ApplicationComponent
import ch.rmy.android.http_shortcuts.databinding.DialogTextBinding
import ch.rmy.android.http_shortcuts.extensions.reloadImageSpans
import ch.rmy.android.http_shortcuts.extensions.showAndAwaitDismissal
import ch.rmy.android.http_shortcuts.scripting.ExecutionContext
import ch.rmy.android.http_shortcuts.utils.ActivityProvider
import ch.rmy.android.http_shortcuts.utils.DialogBuilder
import ch.rmy.android.http_shortcuts.utils.HTMLUtil
import ch.rmy.android.http_shortcuts.variables.Variables
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DialogAction(private val message: String, private val title: String) : BaseAction() {

    @Inject
    lateinit var activityProvider: ActivityProvider

    override fun inject(applicationComponent: ApplicationComponent) {
        applicationComponent.inject(this)
    }

    override suspend fun execute(executionContext: ExecutionContext) {
        val finalMessage = Variables.rawPlaceholdersToResolvedValues(
            message,
            executionContext.variableManager.getVariableValuesByIds(),
        )
        if (finalMessage.isEmpty()) {
            return
        }

        withContext(Dispatchers.Main) {
            val activity = activityProvider.getActivity()
            val view = DialogTextBinding.inflate(LayoutInflater.from(activity))
            val textView = view.text
            textView.text = HTMLUtil.formatWithImageSupport(
                string = finalMessage,
                context = activity,
                onImageLoaded = textView::reloadImageSpans,
                coroutineScope = this,
            )
            textView.movementMethod = LinkMovementMethod.getInstance()
            DialogBuilder(activity)
                .title(title)
                .view(view.root)
                .positive(R.string.dialog_ok)
                .showAndAwaitDismissal()
        }
    }
}
