package akhtarerror.apps.aplicationvocab.ui.home

import android.widget.Toast

class HomeFragmentSelectionManager(private val fragment: HomeFragment) {

    fun toggleSelectMode() {
        fragment.setSelectMode(!fragment.isInSelectMode())
        fragment.setMoveMode(false)
        fragment.getSelectedItems().clear()
        fragment.activity?.let {
            HomeFragmentUIManager(fragment).updateUI()
            fragment.getAdapter().notifyDataSetChanged()
            it.invalidateOptionsMenu()
        }
    }

    fun toggleItemSelection(vocabId: Long) {
        val selectedItems = fragment.getSelectedItems()
        if (selectedItems.contains(vocabId)) {
            selectedItems.remove(vocabId)
        } else {
            selectedItems.add(vocabId)
        }
        HomeFragmentUIManager(fragment).updateSelectedCountText()
        fragment.getAdapter().notifyDataSetChanged()
    }

    fun selectAllItems() {
        val currentVocabList = fragment.getCurrentVocabList()
        val selectedItems = fragment.getSelectedItems()

        if (currentVocabList.isEmpty()) {
            Toast.makeText(fragment.context, "Tidak ada item untuk dipilih", Toast.LENGTH_SHORT).show()
            return
        }

        val allSelected = selectedItems.size == currentVocabList.size

        if (allSelected) {
            selectedItems.clear()
            Toast.makeText(fragment.context, "Semua item dibatalkan", Toast.LENGTH_SHORT).show()
        } else {
            selectedItems.clear()
            currentVocabList.forEach { vocab ->
                selectedItems.add(vocab.id)
            }
            Toast.makeText(fragment.context, "Semua item dipilih", Toast.LENGTH_SHORT).show()
        }

        HomeFragmentUIManager(fragment).updateSelectedCountText()
        fragment.getAdapter().notifyDataSetChanged()
        fragment.activity?.invalidateOptionsMenu()
    }

    fun bulkDeleteItems() {
        val selectedItems = fragment.getSelectedItems()
        val selectedIds = selectedItems.toList()
        fragment.getVocabViewModel().deleteVocabByIds(selectedIds)

        selectedItems.clear()
        toggleSelectMode()

        Toast.makeText(fragment.context, "${selectedIds.size} item berhasil dihapus", Toast.LENGTH_SHORT).show()
    }

    fun bulkDeleteIndonesian() {
        val selectedItems = fragment.getSelectedItems()
        val selectedIds = selectedItems.toList()
        fragment.getVocabViewModel().clearIndonesianByIds(selectedIds)

        val count = selectedItems.size
        selectedItems.clear()
        toggleSelectMode()

        Toast.makeText(fragment.context, "Terjemahan Indonesia dari $count item berhasil dihapus", Toast.LENGTH_SHORT).show()
    }
}