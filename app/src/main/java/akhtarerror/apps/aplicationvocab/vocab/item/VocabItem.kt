package akhtarerror.apps.aplicationvocab.vocab.item

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vocab_items")
data class VocabItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val english: String,
    val indonesian: String,
    val position: Int = 0 // For maintaining order
)