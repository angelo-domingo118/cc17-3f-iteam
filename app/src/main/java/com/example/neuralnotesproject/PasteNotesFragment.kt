package com.example.neuralnotesproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.util.UUID
import android.util.Log
import com.example.neuralnotesproject.data.Source
import com.example.neuralnotesproject.data.SourceType

class PasteNotesFragment : Fragment() {

    private var notebookId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notebookId = arguments?.getString("notebookId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_paste_notes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<MaterialButton>(R.id.btn_insert).setOnClickListener {
            val pastedText = view.findViewById<TextInputEditText>(R.id.et_notes_input).text.toString()
            if (pastedText.isNotEmpty()) {
                savePastedTextAsSource(pastedText)
                parentFragmentManager.popBackStack()  // Changed from dismiss() to popBackStack()
            } else {
                Toast.makeText(context, "Please paste some text", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePastedTextAsSource(text: String) {
        notebookId?.let { id ->
            val sourceId = UUID.randomUUID().toString()
            val title = extractTitle(text)
            
            val source = Source(
                id = sourceId,
                name = title,
                type = SourceType.PASTE_TEXT,
                content = text,
                filePath = null,  // No file path for PASTE_TEXT type
                notebookId = id
            )
            
            val sourcesFragment = parentFragmentManager.fragments
                .firstOrNull { it is SourcesFragment } as? SourcesFragment
            sourcesFragment?.addSource(source)
        }
    }

    /**
     * Extracts a title from the pasted text.
     * Uses the first line or a substring of the first 30 characters.
     *
     * @param text The full pasted text.
     * @param maxLength The maximum length of the title.
     * @return A string to be used as the title.
     */
    private fun extractTitle(text: String, maxLength: Int = 30): String {
        val firstLine = text.lineSequence().firstOrNull()?.trim() ?: ""
        return if (firstLine.length > maxLength) {
            firstLine.substring(0, maxLength) + "..."
        } else if (firstLine.isNotEmpty()) {
            firstLine
        } else {
            if (text.length > maxLength) {
                text.substring(0, maxLength) + "..."
            } else {
                text
            }
        }
    }
}
