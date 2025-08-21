package akhtarerror.apps.aplicationvocab.ui.home

import akhtarerror.apps.aplicationvocab.R
import akhtarerror.apps.aplicationvocab.vocab.item.VocabItem
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class HomeFragmentDialogManager(private val fragment: HomeFragment) {

    fun showAddEditDialog(vocabItem: VocabItem? = null) {
        val dialogView = LayoutInflater.from(fragment.context).inflate(R.layout.dialog_add_edit_vocab, null)
        val etEnglish = dialogView.findViewById<EditText>(R.id.etEnglish)
        val etIndonesian = dialogView.findViewById<EditText>(R.id.etIndonesian)

        vocabItem?.let {
            etEnglish.setText(it.english)
            etIndonesian.setText(it.indonesian)
        }

        val title = if (vocabItem == null) "Tambah Vocabulary" else "Edit Vocabulary"

        AlertDialog.Builder(fragment.requireContext())
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val english = etEnglish.text.toString().trim()
                val indonesian = etIndonesian.text.toString().trim()

                if (english.isNotEmpty()) {
                    if (vocabItem == null) {
                        // Adding new item
                        val newItem = VocabItem(
                            english = english,
                            indonesian = indonesian
                        )
                        fragment.getVocabViewModel().insertVocab(newItem)

                        // Scroll to bottom to show new item
                        fragment.getRecyclerView().postDelayed({
                            fragment.getRecyclerView().smoothScrollToPosition(fragment.getAdapter().itemCount - 1)
                        }, 100)

                        Toast.makeText(fragment.context, "Vocabulary berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    } else {
                        // Editing existing item
                        val updatedItem = vocabItem.copy(
                            english = english,
                            indonesian = indonesian
                        )
                        fragment.getVocabViewModel().updateVocab(updatedItem)
                        Toast.makeText(fragment.context, "Vocabulary berhasil diupdate", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(fragment.context, "Harap isi field bahasa Inggris", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    fun showBulkAddDialog() {
        val dialogView = LayoutInflater.from(fragment.context).inflate(R.layout.dialog_bulk_add_vocab, null)
        val etBulkText = dialogView.findViewById<EditText>(R.id.etBulkText)

        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Tambah Vocabulary Massal")
            .setView(dialogView)
            .setMessage("Format: english = indonesian\nContoh:\nalways = selalu\nlove = cinta")
            .setPositiveButton("Tambah") { _, _ ->
                val bulkText = etBulkText.text.toString().trim()
                if (bulkText.isNotEmpty()) {
                    processBulkAdd(bulkText)
                } else {
                    Toast.makeText(fragment.context, "Harap masukkan data vocabulary", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun processBulkAdd(bulkText: String) {
        val lines = bulkText.split("\n")
        val validItems = mutableListOf<VocabItem>()
        var invalidCount = 0

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            if (trimmedLine.contains("=")) {
                val parts = trimmedLine.split("=", limit = 2)
                if (parts.size == 2) {
                    val english = parts[0].trim()
                    val indonesian = parts[1].trim()

                    if (english.isNotEmpty()) {
                        validItems.add(
                            VocabItem(
                                english = english,
                                indonesian = indonesian
                            )
                        )
                    } else {
                        invalidCount++
                    }
                } else {
                    invalidCount++
                }
            } else {
                // If no "=" found, treat as English only
                if (trimmedLine.isNotEmpty()) {
                    validItems.add(
                        VocabItem(
                            english = trimmedLine,
                            indonesian = ""
                        )
                    )
                }
            }
        }

        if (validItems.isNotEmpty()) {
            // Insert all valid items
            validItems.forEach { item ->
                fragment.getVocabViewModel().insertVocab(item)
            }

            // Scroll to bottom to show new items
            fragment.getRecyclerView().postDelayed({
                fragment.getRecyclerView().smoothScrollToPosition(fragment.getAdapter().itemCount - 1)
            }, 100)

            val successMessage = if (invalidCount > 0) {
                "${validItems.size} vocabulary berhasil ditambahkan, ${invalidCount} baris tidak valid diabaikan"
            } else {
                "${validItems.size} vocabulary berhasil ditambahkan"
            }

            Toast.makeText(fragment.context, successMessage, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(fragment.context, "Tidak ada data valid yang dapat ditambahkan", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteVocabItem(vocab: VocabItem) {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Hapus Vocabulary")
            .setMessage("Apakah Anda yakin ingin menghapus vocabulary ini?")
            .setPositiveButton("Hapus") { _, _ ->
                fragment.getVocabViewModel().deleteVocab(vocab)
                Toast.makeText(fragment.context, "Vocabulary berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    fun showBulkActionDialog() {
        val selectedItems = fragment.getSelectedItems()
        if (selectedItems.isEmpty()) {
            Toast.makeText(fragment.context, "Pilih minimal 1 item terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val options = arrayOf("Hapus Item", "Hapus Terjemahan Indonesia")

        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Pilih Aksi untuk ${selectedItems.size} item")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> confirmBulkDeleteItems()
                    1 -> confirmBulkDeleteIndonesian()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun confirmBulkDeleteItems() {
        val selectedItems = fragment.getSelectedItems()
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Hapus Item")
            .setMessage("Apakah Anda yakin ingin menghapus ${selectedItems.size} item yang dipilih?")
            .setPositiveButton("Hapus") { _, _ ->
                HomeFragmentSelectionManager(fragment).bulkDeleteItems()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun confirmBulkDeleteIndonesian() {
        val selectedItems = fragment.getSelectedItems()
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Hapus Terjemahan Indonesia")
            .setMessage("Apakah Anda yakin ingin menghapus terjemahan bahasa Indonesia dari ${selectedItems.size} item yang dipilih?")
            .setPositiveButton("Hapus") { _, _ ->
                HomeFragmentSelectionManager(fragment).bulkDeleteIndonesian()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    fun deleteAllIndonesianVocab() {
        val currentVocabList = fragment.getCurrentVocabList()
        val itemsWithIndonesian = currentVocabList.filter { it.indonesian.isNotEmpty() }

        if (itemsWithIndonesian.isEmpty()) {
            Toast.makeText(fragment.context, "Tidak ada vocabulary dengan bahasa Indonesia untuk dihapus", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Hapus Semua Bahasa Indonesia")
            .setMessage("Apakah Anda yakin ingin menghapus semua terjemahan bahasa Indonesia? (${itemsWithIndonesian.size} item)")
            .setPositiveButton("Hapus Semua") { _, _ ->
                fragment.getVocabViewModel().clearAllIndonesian()
                Toast.makeText(fragment.context, "Semua terjemahan bahasa Indonesia berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}