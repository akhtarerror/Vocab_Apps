package akhtarerror.apps.aplicationvocab.ui.home

import android.view.View

class HomeFragmentUIManager(private val fragment: HomeFragment) {

    fun updateUI() {
        when {
            fragment.isInSelectMode() -> {
                fragment.getFabAdd().hide()
                fragment.getFabDelete().show()
                fragment.getTvSelectedCount().visibility = View.VISIBLE
                updateSelectedCountText()
            }
            fragment.isInMoveMode() -> {
                fragment.getFabAdd().hide()
                fragment.getFabDelete().hide()
                fragment.getTvSelectedCount().visibility = View.GONE
            }
            else -> {
                fragment.getFabAdd().show()
                fragment.getFabDelete().hide()
                fragment.getTvSelectedCount().visibility = View.GONE
            }
        }
    }

    fun updateSelectedCountText() {
        val count = fragment.getSelectedItems().size
        fragment.getTvSelectedCount().text = "$count item dipilih"
    }
}