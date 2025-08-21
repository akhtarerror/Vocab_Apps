package akhtarerror.apps.aplicationvocab.ui.home

import akhtarerror.apps.aplicationvocab.R
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class HomeFragmentSearchManager(private val fragment: HomeFragment) {

    fun showSearchDialog() {
        val currentVocabList = fragment.getCurrentVocabList()
        if (currentVocabList.isEmpty()) {
            Toast.makeText(fragment.context, "Tidak ada vocabulary untuk dicari", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(fragment.context).inflate(R.layout.dialog_search_vocab, null)
        val etSearch = dialogView.findViewById<EditText>(R.id.etSearch)

        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Cari Vocabulary")
            .setView(dialogView)
            .setPositiveButton("Cari") { _, _ ->
                val searchQuery = etSearch.text.toString().trim()
                if (searchQuery.isNotEmpty()) {
                    searchVocabulary(searchQuery)
                } else {
                    Toast.makeText(fragment.context, "Masukkan kata kunci pencarian", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun searchVocabulary(query: String) {
        val currentVocabList = fragment.getCurrentVocabList()
        val searchResults = mutableListOf<Pair<Int, Int>>()

        currentVocabList.forEachIndexed { index, vocab ->
            var matchScore = 0
            val lowerQuery = query.lowercase()

            // Check for numeric index search
            val queryAsNumber = query.toIntOrNull()
            if (queryAsNumber != null && queryAsNumber == index + 1) {
                matchScore += 100
            }

            // Check English matches
            val englishLower = vocab.english.lowercase()
            when {
                englishLower == lowerQuery -> matchScore += 90
                englishLower.startsWith(lowerQuery) -> matchScore += 80
                englishLower.contains(lowerQuery) -> matchScore += 70
            }

            // Check Indonesian matches
            val indonesianLower = vocab.indonesian.lowercase()
            when {
                indonesianLower == lowerQuery -> matchScore += 90
                indonesianLower.startsWith(lowerQuery) -> matchScore += 80
                indonesianLower.contains(lowerQuery) -> matchScore += 70
            }

            if (matchScore > 0) {
                searchResults.add(Pair(index, matchScore))
            }
        }

        handleSearchResults(searchResults, query, currentVocabList)
    }

    private fun handleSearchResults(
        searchResults: List<Pair<Int, Int>>,
        query: String,
        currentVocabList: List<akhtarerror.apps.aplicationvocab.vocab.item.VocabItem>
    ) {
        if (searchResults.isNotEmpty()) {
            val sortedResults = searchResults.sortedByDescending { it.second }
            val bestMatchPosition = sortedResults.first().first

            fragment.getRecyclerView().smoothScrollToPosition(bestMatchPosition)

            val resultCount = searchResults.size
            val bestMatch = currentVocabList[bestMatchPosition]
            Toast.makeText(
                fragment.context,
                "Ditemukan $resultCount hasil. Menampilkan yang terbaik: ${bestMatch.english}",
                Toast.LENGTH_LONG
            ).show()

            // Highlight the result briefly
            fragment.getRecyclerView().postDelayed({
                fragment.getAdapter().notifyItemChanged(bestMatchPosition)
            }, 500)
        } else {
            Toast.makeText(
                fragment.context,
                "Tidak ditemukan vocabulary yang cocok dengan '$query'",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}