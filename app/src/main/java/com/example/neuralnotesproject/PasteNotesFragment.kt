package com.example.neuralnotesproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class PasteNotesFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_paste_notes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            dismiss()
            AddSourceBottomSheetFragment().show(parentFragmentManager, AddSourceBottomSheetFragment.TAG)
        }

        view.findViewById<MaterialButton>(R.id.btn_insert).setOnClickListener {
            val pastedText = view.findViewById<TextInputEditText>(R.id.et_notes_input).text.toString()
            if (pastedText.isNotEmpty()) {
                // TODO: Handle pasted text insertion
                Toast.makeText(context, "Text inserted: ${pastedText.take(20)}...", Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(context, "Please paste some text", Toast.LENGTH_SHORT).show()
            }
        }
    }
}