package akhtarerror.apps.aplicationvocab.ui.test

import akhtarerror.apps.aplicationvocab.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class TestFragment : Fragment() {

    private lateinit var rgDuration: RadioGroup
    private lateinit var rgQuestionCount: RadioGroup
    private lateinit var btnStart: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
    }

    private fun initViews(view: View) {
        rgDuration = view.findViewById(R.id.rgDuration)
        rgQuestionCount = view.findViewById(R.id.rgQuestionCount)
        btnStart = view.findViewById(R.id.btnStart)
    }

    private fun setupClickListeners() {
        btnStart.setOnClickListener {
            val selectedDuration = getSelectedDuration()
            val selectedQuestionCount = getSelectedQuestionCount()

            if (selectedDuration.isNotEmpty() && selectedQuestionCount.isNotEmpty()) {
                Toast.makeText(
                    context,
                    "Memulai ujian: $selectedQuestionCount soal dalam $selectedDuration",
                    Toast.LENGTH_SHORT
                ).show()
                // TODO: Navigate to quiz activity/fragment
            } else {
                Toast.makeText(
                    context,
                    "Silakan pilih durasi dan jumlah soal terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getSelectedDuration(): String {
        val selectedId = rgDuration.checkedRadioButtonId
        return if (selectedId != -1) {
            val radioButton = view?.findViewById<RadioButton>(selectedId)
            radioButton?.text.toString()
        } else {
            ""
        }
    }

    private fun getSelectedQuestionCount(): String {
        val selectedId = rgQuestionCount.checkedRadioButtonId
        return if (selectedId != -1) {
            val radioButton = view?.findViewById<RadioButton>(selectedId)
            radioButton?.text.toString()
        } else {
            ""
        }
    }
}