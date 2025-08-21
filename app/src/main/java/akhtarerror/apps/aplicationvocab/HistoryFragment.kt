package akhtarerror.apps.aplicationvocab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class HistoryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvDescription = view.findViewById<TextView>(R.id.tvDescription)

        tvTitle.text = "History Fragment"
        tvDescription.text = "Ini adalah halaman History.\n\nDi sini nanti bisa ditambahkan fitur-fitur seperti:\n• Riwayat tes yang sudah dilakukan\n• Skor dan progress\n• Statistik pembelajaran\n• Achievement dan badges\n• Timeline aktivitas"
    }
}