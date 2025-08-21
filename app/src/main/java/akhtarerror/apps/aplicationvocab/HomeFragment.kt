package akhtarerror.apps.aplicationvocab

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var fabDelete: FloatingActionButton
    private lateinit var tvSelectedCount: TextView
    private lateinit var adapter: VocabAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var itemTouchHelper: ItemTouchHelper
    private val gson = Gson()

    private val vocabList = mutableListOf<VocabItem>()
    private var isSelectMode = false
    private var isMoveMode = false
    private val selectedItems = mutableSetOf<Int>()

    companion object {
        private const val PREF_NAME = "vocab_preferences"
        private const val VOCAB_LIST_KEY = "vocab_list"
    }

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
        setupRecyclerView()
        loadVocabList()

        fabAdd.setOnClickListener {
            showAddEditDialog()
        }

        fabDelete.setOnClickListener {
            showBulkActionDialog()
        }
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        fabAdd = view.findViewById(R.id.fabAdd)
        fabDelete = view.findViewById(R.id.fabDelete)
        tvSelectedCount = view.findViewById(R.id.tvSelectedCount)
        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun setupRecyclerView() {
        adapter = VocabAdapter(
            vocabList = vocabList,
            isSelectMode = { isSelectMode },
            isMoveMode = { isMoveMode },
            selectedItems = selectedItems,
            onItemAction = { action, position ->
                when (action) {
                    VocabAdapter.Action.EDIT -> showAddEditDialog(vocabList[position], position)
                    VocabAdapter.Action.DELETE -> deleteVocabItem(position)
                    VocabAdapter.Action.SELECT -> toggleItemSelection(position)
                    VocabAdapter.Action.CLICK -> {
                        if (isSelectMode) {
                            toggleItemSelection(position)
                        }
                    }
                }
            }
        )

        // Setup ItemTouchHelper for drag and drop
        val itemTouchHelperCallback = VocabItemTouchHelperCallback(
            adapter = adapter,
            isMoveMode = { isMoveMode },
            onItemMoved = { fromPosition, toPosition ->
                // Update the entire list to refresh numbering
                recyclerView.post {
                    adapter.notifyDataSetChanged()
                }
                saveVocabList()
            }
        )
        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Add touch listener to handle drag initiation from drag handle
        recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (isMoveMode && e.action == MotionEvent.ACTION_DOWN) {
                    val childView = rv.findChildViewUnder(e.x, e.y)
                    if (childView != null) {
                        val dragHandle = childView.findViewById<View>(R.id.dragHandle)
                        if (dragHandle != null && dragHandle.visibility == View.VISIBLE) {
                            val dragHandleRect = android.graphics.Rect()
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

        recyclerView.post {
            adapter.notifyDataSetChanged()
        }
        activity?.invalidateOptionsMenu()

        val message = if (isMoveMode) {
            "Mode drag & drop aktif. Seret icon handle untuk memindahkan item"
        } else {
            "Mode drag & drop dinonaktifkan"
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showSearchDialog() {
        if (vocabList.isEmpty()) {
            Toast.makeText(context, "Tidak ada vocabulary untuk dicari", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_search_vocab, null)
        val etSearch = dialogView.findViewById<android.widget.EditText>(R.id.etSearch)

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

    private fun searchVocabulary(query: String) {
        val searchResults = mutableListOf<Pair<Int, Int>>() // Pair<position, matchScore>

        vocabList.forEachIndexed { index, vocab ->
            var matchScore = 0
            val lowerQuery = query.toLowerCase()

            // Check if it's a number search
            val queryAsNumber = query.toIntOrNull()
            if (queryAsNumber != null && queryAsNumber == index + 1) {
                matchScore += 100 // Highest priority for exact number match
            }

            // Check English word
            val englishLower = vocab.english.toLowerCase()
            when {
                englishLower == lowerQuery -> matchScore += 90
                englishLower.startsWith(lowerQuery) -> matchScore += 80
                englishLower.contains(lowerQuery) -> matchScore += 70
            }

            // Check Indonesian word
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
            // Sort by match score (highest first)
            searchResults.sortByDescending { it.second }
            val bestMatchPosition = searchResults.first().first

            // Smooth scroll to the best match
            recyclerView.smoothScrollToPosition(bestMatchPosition)

            // Show results info
            val resultCount = searchResults.size
            val bestMatch = vocabList[bestMatchPosition]
            Toast.makeText(
                context,
                "Ditemukan $resultCount hasil. Menampilkan yang terbaik: ${bestMatch.english}",
                Toast.LENGTH_LONG
            ).show()

            // Optional: Highlight the found item briefly
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

        recyclerView.post {
            adapter.notifyDataSetChanged()
        }
        activity?.invalidateOptionsMenu()
    }

    private fun toggleItemSelection(position: Int) {
        if (position < 0 || position >= vocabList.size) return

        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }
        updateSelectedCountText()

        recyclerView.post {
            adapter.notifyItemChanged(position)
        }
    }

    private fun selectAllItems() {
        if (vocabList.isEmpty()) {
            Toast.makeText(context, "Tidak ada item untuk dipilih", Toast.LENGTH_SHORT).show()
            return
        }

        val allSelected = selectedItems.size == vocabList.size

        if (allSelected) {
            // If all items are selected, deselect all
            selectedItems.clear()
            Toast.makeText(context, "Semua item dibatalkan", Toast.LENGTH_SHORT).show()
        } else {
            // Select all items
            selectedItems.clear()
            for (i in vocabList.indices) {
                selectedItems.add(i)
            }
            Toast.makeText(context, "Semua item dipilih", Toast.LENGTH_SHORT).show()
        }

        updateSelectedCountText()
        recyclerView.post {
            adapter.notifyDataSetChanged()
        }
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
        val sortedPositions = selectedItems.sortedDescending()
        for (position in sortedPositions) {
            if (position >= 0 && position < vocabList.size) {
                vocabList.removeAt(position)
            }
        }

        selectedItems.clear()
        toggleSelectMode()

        recyclerView.post {
            adapter.notifyDataSetChanged()
        }
        saveVocabList()

        Toast.makeText(context, "${sortedPositions.size} item berhasil dihapus", Toast.LENGTH_SHORT).show()
    }

    private fun bulkDeleteIndonesian() {
        for (position in selectedItems) {
            if (position >= 0 && position < vocabList.size) {
                val item = vocabList[position]
                vocabList[position] = item.copy(indonesian = "")
            }
        }

        val count = selectedItems.size
        selectedItems.clear()
        toggleSelectMode()

        recyclerView.post {
            adapter.notifyDataSetChanged()
        }
        saveVocabList()

        Toast.makeText(context, "Terjemahan Indonesia dari $count item berhasil dihapus", Toast.LENGTH_SHORT).show()
    }

    private fun showAddEditDialog(vocabItem: VocabItem? = null, position: Int = -1) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_edit_vocab, null)
        val etEnglish = dialogView.findViewById<android.widget.EditText>(R.id.etEnglish)
        val etIndonesian = dialogView.findViewById<android.widget.EditText>(R.id.etIndonesian)

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
                        val newItem = VocabItem(english = english, indonesian = indonesian)
                        vocabList.add(newItem)

                        recyclerView.post {
                            adapter.notifyItemInserted(vocabList.size - 1)
                            // Scroll to the newly added item with smooth scrolling
                            recyclerView.smoothScrollToPosition(vocabList.size - 1)
                        }
                        saveVocabList()
                        Toast.makeText(context, "Vocabulary berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    } else {
                        // Editing existing item
                        if (position >= 0 && position < vocabList.size) {
                            vocabList[position] = vocabItem.copy(english = english, indonesian = indonesian)
                        }

                        recyclerView.post {
                            adapter.notifyItemChanged(position)
                        }
                        saveVocabList()
                        Toast.makeText(context, "Vocabulary berhasil diupdate", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Harap isi field bahasa Inggris", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteVocabItem(position: Int) {
        if (position < 0 || position >= vocabList.size) return

        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Vocabulary")
            .setMessage("Apakah Anda yakin ingin menghapus vocabulary ini?")
            .setPositiveButton("Hapus") { _, _ ->
                vocabList.removeAt(position)

                recyclerView.post {
                    adapter.notifyItemRemoved(position)
                    adapter.notifyItemRangeChanged(position, vocabList.size)
                }
                saveVocabList()
                Toast.makeText(context, "Vocabulary berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteAllIndonesianVocab() {
        val itemsWithIndonesian = vocabList.filter { it.indonesian.isNotEmpty() }

        if (itemsWithIndonesian.isEmpty()) {
            Toast.makeText(context, "Tidak ada vocabulary dengan bahasa Indonesia untuk dihapus", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Semua Bahasa Indonesia")
            .setMessage("Apakah Anda yakin ingin menghapus semua terjemahan bahasa Indonesia? (${itemsWithIndonesian.size} item)")
            .setPositiveButton("Hapus Semua") { _, _ ->
                vocabList.forEachIndexed { index, item ->
                    if (item.indonesian.isNotEmpty()) {
                        vocabList[index] = item.copy(indonesian = "")
                    }
                }

                recyclerView.post {
                    adapter.notifyDataSetChanged()
                }
                saveVocabList()
                Toast.makeText(context, "Semua terjemahan bahasa Indonesia berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
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

                // Update select all icon and title based on selection state
                val allSelected = selectedItems.size == vocabList.size && vocabList.isNotEmpty()
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

    private fun saveVocabList() {
        val json = gson.toJson(vocabList)
        sharedPreferences.edit().putString(VOCAB_LIST_KEY, json).apply()
    }

    private fun loadVocabList() {
        val json = sharedPreferences.getString(VOCAB_LIST_KEY, null)
        if (json != null) {
            val type = object : TypeToken<MutableList<VocabItem>>() {}.type
            val loadedList: MutableList<VocabItem> = gson.fromJson(json, type)
            vocabList.clear()
            vocabList.addAll(loadedList)

            recyclerView.post {
                adapter.notifyDataSetChanged()
            }
        }
    }
}