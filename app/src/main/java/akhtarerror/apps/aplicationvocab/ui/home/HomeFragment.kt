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
import android.widget.TextView
import android.widget.Toast
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
    private val selectedItems = mutableSetOf<Long>()
    private var currentVocabList = mutableListOf<VocabItem>()

    // Coroutine scope for the fragment
    private val fragmentScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Delegate classes
    private val uiManager by lazy { HomeFragmentUIManager(this) }
    private val selectionManager by lazy { HomeFragmentSelectionManager(this) }
    private val dialogManager by lazy { HomeFragmentDialogManager(this) }
    private val searchManager by lazy { HomeFragmentSearchManager(this) }
    private val menuManager by lazy { HomeFragmentMenuManager(this) }

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
        migrateData()
        setupClickListeners()
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
            adapter.submitList(vocabList.toList())

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

    private fun setupClickListeners() {
        fabAdd.setOnClickListener {
            dialogManager.showAddEditDialog()
        }

        fabDelete.setOnClickListener {
            dialogManager.showBulkActionDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = VocabAdapter(
            isSelectMode = { isSelectMode },
            isMoveMode = { isMoveMode },
            selectedItems = selectedItems,
            onItemAction = { action, vocab, position ->
                when (action) {
                    VocabAdapter.Action.EDIT -> dialogManager.showAddEditDialog(vocab)
                    VocabAdapter.Action.DELETE -> dialogManager.deleteVocabItem(vocab)
                    VocabAdapter.Action.SELECT -> selectionManager.toggleItemSelection(vocab.id)
                    VocabAdapter.Action.CLICK -> {
                        if (isSelectMode) {
                            selectionManager.toggleItemSelection(vocab.id)
                        }
                    }
                    VocabAdapter.Action.MOVE -> {
                        // Handle move completion
                    }
                }
            }
        )

        setupItemTouchHelper()
        setupRecyclerViewTouchListener()

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun setupItemTouchHelper() {
        val itemTouchHelperCallback = VocabItemTouchHelperCallback(
            adapter = adapter,
            isMoveMode = { isMoveMode },
            onItemMoved = { fromPosition, toPosition ->
                val newList = adapter.moveItem(fromPosition, toPosition)
                vocabViewModel.updateVocabPositions(newList)
            }
        )
        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun setupRecyclerViewTouchListener() {
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

    fun toggleMoveMode() {
        isMoveMode = !isMoveMode
        isSelectMode = false
        selectedItems.clear()
        uiManager.updateUI()
        adapter.notifyDataSetChanged()
        activity?.invalidateOptionsMenu()

        val message = if (isMoveMode) {
            "Mode drag & drop aktif. Seret icon handle untuk memindahkan item"
        } else {
            "Mode drag & drop dinonaktifkan"
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        menuManager.setupMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return menuManager.handleMenuItemSelected(item) || super.onOptionsItemSelected(item)
    }

    // Getter methods for delegate classes
    fun getRecyclerView() = recyclerView
    fun getFabAdd() = fabAdd
    fun getFabDelete() = fabDelete
    fun getTvSelectedCount() = tvSelectedCount
    fun getTvEmptyState() = tvEmptyState
    fun getAdapter() = adapter
    fun getVocabViewModel() = vocabViewModel
    fun getCurrentVocabList() = currentVocabList
    fun getSelectedItems() = selectedItems
    fun isInSelectMode() = isSelectMode
    fun isInMoveMode() = isMoveMode
    fun setSelectMode(selectMode: Boolean) { isSelectMode = selectMode }
    fun setMoveMode(moveMode: Boolean) { isMoveMode = moveMode }
}