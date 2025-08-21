package akhtarerror.apps.aplicationvocab.database.repository

import akhtarerror.apps.aplicationvocab.vocab.item.VocabItem
import akhtarerror.apps.aplicationvocab.database.room.VocabDao
import kotlinx.coroutines.flow.Flow

class VocabRepository(private val vocabDao: VocabDao) {

    fun getAllVocabFlow(): Flow<List<VocabItem>> = vocabDao.getAllVocabFlow()

    suspend fun getAllVocab(): List<VocabItem> = vocabDao.getAllVocab()

    suspend fun getVocabById(id: Long): VocabItem? = vocabDao.getVocabById(id)

    suspend fun insertVocab(vocab: VocabItem): Long {
        val count = vocabDao.getVocabCount()
        val vocabWithPosition = vocab.copy(position = count)
        return vocabDao.insertVocab(vocabWithPosition)
    }

    suspend fun updateVocab(vocab: VocabItem) = vocabDao.updateVocab(vocab)

    suspend fun deleteVocab(vocab: VocabItem) = vocabDao.deleteVocab(vocab)

    suspend fun deleteVocabByIds(ids: List<Long>) = vocabDao.deleteVocabByIds(ids)

    suspend fun clearIndonesianByIds(ids: List<Long>) = vocabDao.clearIndonesianByIds(ids)

    suspend fun clearAllIndonesian() = vocabDao.clearAllIndonesian()

    suspend fun deleteAllVocab() = vocabDao.deleteAllVocab()

    suspend fun updateVocabPositions(vocabs: List<VocabItem>) {
        val updatedVocabs = vocabs.mapIndexed { index, vocab ->
            vocab.copy(position = index)
        }
        vocabDao.updateVocabPositions(updatedVocabs)
    }

    suspend fun getVocabCount(): Int = vocabDao.getVocabCount()
}