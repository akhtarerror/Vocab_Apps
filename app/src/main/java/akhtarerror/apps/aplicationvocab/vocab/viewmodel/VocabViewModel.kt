package akhtarerror.apps.aplicationvocab.vocab.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import akhtarerror.apps.aplicationvocab.vocab.item.VocabItem
import akhtarerror.apps.aplicationvocab.database.room.VocabDatabase
import akhtarerror.apps.aplicationvocab.database.repository.VocabRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VocabViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VocabRepository
    val allVocab: LiveData<List<VocabItem>>

    init {
        val vocabDao = VocabDatabase.getDatabase(application).vocabDao()
        repository = VocabRepository(vocabDao)
        allVocab = repository.getAllVocabFlow().asLiveData()
    }

    fun insertVocab(vocab: VocabItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertVocab(vocab)
    }

    fun updateVocab(vocab: VocabItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateVocab(vocab)
    }

    fun deleteVocab(vocab: VocabItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteVocab(vocab)
    }

    fun deleteVocabByIds(ids: List<Long>) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteVocabByIds(ids)
    }

    fun clearIndonesianByIds(ids: List<Long>) = viewModelScope.launch(Dispatchers.IO) {
        repository.clearIndonesianByIds(ids)
    }

    fun clearAllIndonesian() = viewModelScope.launch(Dispatchers.IO) {
        repository.clearAllIndonesian()
    }

    fun updateVocabPositions(vocabs: List<VocabItem>) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateVocabPositions(vocabs)
    }

    suspend fun getVocabById(id: Long): VocabItem? {
        return repository.getVocabById(id)
    }
}
