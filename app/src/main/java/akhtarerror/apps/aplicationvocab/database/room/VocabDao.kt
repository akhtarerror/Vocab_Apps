package akhtarerror.apps.aplicationvocab.database.room

import akhtarerror.apps.aplicationvocab.vocab.item.VocabItem
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabDao {

    @Query("SELECT * FROM vocab_items ORDER BY position ASC")
    fun getAllVocabFlow(): Flow<List<VocabItem>>

    @Query("SELECT * FROM vocab_items ORDER BY position ASC")
    suspend fun getAllVocab(): List<VocabItem>

    @Query("SELECT * FROM vocab_items WHERE id = :id")
    suspend fun getVocabById(id: Long): VocabItem?

    @Insert
    suspend fun insertVocab(vocab: VocabItem): Long

    @Update
    suspend fun updateVocab(vocab: VocabItem)

    @Delete
    suspend fun deleteVocab(vocab: VocabItem)

    @Query("DELETE FROM vocab_items WHERE id IN (:ids)")
    suspend fun deleteVocabByIds(ids: List<Long>)

    @Query("UPDATE vocab_items SET indonesian = '' WHERE id IN (:ids)")
    suspend fun clearIndonesianByIds(ids: List<Long>)

    @Query("UPDATE vocab_items SET indonesian = '' WHERE indonesian != ''")
    suspend fun clearAllIndonesian()

    @Query("DELETE FROM vocab_items")
    suspend fun deleteAllVocab()

    @Transaction
    suspend fun updateVocabPositions(vocabs: List<VocabItem>) {
        vocabs.forEach { vocab ->
            updateVocab(vocab)
        }
    }

    @Query("SELECT COUNT(*) FROM vocab_items")
    suspend fun getVocabCount(): Int
}