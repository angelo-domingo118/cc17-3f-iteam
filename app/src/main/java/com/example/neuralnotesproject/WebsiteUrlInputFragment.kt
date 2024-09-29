package com.example.neuralnotesproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText

class WebsiteUrlInputFragment : DialogFragment() {

    interface WebsiteUrlListener {
        fun onWebsiteUrlEntered(url: String)
    }

    private var listener: WebsiteUrlListener? = null

    fun setWebsiteUrlListener(listener: WebsiteUrlListener) {
        this.listener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_website_url_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val urlInput = view.findViewById<TextInputEditText>(R.id.et_website_url)
        val submitButton = view.findViewById<Button>(R.id.btn_submit_url)

        submitButton.setOnClickListener {
            val url = urlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                listener?.onWebsiteUrlEntered(url)
                dismiss()
            }
        }
    }
}