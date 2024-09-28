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
import java.io.File
import java.util.UUID
import android.util.Log

class PasteNotesFragment : DialogFragment() {

    private var notebookId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle)
        notebookId = arguments?.getString("notebookId")
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
                savePastedTextAsSource(pastedText)
                dismiss()
            } else {
                Toast.makeText(context, "Please paste some text", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePastedTextAsSource(text: String) {
        notebookId?.let { id ->
            val sourceId = UUID.randomUUID().toString()
            val sourceDir = File(requireContext().filesDir, "$id/sources")
            sourceDir.mkdirs()

            val file = File(sourceDir, "$sourceId.txt")
            file.writeText("Pasted Text\n$text")

            // Update SourcesFragment
            val sourcesFragment = parentFragmentManager.fragments.firstOrNull { it is SourcesFragment } as? SourcesFragment
            sourcesFragment?.addSource(
                Source(
                    id = sourceId,
                    name = "Pasted Text",
                    type = SourceType.TEXT,
                    content = text
                )
            )

            Toast.makeText(context, "Text saved as source", Toast.LENGTH_SHORT).show()
        } ?: run {
            Log.e("PasteNotesFragment", "Notebook ID is null")
            Toast.makeText(context, "Error: Notebook ID not found", Toast.LENGTH_SHORT).show()
        }
    }
}