package akhtarerror.apps.aplicationvocab.adapter

import akhtarerror.apps.aplicationvocab.R
import akhtarerror.apps.aplicationvocab.vocab.item.VocabItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class VocabAdapter(
    private val isSelectMode: () -> Boolean,
    private val isMoveMode: () -> Boolean,
    private val selectedItems: MutableSet<Long>, // Changed to Long for Room IDs
    private val onItemAction: (Action, VocabItem, Int) -> Unit // Pass VocabItem instead of just position
) : ListAdapter<VocabItem, VocabAdapter.VocabViewHolder>(VocabDiffCallback()) {

    enum class Action {
        EDIT, DELETE, SELECT, CLICK, MOVE
    }

    class VocabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNumber: TextView = itemView.findViewById(R.id.tvNumber)
        val tvEnglish: TextView = itemView.findViewById(R.id.tvEnglish)
        val tvIndonesian: TextView = itemView.findViewById(R.id.tvIndonesian)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        val dragHandle: ImageView = itemView.findViewById(R.id.dragHandle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VocabViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vocab, parent, false)
        return VocabViewHolder(view)
    }

    override fun onBindViewHolder(holder: VocabViewHolder, position: Int) {
        val vocab = getItem(position)

        // Set the number (position + 1 for 1-based numbering)
        holder.tvNumber.text = "${position + 1}."

        holder.tvEnglish.text = vocab.english
        holder.tvIndonesian.text = if (vocab.indonesian.isNotEmpty()) {
            vocab.indonesian
        } else {
            "Belum ada terjemahan"
        }

        // Change text color based on whether Indonesian translation exists
        if (vocab.indonesian.isNotEmpty()) {
            holder.tvIndonesian.setTextColor(holder.itemView.context.getColor(android.R.color.primary_text_light))
        } else {
            holder.tvIndonesian.setTextColor(holder.itemView.context.getColor(android.R.color.secondary_text_light))
        }

        // Handle different modes
        when {
            isSelectMode() -> {
                holder.btnEdit.visibility = View.GONE
                holder.btnDelete.visibility = View.GONE
                holder.dragHandle.visibility = View.GONE
                holder.checkBox.visibility = View.VISIBLE

                holder.checkBox.setOnCheckedChangeListener(null)
                holder.checkBox.isChecked = selectedItems.contains(vocab.id)

                holder.checkBox.setOnCheckedChangeListener { _, _ ->
                    holder.itemView.post {
                        onItemAction(Action.SELECT, vocab, holder.adapterPosition)
                    }
                }

                holder.itemView.setOnClickListener {
                    holder.itemView.post {
                        val currentPosition = holder.adapterPosition
                        if (currentPosition != RecyclerView.NO_POSITION) {
                            onItemAction(Action.CLICK, vocab, currentPosition)
                        }
                    }
                }
            }
            isMoveMode() -> {
                holder.btnEdit.visibility = View.GONE
                holder.btnDelete.visibility = View.GONE
                holder.dragHandle.visibility = View.VISIBLE
                holder.checkBox.visibility = View.GONE

                holder.itemView.setOnClickListener(null)
                holder.checkBox.setOnCheckedChangeListener(null)
            }
            else -> {
                holder.btnEdit.visibility = View.VISIBLE
                holder.btnDelete.visibility = View.VISIBLE
                holder.dragHandle.visibility = View.GONE
                holder.checkBox.visibility = View.GONE

                holder.itemView.setOnClickListener(null)
                holder.checkBox.setOnCheckedChangeListener(null)
            }
        }

        // Set up button click listeners
        holder.btnEdit.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                onItemAction(Action.EDIT, vocab, currentPosition)
            }
        }

        holder.btnDelete.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                onItemAction(Action.DELETE, vocab, currentPosition)
            }
        }
    }

    // Method to move item for drag and drop
    fun moveItem(fromPosition: Int, toPosition: Int): List<VocabItem> {
        val currentList = currentList.toMutableList()
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                currentList[i] = currentList[i + 1].also { currentList[i + 1] = currentList[i] }
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                currentList[i] = currentList[i - 1].also { currentList[i - 1] = currentList[i] }
            }
        }
        return currentList
    }

    class VocabDiffCallback : DiffUtil.ItemCallback<VocabItem>() {
        override fun areItemsTheSame(oldItem: VocabItem, newItem: VocabItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: VocabItem, newItem: VocabItem): Boolean {
            return oldItem == newItem
        }
    }
}