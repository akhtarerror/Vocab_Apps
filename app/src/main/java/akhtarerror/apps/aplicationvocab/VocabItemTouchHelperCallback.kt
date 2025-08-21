package akhtarerror.apps.aplicationvocab

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class VocabItemTouchHelperCallback(
    private val adapter: VocabAdapter,
    private val isMoveMode: () -> Boolean,
    private val onItemMoved: (fromPosition: Int, toPosition: Int) -> Unit
) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return if (isMoveMode()) {
            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
            makeMovementFlags(dragFlags, 0)
        } else {
            0
        }
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.adapterPosition
        val toPosition = target.adapterPosition

        if (fromPosition != RecyclerView.NO_POSITION && toPosition != RecyclerView.NO_POSITION) {
            adapter.moveItem(fromPosition, toPosition)
            return true
        }
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Not used for drag and drop
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

        when (actionState) {
            ItemTouchHelper.ACTION_STATE_DRAG -> {
                // Scale up the item being dragged
                viewHolder?.itemView?.alpha = 0.8f
                viewHolder?.itemView?.scaleX = 1.05f
                viewHolder?.itemView?.scaleY = 1.05f
            }
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        // Reset the item appearance
        viewHolder.itemView.alpha = 1.0f
        viewHolder.itemView.scaleX = 1.0f
        viewHolder.itemView.scaleY = 1.0f

        // Notify that the item has been moved (to update numbering)
        val position = viewHolder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
            onItemMoved(position, position) // This will trigger save and number update
        }
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false // We'll handle drag initiation manually through the drag handle
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }
}