package ch.rmy.android.http_shortcuts.activities.categories

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseActivity
import ch.rmy.android.http_shortcuts.data.models.Category
import ch.rmy.android.http_shortcuts.databinding.ActivityCategoriesBinding
import ch.rmy.android.http_shortcuts.dialogs.DialogBuilder
import ch.rmy.android.http_shortcuts.extensions.applyTheme
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.extensions.bindViewModel
import ch.rmy.android.http_shortcuts.extensions.consume
import ch.rmy.android.http_shortcuts.extensions.mapIf
import ch.rmy.android.http_shortcuts.extensions.openURL
import ch.rmy.android.http_shortcuts.extensions.showSnackbar
import ch.rmy.android.http_shortcuts.utils.BaseIntentBuilder
import ch.rmy.android.http_shortcuts.utils.DragOrderingHelper
import ch.rmy.android.http_shortcuts.utils.ExternalURLs
import ch.rmy.android.http_shortcuts.utils.LauncherShortcutManager
import ch.rmy.android.http_shortcuts.utils.PermissionManager

class CategoriesActivity : BaseActivity() {

    private val viewModel: CategoriesViewModel by bindViewModel()

    private lateinit var binding: ActivityCategoriesBinding

    private val categories by lazy { viewModel.getCategories() }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = applyBinding(ActivityCategoriesBinding.inflate(layoutInflater))
        setTitle(R.string.title_categories)
        initViews()

