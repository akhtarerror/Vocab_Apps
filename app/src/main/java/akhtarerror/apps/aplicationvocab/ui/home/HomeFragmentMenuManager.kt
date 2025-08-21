package akhtarerror.apps.aplicationvocab.ui.home

import akhtarerror.apps.aplicationvocab.R
import android.view.Menu
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

class HomeFragmentMenuManager(private val fragment: HomeFragment) {

    fun setupMenu(menu: Menu) {
        val selectItem = menu.findItem(R.id.action_select)
        val selectAllItem = menu.findItem(R.id.action_select_all)
        val searchItem = menu.findItem(R.id.action_search)
        val moveItem = menu.findItem(R.id.action_move_item)
        val deleteAllItem = menu.findItem(R.id.action_delete_all_indonesian)
        val bulkAddItem = menu.findItem(R.id.action_bulk_add)

        when {
            fragment.isInSelectMode() -> {
                setupSelectModeMenu(selectItem, selectAllItem, searchItem, moveItem, deleteAllItem, bulkAddItem)
            }
            fragment.isInMoveMode() -> {
                setupMoveModeMenu(selectItem, selectAllItem, searchItem, moveItem, deleteAllItem, bulkAddItem)
            }
            else -> {
                setupNormalModeMenu(selectItem, selectAllItem, searchItem, moveItem, deleteAllItem, bulkAddItem)
            }
        }

        // Apply icon tinting
        tintMenuIcons(selectItem, selectAllItem, searchItem, moveItem, deleteAllItem, bulkAddItem)
    }

    private fun setupSelectModeMenu(
        selectItem: MenuItem?,
        selectAllItem: MenuItem?,
        searchItem: MenuItem?,
        moveItem: MenuItem?,
        deleteAllItem: MenuItem?,
        bulkAddItem: MenuItem?
    ) {
        selectItem?.setTitle("Batal")
        selectItem?.setIcon(R.drawable.ic_close)
        selectAllItem?.isVisible = true
        searchItem?.isVisible = false
        moveItem?.isVisible = false
        deleteAllItem?.isVisible = false
        bulkAddItem?.isVisible = false

        val allSelected = fragment.getSelectedItems().size == fragment.getCurrentVocabList().size &&
                fragment.getCurrentVocabList().isNotEmpty()

        if (allSelected) {
            selectAllItem?.setTitle("Batal Pilih Semua")
            selectAllItem?.setIcon(R.drawable.ic_deselect_all)
        } else {
            selectAllItem?.setTitle("Pilih Semua")
            selectAllItem?.setIcon(R.drawable.ic_select_all)
        }
    }

    private fun setupMoveModeMenu(
        selectItem: MenuItem?,
        selectAllItem: MenuItem?,
        searchItem: MenuItem?,
        moveItem: MenuItem?,
        deleteAllItem: MenuItem?,
        bulkAddItem: MenuItem?
    ) {
        selectItem?.setTitle("Batal")
        selectItem?.setIcon(R.drawable.ic_close)
        selectAllItem?.isVisible = false
        searchItem?.isVisible = false
        moveItem?.isVisible = false
        deleteAllItem?.isVisible = false
        bulkAddItem?.isVisible = false
    }

    private fun setupNormalModeMenu(
        selectItem: MenuItem?,
        selectAllItem: MenuItem?,
        searchItem: MenuItem?,
        moveItem: MenuItem?,
        deleteAllItem: MenuItem?,
        bulkAddItem: MenuItem?
    ) {
        selectItem?.setTitle("Pilih Item")
        selectItem?.setIcon(R.drawable.ic_select)
        selectAllItem?.isVisible = false
        searchItem?.isVisible = true
        moveItem?.isVisible = true
        deleteAllItem?.isVisible = true
        bulkAddItem?.isVisible = true
    }

    private fun tintMenuIcons(
        selectItem: MenuItem?,
        selectAllItem: MenuItem?,
        searchItem: MenuItem?,
        moveItem: MenuItem?,
        deleteAllItem: MenuItem?,
        bulkAddItem: MenuItem?
    ) {
        val whiteColor = ContextCompat.getColor(fragment.requireContext(), android.R.color.white)

        selectItem?.icon?.let { icon ->
            val wrappedIcon = DrawableCompat.wrap(icon)
            DrawableCompat.setTint(wrappedIcon, whiteColor)
            selectItem.icon = wrappedIcon
        }

        selectAllItem?.icon?.let { icon ->
            val wrappedIcon = DrawableCompat.wrap(icon)
            DrawableCompat.setTint(wrappedIcon, whiteColor)
            selectAllItem.icon = wrappedIcon
        }

        searchItem?.icon?.let { icon ->
            val wrappedIcon = DrawableCompat.wrap(icon)
            DrawableCompat.setTint(wrappedIcon, whiteColor)
            searchItem.icon = wrappedIcon
        }

        moveItem?.icon?.let { icon ->
            val wrappedIcon = DrawableCompat.wrap(icon)
            DrawableCompat.setTint(wrappedIcon, whiteColor)
            moveItem.icon = wrappedIcon
        }

        deleteAllItem?.icon?.let { icon ->
            val wrappedIcon = DrawableCompat.wrap(icon)
            DrawableCompat.setTint(wrappedIcon, whiteColor)
            deleteAllItem.icon = wrappedIcon
        }

        bulkAddItem?.icon?.let { icon ->
            val wrappedIcon = DrawableCompat.wrap(icon)
            DrawableCompat.setTint(wrappedIcon, whiteColor)
            bulkAddItem.icon = wrappedIcon
        }
    }

    fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_select -> {
                if (fragment.isInMoveMode()) {
                    fragment.toggleMoveMode()
                } else {
                    HomeFragmentSelectionManager(fragment).toggleSelectMode()
                }
                true
            }
            R.id.action_select_all -> {
                HomeFragmentSelectionManager(fragment).selectAllItems()
                true
            }
            R.id.action_search -> {
                HomeFragmentSearchManager(fragment).showSearchDialog()
                true
            }
            R.id.action_move_item -> {
                fragment.toggleMoveMode()
                true
            }
            R.id.action_delete_all_indonesian -> {
                HomeFragmentDialogManager(fragment).deleteAllIndonesianVocab()
                true
            }
            R.id.action_bulk_add -> {
                HomeFragmentDialogManager(fragment).showBulkAddDialog()
                true
            }
            else -> false
        }
    }
}