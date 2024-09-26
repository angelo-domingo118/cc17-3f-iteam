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

class WebsiteUrlInputFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_website_url_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            dismiss()
            AddSourceBottomSheetFragment().show(parentFragmentManager, AddSourceBottomSheetFragment.TAG)
        }

        view.findViewById<MaterialButton>(R.id.btn_insert).setOnClickListener {
            val urlInput = view.findViewById<TextInputEditText>(R.id.et_url_input).text.toString()
            if (urlInput.isNotEmpty()) {
                // TODO: Handle URL insertion
                Toast.makeText(context, "URL inserted: $urlInput", Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(context, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
            }
        }
    }
}