        viewModel.hasChanges.observe(this) {
            setResult(
                Activity.RESULT_OK,
                Intent().apply {
                    putExtra(EXTRA_CATEGORIES_CHANGED, it)
                },
            )
        }
    }

    private fun initViews() {
        val adapter = destroyer.own(CategoryAdapter(context, categories))

        val manager = LinearLayoutManager(context)
        binding.categoryList.layoutManager = manager
        binding.categoryList.setHasFixedSize(true)
        binding.categoryList.adapter = adapter

        adapter.clickListener = ::showContextMenu

        initDragOrdering()

        binding.buttonCreateCategory.applyTheme(themeHelper)
        binding.buttonCreateCategory.setOnClickListener { openCreateDialog() }
    }

    private fun initDragOrdering() {
        val dragOrderingHelper = DragOrderingHelper { categories.size > 1 }
        dragOrderingHelper.attachTo(binding.categoryList)
        dragOrderingHelper.positionChangeSource
            .concatMapCompletable { (oldPosition, newPosition) ->
                val category = categories[oldPosition]!!
                viewModel.moveCategory(category.id, newPosition)
            }
            .subscribe()
            .attachTo(destroyer)
    }

    private fun openCreateDialog() {
        DialogBuilder(context)
            .title(R.string.title_create_category)
            .textInput(
                hint = getString(R.string.placeholder_category_name),
                allowEmpty = false,
                maxLength = NAME_MAX_LENGTH,
                callback = ::createCategory
            )
            .showIfPossible()
    }

    private fun createCategory(name: String) {
        viewModel.createCategory(name)
            .subscribe {
                showSnackbar(R.string.message_category_created)
            }
            .attachTo(destroyer)
    }

    private fun showContextMenu(categoryData: LiveData<Category?>) {
        val category = categoryData.value ?: return
        DialogBuilder(context)
            .title(category.name)
            .item(R.string.action_rename) {
                showRenameDialog(categoryData)
            }
            .mapIf(category.hidden) {
                item(R.string.action_show_category) {
                    toggleCategoryHidden(categoryData, hidden = false)
                }
            }
            .mapIf(!category.hidden && categories.count { !it.hidden } > 1) {
                item(R.string.action_hide_category) {
                    toggleCategoryHidden(categoryData, hidden = true)
                }
            }
            .mapIf(!category.hidden) {
                item(R.string.action_change_category_layout_type) {
                    showLayoutTypeDialog(categoryData)
                }
                    .item(R.string.action_change_category_background) {
                        showBackgroundChangeDialog(categoryData)
                    }
            }
            .mapIf(!category.hidden && LauncherShortcutManager.supportsPinning(context)) {
                item(R.string.action_place_category) {
                    LauncherShortcutManager.pinCategory(context, categoryData.value ?: return@item)
                }
            }
            .mapIf(categories.size > 1) {
                item(R.string.action_delete) {
                    showDeleteDialog(categoryData)
                }
            }
            .showIfPossible()
    }

    private fun showRenameDialog(categoryData: LiveData<Category?>) {
        val category = categoryData.value ?: return
        DialogBuilder(context)
            .title(R.string.title_rename_category)
            .textInput(
                hint = getString(R.string.placeholder_category_name),
                prefill = category.name,
                allowEmpty = false,
                maxLength = NAME_MAX_LENGTH
            ) { input ->
                renameCategory(categoryData, input)
            }
            .showIfPossible()
    }

    private fun showLayoutTypeDialog(categoryData: LiveData<Category?>) {
        DialogBuilder(context)
            .item(R.string.layout_type_linear_list) {
                changeLayoutType(categoryData, Category.LAYOUT_LINEAR_LIST)
            }
            .item(R.string.layout_type_grid) {
                changeLayoutType(categoryData, Category.LAYOUT_GRID)
            }
            .showIfPossible()
    }

    private fun showBackgroundChangeDialog(categoryData: LiveData<Category?>) {
        DialogBuilder(context)
            .item(R.string.category_background_type_white) {
                changeBackgroundType(categoryData, Category.BACKGROUND_TYPE_WHITE)
            }
            .item(R.string.category_background_type_black) {
                changeBackgroundType(categoryData, Category.BACKGROUND_TYPE_BLACK)
            }
            .item(R.string.category_background_type_wallpaper) {
                PermissionManager.requestFileStoragePermissionIfNeeded(this)
                changeBackgroundType(categoryData, Category.BACKGROUND_TYPE_WALLPAPER)
            }
            .showIfPossible()
    }

    private fun toggleCategoryHidden(categoryData: LiveData<Category?>, hidden: Boolean) {
        val category = categoryData.value ?: return
        viewModel.toggleCategoryHidden(category.id, hidden)
            .subscribe {
                showSnackbar(if (hidden) R.string.message_category_hidden else R.string.message_category_visible)
            }
            .attachTo(destroyer)
    }

    private fun renameCategory(categoryData: LiveData<Category?>, newName: String) {
        val category = categoryData.value ?: return
        viewModel.renameCategory(category.id, newName)
            .subscribe {
                LauncherShortcutManager.updatePinnedCategoryShortcut(context, categoryData.value?.id ?: return@subscribe, newName)
                showSnackbar(R.string.message_category_renamed)
            }
            .attachTo(destroyer)
    }

    private fun changeLayoutType(categoryData: LiveData<Category?>, layoutType: String) {
        val category = categoryData.value ?: return
        viewModel.setLayoutType(category.id, layoutType)
            .subscribe {
                showSnackbar(R.string.message_layout_type_changed)
            }
            .attachTo(destroyer)
    }

    private fun changeBackgroundType(categoryData: LiveData<Category?>, backgroundType: String) {
        val category = categoryData.value ?: return
        viewModel.setBackground(category.id, backgroundType)
            .subscribe {
                showSnackbar(R.string.message_background_type_changed)
            }
            .attachTo(destroyer)
    }

    private fun showDeleteDialog(categoryData: LiveData<Category?>) {
        val category = categoryData.value ?: return
        if (category.shortcuts.isEmpty()) {
            deleteCategory(category)
            return
        }
        DialogBuilder(context)
            .message(R.string.confirm_delete_category_message)
            .positive(R.string.dialog_delete) {
                deleteCategory(categoryData.value ?: return@positive)
            }
            .negative(R.string.dialog_cancel)
            .showIfPossible()
    }

    private fun deleteCategory(category: Category) {
        viewModel.deleteCategory(category.id)
            .subscribe {
                showSnackbar(R.string.message_category_deleted)
            }
            .attachTo(destroyer)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.categories_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_show_help -> consume { showHelp() }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showHelp() {
        openURL(ExternalURLs.CATEGORIES_DOCUMENTATION)
    }

    class IntentBuilder(context: Context) : BaseIntentBuilder(context, CategoriesActivity::class.java)

    companion object {

        const val EXTRA_CATEGORIES_CHANGED = "categories_changed"

        private const val NAME_MAX_LENGTH = 20
    }
}
