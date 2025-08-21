package akhtarerror.apps.aplicationvocab

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VocabAdapter(
    private val vocabList: MutableList<VocabItem>,
    private val isSelectMode: () -> Boolean,
    private val isMoveMode: () -> Boolean,
    private val selectedItems: MutableSet<Int>,
    private val onItemAction: (Action, Int) -> Unit
) : RecyclerView.Adapter<VocabAdapter.VocabViewHolder>() {

    enum class Action {
        EDIT, DELETE, SELECT, CLICK
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
        val vocab = vocabList[position]

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
                // Select mode
                holder.btnEdit.visibility = View.GONE
                holder.btnDelete.visibility = View.GONE
                holder.dragHandle.visibility = View.GONE
                holder.checkBox.visibility = View.VISIBLE

                // Remove previous listener first to prevent unwanted triggers
                holder.checkBox.setOnCheckedChangeListener(null)

                // Set checkbox state without triggering listener
                holder.checkBox.isChecked = selectedItems.contains(position)

                // Set the listener after setting the state
                holder.checkBox.setOnCheckedChangeListener { _, _ ->
                    // Post the action to avoid RecyclerView layout conflicts
                    holder.itemView.post {
                        onItemAction(Action.SELECT, holder.adapterPosition)
                    }
                }

                // Make the entire item clickable for selection
                holder.itemView.setOnClickListener {
                    // Post the action to avoid RecyclerView layout conflicts
                    holder.itemView.post {
                        val currentPosition = holder.adapterPosition
                        if (currentPosition != RecyclerView.NO_POSITION) {
                            onItemAction(Action.CLICK, currentPosition)
                        }
                    }
                }
            }
            isMoveMode() -> {
                // Move mode - show drag handle
                holder.btnEdit.visibility = View.GONE
                holder.btnDelete.visibility = View.GONE
                holder.dragHandle.visibility = View.VISIBLE
                holder.checkBox.visibility = View.GONE

                // Remove click listener in move mode
                holder.itemView.setOnClickListener(null)
                holder.checkBox.setOnCheckedChangeListener(null)
            }
            else -> {
                // Normal mode
                holder.btnEdit.visibility = View.VISIBLE
                holder.btnDelete.visibility = View.VISIBLE
                holder.dragHandle.visibility = View.GONE
                holder.checkBox.visibility = View.GONE

                // Remove listeners in normal mode
                holder.itemView.setOnClickListener(null)
                holder.checkBox.setOnCheckedChangeListener(null)
            }
        }

        // Set up button click listeners
        holder.btnEdit.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                onItemAction(Action.EDIT, currentPosition)
            }
        }

        holder.btnDelete.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                onItemAction(Action.DELETE, currentPosition)
            }
        }
    }

    override fun getItemCount(): Int = vocabList.size

    // Method to move item for drag and drop
    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                vocabList[i] = vocabList[i + 1].also { vocabList[i + 1] = vocabList[i] }
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                vocabList[i] = vocabList[i - 1].also { vocabList[i - 1] = vocabList[i] }
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }
}