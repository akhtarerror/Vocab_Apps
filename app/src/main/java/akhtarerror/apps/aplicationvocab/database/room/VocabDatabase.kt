package akhtarerror.apps.aplicationvocab.database.room

import akhtarerror.apps.aplicationvocab.vocab.item.VocabItem
import akhtarerror.apps.aplicationvocab.database.room.VocabDao
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VocabItem::class],
    version = 1,
    exportSchema = false
)
abstract class VocabDatabase : RoomDatabase() {

    abstract fun vocabDao(): VocabDao

    companion object {
        @Volatile
        private var INSTANCE: VocabDatabase? = null

        fun getDatabase(context: Context): VocabDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VocabDatabase::class.java,
                    "vocab_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}