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

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_add_source, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btn_add_website).setOnClickListener {
            showWebsiteUrlInput()
        }

        view.findViewById<View>(R.id.btn_paste_text).setOnClickListener {
            showPasteNotes()
        }
    }

    private fun showWebsiteUrlInput() {
        // Remove the creation of WebsiteUrlInputFragment
        // Instead, navigate to a new activity or fragment using layout_website_url_input.xml
        val intent = Intent(context, WebsiteUrlInputActivity::class.java)
        startActivityForResult(intent, WEBSITE_URL_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == WEBSITE_URL_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val url = data?.getStringExtra(WebsiteUrlInputActivity.EXTRA_WEBSITE_URL)
            url?.let {
                (parentFragment as? SourceActionListener)?.onWebsiteUrlSelected(it)
                dismiss()
            }
        }
    }

    private fun showPasteNotes() {
        (parentFragment as? SourcesFragment)?.showPasteNotes()
        dismiss()
    }

    companion object {
        const val TAG = "AddSourceBottomSheetFragment"
        private const val WEBSITE_URL_REQUEST_CODE = 1001
    }
}