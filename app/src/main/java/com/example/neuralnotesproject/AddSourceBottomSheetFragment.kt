package com.example.neuralnotesproject

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddSourceBottomSheetFragment : BottomSheetDialogFragment() {

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Handle the selected file
                (parentFragment as? SourceActionListener)?.onFileSelected(uri)
            }
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_add_source, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btn_add_from_files).setOnClickListener {
            launchFilePicker()
        }

        view.findViewById<View>(R.id.btn_website_link).setOnClickListener {
            showWebsiteUrlInput()
        }

        view.findViewById<View>(R.id.btn_paste_text).setOnClickListener {
            showPasteNotes()
        }
    }

    private fun launchFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "text/plain"))
        }
        filePickerLauncher.launch(intent)
    }

    private fun showWebsiteUrlInput() {
        val websiteUrlFragment = WebsiteUrlInputFragment()
        websiteUrlFragment.show(parentFragmentManager, "WebsiteUrlInputFragment")
        dismiss()
    }

    private fun showPasteNotes() {
        (parentFragment as? SourcesFragment)?.showPasteNotes()
        dismiss()
    }

    companion object {
        const val TAG = "AddSourceBottomSheetFragment"
    }
}