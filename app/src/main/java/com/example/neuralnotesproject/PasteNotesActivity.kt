package com.example.neuralnotesproject

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class PasteNotesActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PASTED_TEXT = "extra_pasted_text"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_paste_notes)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            onBackPressed()
        }

        findViewById<MaterialButton>(R.id.btn_insert).setOnClickListener {
            val pastedText = findViewById<TextInputEditText>(R.id.et_notes_input).text.toString()
            if (pastedText.isNotEmpty()) {
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_PASTED_TEXT, pastedText)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Please paste some text", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent()
        intent.putExtra("show_bottom_sheet", true)
        setResult(Activity.RESULT_CANCELED, intent)
    }
}
