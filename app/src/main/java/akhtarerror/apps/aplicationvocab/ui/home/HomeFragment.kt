package akhtarerror.apps.aplicationvocab.ui.home

import akhtarerror.apps.aplicationvocab.R
import akhtarerror.apps.aplicationvocab.adapter.VocabAdapter
import akhtarerror.apps.aplicationvocab.database.migration.DataMigration
import akhtarerror.apps.aplicationvocab.vocab.helper.VocabItemTouchHelperCallback
import akhtarerror.apps.aplicationvocab.vocab.item.VocabItem
import akhtarerror.apps.aplicationvocab.vocab.viewmodel.VocabViewModel
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var fabDelete: FloatingActionButton
    private lateinit var tvSelectedCount: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var adapter: VocabAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var vocabViewModel: VocabViewModel

    private var isSelectMode = false
    private var isMoveMode = false
    private val selectedItems = mutableSetOf<Long>() // Changed to Long for Room IDs
    private var currentVocabList = mutableListOf<VocabItem>()

    // Coroutine scope for the fragment
    private val fragmentScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupViewModel()
        setupRecyclerView()

        // Migrate data from SharedPreferences if needed
        migrateData()

        fabAdd.setOnClickListener {
            showAddEditDialog()
        }

        fabDelete.setOnClickListener {
            showBulkActionDialog()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentScope.cancel()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        fabAdd = view.findViewById(R.id.fabAdd)
        fabDelete = view.findViewById(R.id.fabDelete)
        tvSelectedCount = view.findViewById(R.id.tvSelectedCount)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
    }

    private fun setupViewModel() {
        vocabViewModel = ViewModelProvider(this)[VocabViewModel::class.java]

        // Observe vocabulary data changes
        vocabViewModel.allVocab.observe(viewLifecycleOwner) { vocabList ->
            currentVocabList.clear()
            currentVocabList.addAll(vocabList)
            adapter.submitList(vocabList.toList()) // Create new list for ListAdapter

            // Show/hide empty state
            if (vocabList.isEmpty()) {
                tvEmptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                tvEmptyState.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun migrateData() {
        fragmentScope.launch {
            try {
                DataMigration.Companion.migrateFromSharedPreferences(requireContext())
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error during data migration: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = VocabAdapter(
            isSelectMode = { isSelectMode },
            isMoveMode = { isMoveMode },
            selectedItems = selectedItems,
            onItemAction = { action, vocab, position ->
                when (action) {
                    VocabAdapter.Action.EDIT -> showAddEditDialog(vocab)
                    VocabAdapter.Action.DELETE -> deleteVocabItem(vocab)
                    VocabAdapter.Action.SELECT -> toggleItemSelection(vocab.id)
                    VocabAdapter.Action.CLICK -> {
                        if (isSelectMode) {
                            toggleItemSelection(vocab.id)
                        }
                    }

                    VocabAdapter.Action.MOVE -> {
                        // Handle move completion
                    }
                }
            }
        )

        // Setup ItemTouchHelper for drag and drop
        val itemTouchHelperCallback = VocabItemTouchHelperCallback(
            adapter = adapter,
            isMoveMode = { isMoveMode },
            onItemMoved = { fromPosition, toPosition ->
                val newList = adapter.moveItem(fromPosition, toPosition)
                // Update positions in database
                vocabViewModel.updateVocabPositions(newList)
            }
        )
        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Add touch listener for drag handle
        recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (isMoveMode && e.action == MotionEvent.ACTION_DOWN) {
                    val childView = rv.findChildViewUnder(e.x, e.y)
                    if (childView != null) {
                        val dragHandle = childView.findViewById<View>(R.id.dragHandle)
                        if (dragHandle != null && dragHandle.visibility == View.VISIBLE) {
                            val dragHandleRect = Rect()
                            dragHandle.getGlobalVisibleRect(dragHandleRect)

                            val location = IntArray(2)
                            rv.getLocationOnScreen(location)
                            val touchX = e.rawX.toInt() - location[0]
                            val touchY = e.rawY.toInt() - location[1]

                            if (dragHandleRect.contains(touchX + location[0], touchY + location[1])) {
                                val viewHolder = rv.getChildViewHolder(childView)
                                itemTouchHelper.startDrag(viewHolder)
                                return true
                            }
                        }
                    }
                }
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    private fun toggleMoveMode() {
        isMoveMode = !isMoveMode
        isSelectMode = false
        selectedItems.clear()
        updateUI()
        adapter.notifyDataSetChanged()
        activity?.invalidateOptionsMenu()

        val message = if (isMoveMode) {
            "Mode drag & drop aktif. Seret icon handle untuk memindahkan item"
        } else {
            "Mode drag & drop dinonaktifkan"
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showSearchDialog() {
        if (currentVocabList.isEmpty()) {
            Toast.makeText(context, "Tidak ada vocabulary untuk dicari", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_search_vocab, null)
        val etSearch = dialogView.findViewById<EditText>(R.id.etSearch)

        AlertDialog.Builder(requireContext())
            .setTitle("Cari Vocabulary")
            .setView(dialogView)
            .setPositiveButton("Cari") { _, _ ->
                val searchQuery = etSearch.text.toString().trim()
                if (searchQuery.isNotEmpty()) {
                    searchVocabulary(searchQuery)
                } else {
                    Toast.makeText(context, "Masukkan kata kunci pencarian", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showBulkActionDialog() {
        if (selectedItems.isEmpty()) {
            Toast.makeText(context, "Pilih minimal 1 item terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val options = arrayOf("Hapus Item", "Hapus Terjemahan Indonesia")

        AlertDialog.Builder(requireContext())
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
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Item")
            .setMessage("Apakah Anda yakin ingin menghapus ${selectedItems.size} item yang dipilih?")
            .setPositiveButton("Hapus") { _, _ ->
                bulkDeleteItems()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun confirmBulkDeleteIndonesian() {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Terjemahan Indonesia")
            .setMessage("Apakah Anda yakin ingin menghapus terjemahan bahasa Indonesia dari ${selectedItems.size} item yang dipilih?")
            .setPositiveButton("Hapus") { _, _ ->
                bulkDeleteIndonesian()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun bulkDeleteItems() {
        val selectedIds = selectedItems.toList()
        vocabViewModel.deleteVocabByIds(selectedIds)

        selectedItems.clear()
        toggleSelectMode()

        Toast.makeText(context, "${selectedIds.size} item berhasil dihapus", Toast.LENGTH_SHORT).show()
    }

    private fun bulkDeleteIndonesian() {
        val selectedIds = selectedItems.toList()
        vocabViewModel.clearIndonesianByIds(selectedIds)

        val count = selectedItems.size
        selectedItems.clear()
        toggleSelectMode()

        Toast.makeText(context, "Terjemahan Indonesia dari $count item berhasil dihapus", Toast.LENGTH_SHORT).show()
    }

    private fun showAddEditDialog(vocabItem: VocabItem? = null) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_edit_vocab, null)
        val etEnglish = dialogView.findViewById<EditText>(R.id.etEnglish)
        val etIndonesian = dialogView.findViewById<EditText>(R.id.etIndonesian)

        vocabItem?.let {
            etEnglish.setText(it.english)
            etIndonesian.setText(it.indonesian)
        }

        val title = if (vocabItem == null) "Tambah Vocabulary" else "Edit Vocabulary"

        AlertDialog.Builder(requireContext())
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
                        vocabViewModel.insertVocab(newItem)

                        // Scroll to bottom to show new item
                        recyclerView.postDelayed({
                            recyclerView.smoothScrollToPosition(adapter.itemCount - 1)
                        }, 100)

                        Toast.makeText(context, "Vocabulary berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    } else {
                        // Editing existing item
                        val updatedItem = vocabItem.copy(
                            english = english,
                            indonesian = indonesian
                        )
                        vocabViewModel.updateVocab(updatedItem)
                        Toast.makeText(context, "Vocabulary berhasil diupdate", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Harap isi field bahasa Inggris", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteVocabItem(vocab: VocabItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Vocabulary")
            .setMessage("Apakah Anda yakin ingin menghapus vocabulary ini?")
            .setPositiveButton("Hapus") { _, _ ->
                vocabViewModel.deleteVocab(vocab)
                Toast.makeText(context, "Vocabulary berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteAllIndonesianVocab() {
        val itemsWithIndonesian = currentVocabList.filter { it.indonesian.isNotEmpty() }

        if (itemsWithIndonesian.isEmpty()) {
            Toast.makeText(context, "Tidak ada vocabulary dengan bahasa Indonesia untuk dihapus", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Semua Bahasa Indonesia")
            .setMessage("Apakah Anda yakin ingin menghapus semua terjemahan bahasa Indonesia? (${itemsWithIndonesian.size} item)")
            .setPositiveButton("Hapus Semua") { _, _ ->
                vocabViewModel.clearAllIndonesian()
                Toast.makeText(context, "Semua terjemahan bahasa Indonesia berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun searchVocabulary(query: String) {
        val searchResults = mutableListOf<Pair<Int, Int>>()

        currentVocabList.forEachIndexed { index, vocab ->
            var matchScore = 0
            val lowerQuery = query.toLowerCase()

            val queryAsNumber = query.toIntOrNull()
            if (queryAsNumber != null && queryAsNumber == index + 1) {
                matchScore += 100
            }

            val englishLower = vocab.english.toLowerCase()
            when {
                englishLower == lowerQuery -> matchScore += 90
                englishLower.startsWith(lowerQuery) -> matchScore += 80
                englishLower.contains(lowerQuery) -> matchScore += 70
            }

            val indonesianLower = vocab.indonesian.toLowerCase()
            when {
                indonesianLower == lowerQuery -> matchScore += 90
                indonesianLower.startsWith(lowerQuery) -> matchScore += 80
                indonesianLower.contains(lowerQuery) -> matchScore += 70
            }

            if (matchScore > 0) {
                searchResults.add(Pair(index, matchScore))
            }
        }

        if (searchResults.isNotEmpty()) {
            searchResults.sortByDescending { it.second }
            val bestMatchPosition = searchResults.first().first

            recyclerView.smoothScrollToPosition(bestMatchPosition)

            val resultCount = searchResults.size
            val bestMatch = currentVocabList[bestMatchPosition]
            Toast.makeText(
                context,
                "Ditemukan $resultCount hasil. Menampilkan yang terbaik: ${bestMatch.english}",
                Toast.LENGTH_LONG
            ).show()

            recyclerView.postDelayed({
                adapter.notifyItemChanged(bestMatchPosition)
            }, 500)
        } else {
            Toast.makeText(context, "Tidak ditemukan vocabulary yang cocok dengan '$query'", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleSelectMode() {
        isSelectMode = !isSelectMode
        isMoveMode = false
        selectedItems.clear()
        updateUI()
        adapter.notifyDataSetChanged()
        activity?.invalidateOptionsMenu()
    }

    private fun toggleItemSelection(vocabId: Long) {
        if (selectedItems.contains(vocabId)) {
            selectedItems.remove(vocabId)
        } else {
            selectedItems.add(vocabId)
        }
        updateSelectedCountText()
        adapter.notifyDataSetChanged()
    }

    private fun selectAllItems() {
        if (currentVocabList.isEmpty()) {
            Toast.makeText(context, "Tidak ada item untuk dipilih", Toast.LENGTH_SHORT).show()
            return
        }

        val allSelected = selectedItems.size == currentVocabList.size

        if (allSelected) {
            selectedItems.clear()
            Toast.makeText(context, "Semua item dibatalkan", Toast.LENGTH_SHORT).show()
        } else {
            selectedItems.clear()
            currentVocabList.forEach { vocab ->
                selectedItems.add(vocab.id)
            }
            Toast.makeText(context, "Semua item dipilih", Toast.LENGTH_SHORT).show()
        }

        updateSelectedCountText()
        adapter.notifyDataSetChanged()
        activity?.invalidateOptionsMenu()
    }

    private fun updateUI() {
        when {
            isSelectMode -> {
                fabAdd.hide()
                fabDelete.show()
                tvSelectedCount.visibility = View.VISIBLE
                updateSelectedCountText()
            }
            isMoveMode -> {
                fabAdd.hide()
                fabDelete.hide()
                tvSelectedCount.visibility = View.GONE
            }
            else -> {
                fabAdd.show()
                fabDelete.hide()
                tvSelectedCount.visibility = View.GONE
            }
        }
    }

    private fun updateSelectedCountText() {
        val count = selectedItems.size
        tvSelectedCount.text = "$count item dipilih"
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)

        val selectItem = menu.findItem(R.id.action_select)
        val selectAllItem = menu.findItem(R.id.action_select_all)
        val searchItem = menu.findItem(R.id.action_search)
        val moveItem = menu.findItem(R.id.action_move_item)
        val deleteAllItem = menu.findItem(R.id.action_delete_all_indonesian)

        when {
            isSelectMode -> {
                selectItem?.setTitle("Batal")
                selectItem?.setIcon(R.drawable.ic_close)
                selectAllItem?.isVisible = true
                searchItem?.isVisible = false
                moveItem?.isVisible = false
                deleteAllItem?.isVisible = false

                val allSelected = selectedItems.size == currentVocabList.size && currentVocabList.isNotEmpty()
                if (allSelected) {
                    selectAllItem?.setTitle("Batal Pilih Semua")
                    selectAllItem?.setIcon(R.drawable.ic_deselect_all)
                } else {
                    selectAllItem?.setTitle("Pilih Semua")
                    selectAllItem?.setIcon(R.drawable.ic_select_all)
                }
            }
            isMoveMode -> {
                selectItem?.setTitle("Batal")
                selectItem?.setIcon(R.drawable.ic_close)
                selectAllItem?.isVisible = false
                searchItem?.isVisible = false
                moveItem?.isVisible = false
                deleteAllItem?.isVisible = false
            }
            else -> {
                selectItem?.setTitle("Select")
                selectItem?.setIcon(R.drawable.ic_select)
                selectAllItem?.isVisible = false
                searchItem?.isVisible = true
                moveItem?.isVisible = true
                deleteAllItem?.isVisible = true
            }
        }

        // Tint icons
        selectItem?.icon?.let { icon ->
            val wrappedIcon = DrawableCompat.wrap(icon)
            DrawableCompat.setTint(wrappedIcon, ContextCompat.getColor(requireContext(), android.R.color.white))
            selectItem.icon = wrappedIcon
        }

        selectAllItem?.icon?.let { icon ->
            val wrappedIcon = DrawableCompat.wrap(icon)
            DrawableCompat.setTint(wrappedIcon, ContextCompat.getColor(requireContext(), android.R.color.white))
            selectAllItem.icon = wrappedIcon
        }

        searchItem?.icon?.let { icon ->
            val wrappedIcon = DrawableCompat.wrap(icon)
            DrawableCompat.setTint(wrappedIcon, ContextCompat.getColor(requireContext(), android.R.color.white))
            searchItem.icon = wrappedIcon
        }

        moveItem?.icon?.let { icon ->
            val wrappedIcon = DrawableCompat.wrap(icon)
            DrawableCompat.setTint(wrappedIcon, ContextCompat.getColor(requireContext(), android.R.color.white))
            moveItem.icon = wrappedIcon
        }

        deleteAllItem?.icon?.let { icon ->
            val wrappedIcon = DrawableCompat.wrap(icon)
            DrawableCompat.setTint(wrappedIcon, ContextCompat.getColor(requireContext(), android.R.color.white))
            deleteAllItem.icon = wrappedIcon
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_select -> {
                if (isMoveMode) {
                    toggleMoveMode()
                } else {
                    toggleSelectMode()
                }
                true
            }
            R.id.action_select_all -> {
                selectAllItems()
                true
            }
            R.id.action_search -> {
                showSearchDialog()
                true
            }
            R.id.action_move_item -> {
                toggleMoveMode()
                true
            }
            R.id.action_delete_all_indonesian -> {
                deleteAllIndonesianVocab()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}