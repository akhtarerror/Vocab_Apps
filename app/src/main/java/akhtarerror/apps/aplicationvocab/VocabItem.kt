package akhtarerror.apps.aplicationvocab

data class VocabItem(
    val id: Long = System.currentTimeMillis(),
    val indonesian: String,
    val english: String
)