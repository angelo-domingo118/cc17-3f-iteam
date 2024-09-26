package com.example.neuralnotesproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddSourceBottomSheetFragment : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_add_source, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btn_add_from_files).setOnClickListener {
            // Handle add from files
        }

        view.findViewById<View>(R.id.btn_website_link).setOnClickListener {
            showWebsiteUrlInput()
        }

        view.findViewById<View>(R.id.btn_paste_text).setOnClickListener {
            showPasteNotes()
        }
    }

    private fun showWebsiteUrlInput() {
        val websiteUrlFragment = WebsiteUrlInputFragment()
        websiteUrlFragment.show(parentFragmentManager, "WebsiteUrlInputFragment")
        dismiss()
    }

    private fun showPasteNotes() {
        val pasteNotesFragment = PasteNotesFragment()
        pasteNotesFragment.show(parentFragmentManager, "PasteNotesFragment")
        dismiss()
    }

    companion object {
        const val TAG = "AddSourceBottomSheetFragment"
    }
}