package akhtarerror.apps.aplicationvocab.vocab.helper

import akhtarerror.apps.aplicationvocab.adapter.VocabAdapter
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
            // Don't update adapter directly, just notify about the move
            onItemMoved(fromPosition, toPosition)
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
                viewHolder?.itemView?.elevation = 8f
            }
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        // Reset the item appearance
        viewHolder.itemView.alpha = 1.0f
        viewHolder.itemView.scaleX = 1.0f
        viewHolder.itemView.scaleY = 1.0f
        viewHolder.itemView.elevation = 2f
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false // We handle drag initiation manually through the drag handle
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }

    override fun canDropOver(recyclerView: RecyclerView, current: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return isMoveMode()
    }
}