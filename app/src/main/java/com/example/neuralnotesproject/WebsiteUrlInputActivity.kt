package com.example.neuralnotesproject

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class WebsiteUrlInputActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_website_url_input)

        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val etUrlInput = findViewById<TextInputEditText>(R.id.et_url_input)
        val btnInsert = findViewById<MaterialButton>(R.id.btn_insert)

        btnBack.setOnClickListener {
            finish()
        }

        btnInsert.setOnClickListener {
            val url = etUrlInput.text.toString()
            if (url.isNotEmpty()) {
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_WEBSITE_URL, url)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_WEBSITE_URL = "extra_website_url"
    }
}