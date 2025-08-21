package akhtarerror.apps.aplicationvocab.database.migration

import android.content.Context
import android.content.SharedPreferences
import akhtarerror.apps.aplicationvocab.vocab.item.VocabItem
import akhtarerror.apps.aplicationvocab.database.room.VocabDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataMigration {

    companion object {
        private const val PREF_NAME = "vocab_preferences"
        private const val VOCAB_LIST_KEY = "vocab_list"
        private const val MIGRATION_COMPLETED_KEY = "migration_completed"

        suspend fun migrateFromSharedPreferences(context: Context) {
            withContext(Dispatchers.IO) {
                val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

                // Check if migration is already completed
                if (sharedPreferences.getBoolean(MIGRATION_COMPLETED_KEY, false)) {
                    return@withContext
                }

                val json = sharedPreferences.getString(VOCAB_LIST_KEY, null)
                if (json != null) {
                    try {
                        val gson = Gson()
                        val type = object : TypeToken<MutableList<VocabItem>>() {}.type
                        val vocabList: MutableList<VocabItem> = gson.fromJson(json, type)

                        // Get database instance
                        val database = VocabDatabase.getDatabase(context)
                        val vocabDao = database.vocabDao()

                        // Check if database is empty before migrating
                        val existingCount = vocabDao.getVocabCount()
                        if (existingCount == 0 && vocabList.isNotEmpty()) {
                            // Insert all vocabulary items with proper positions
                            vocabList.forEachIndexed { index, vocab ->
                                val vocabWithPosition = vocab.copy(position = index)
                                vocabDao.insertVocab(vocabWithPosition)
                            }
                        }

                        // Mark migration as completed
                        sharedPreferences.edit()
                            .putBoolean(MIGRATION_COMPLETED_KEY, true)
                            .apply()

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    // No data to migrate, just mark as completed
                    sharedPreferences.edit()
                        .putBoolean(MIGRATION_COMPLETED_KEY, true)
                        .apply()
                }
            }
        }
    }
}
