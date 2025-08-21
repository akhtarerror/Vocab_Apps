package akhtarerror.apps.aplicationvocab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class TestFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvDescription = view.findViewById<TextView>(R.id.tvDescription)

        tvTitle.text = "Test Fragment"
        tvDescription.text = "Ini adalah halaman Test.\n\nDi sini nanti bisa ditambahkan fitur-fitur seperti:\n• Quiz vocabulary\n• Practice mode\n• Multiple choice questions\n• Fill in the blank\n• Voice recognition test"
    }
}