package com.example.neuralnotesproject

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.BulletSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.getSpans
import com.example.neuralnotesproject.NotesFragment.Companion.EXTRA_NOTE_ID
import com.google.android.material.button.MaterialButton

class EditNoteActivity : AppCompatActivity() {

    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteContent: EditText
    private lateinit var btnBold: ImageButton
    private lateinit var btnItalic: ImageButton
    private lateinit var btnUnderline: ImageButton
    private lateinit var btnStrikethrough: ImageButton
    private lateinit var btnBulletList: ImageButton
    private lateinit var btnNumberedList: ImageButton
    private lateinit var btnIndent: ImageButton
    private lateinit var fabSave: MaterialButton

    private var isBold = false
    private var isItalic = false
    private var isUnderline = false
    private var isStrikethrough = false
    private var isBulletList = false
    private var isNumberedList = false

    private var noteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screen_edit_note)

        initViews()
        setupListeners()

        // Add back button functionality
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            onBackPressed()
        }

        // Load existing note data if available
        noteId = intent.getStringExtra(NotesFragment.EXTRA_NOTE_ID)
        if (noteId != null) {
            etNoteTitle.setText(intent.getStringExtra(NotesFragment.EXTRA_NOTE_TITLE))
            etNoteContent.setText(intent.getStringExtra(NotesFragment.EXTRA_NOTE_CONTENT))
        }
    }

    // Add override for back press to handle unsaved changes
    override fun onBackPressed() {
        // Check if there are unsaved changes
        val title = etNoteTitle.text.toString().trim()
        val content = etNoteContent.text.toString().trim()

        if (title.isNotEmpty() || content.isNotEmpty()) {
            // Show confirmation dialog
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("Do you want to save your changes?")
                .setPositiveButton("Save") { _, _ -> saveNote() }
                .setNegativeButton("Discard") { _, _ -> super.onBackPressed() }
                .setNeutralButton("Cancel", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }

    private fun initViews() {
        etNoteTitle = findViewById(R.id.et_note_title)
        etNoteContent = findViewById(R.id.et_note_content)
        btnBold = findViewById(R.id.btn_bold)
        btnItalic = findViewById(R.id.btn_italic)
        btnUnderline = findViewById(R.id.btn_underline)
        btnStrikethrough = findViewById(R.id.btn_strikethrough)
        btnBulletList = findViewById(R.id.btn_bullet_list)
        btnNumberedList = findViewById(R.id.btn_numbered_list)
        btnIndent = findViewById(R.id.btn_indent)
        fabSave = findViewById(R.id.fab_save)
    }

    private fun setupListeners() {
        btnBold.setOnClickListener { toggleStyle(Typeface.BOLD) }
        btnItalic.setOnClickListener { toggleStyle(Typeface.ITALIC) }
        btnUnderline.setOnClickListener { toggleUnderline() }
        btnStrikethrough.setOnClickListener { toggleStrikethrough() }
        btnBulletList.setOnClickListener { toggleBulletList() }
        btnNumberedList.setOnClickListener { toggleNumberedList() }
        btnIndent.setOnClickListener { applyIndentation() }
        fabSave.setOnClickListener { saveNote() }

        etNoteContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count == 1 && s?.subSequence(start, start + count) == "\n") {
                    handleNewLine(start + count)
                } else if (before == 1 && count == 0 && start > 0 && s?.get(start - 1) == '\n') {
                    handleBackspace(start)
                }
            }
            override fun afterTextChanged(s: Editable?) {
                applyCurrentFormatting(s)
            }
        })
    }

    private fun toggleStyle(style: Int) {
        when (style) {
            Typeface.BOLD -> isBold = !isBold
            Typeface.ITALIC -> isItalic = !isItalic
        }
        updateButtonStates()
        updateSpans()
    }

    private fun toggleUnderline() {
        isUnderline = !isUnderline
        updateButtonStates()
        updateSpans()
    }

    private fun toggleStrikethrough() {
        isStrikethrough = !isStrikethrough
        updateButtonStates()
        updateSpans()
    }

    private fun toggleBulletList() {
        isBulletList = !isBulletList
        isNumberedList = false
        updateButtonStates()
        applyListStyle()
    }

    private fun toggleNumberedList() {
        isNumberedList = !isNumberedList
        isBulletList = false
        updateButtonStates()
        applyListStyle()
    }

    private fun updateButtonStates() {
        btnBold.isSelected = isBold
        btnItalic.isSelected = isItalic
        btnUnderline.isSelected = isUnderline
        btnStrikethrough.isSelected = isStrikethrough
        btnBulletList.isSelected = isBulletList
        btnNumberedList.isSelected = isNumberedList
    }

    private fun applyCurrentFormatting(editable: Editable?) {
        editable?.let { text ->
            val selectionStart = etNoteContent.selectionStart
            val selectionEnd = etNoteContent.selectionEnd
            
            if (selectionStart != selectionEnd) {
                // Apply formatting to selected text
                applySpans(text, selectionStart, selectionEnd)
            } else {
                // Apply formatting to newly typed text
                val start = text.getSpans<StyleSpan>(0, text.length).lastOrNull()?.let { span ->
                    text.getSpanEnd(span)
                } ?: 0
                applySpans(text, start, text.length)
            }
        }
    }

    private fun applySpans(text: Editable, start: Int, end: Int) {
        if (isBold) text.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (isItalic) text.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (isUnderline) text.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (isStrikethrough) text.setSpan(StrikethroughSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun handleNewLine(cursorPosition: Int) {
        val text = etNoteContent.text
        val lineStart = text.lastIndexOf('\n', cursorPosition - 2) + 1
        val currentLine = text.subSequence(lineStart, cursorPosition - 1).toString()
        
        when {
            currentLine.startsWith("• ") -> text.insert(cursorPosition, "\n• ")
            currentLine.matches(Regex("\\d+\\.\\s.*")) -> {
                val nextNumber = currentLine.substringBefore('.').toInt() + 1
                text.insert(cursorPosition, "\n$nextNumber. ")
            }
        }
    }

    private fun handleBackspace(cursorPosition: Int) {
        val text = etNoteContent.text
        val lineStart = text.lastIndexOf('\n', cursorPosition - 2) + 1
        val currentLine = text.subSequence(lineStart, cursorPosition - 1).toString()
        
        if (currentLine.matches(Regex("^[•\\d]+\\.?\\s*$"))) {
            text.delete(lineStart, cursorPosition)
            if (currentLine.startsWith("• ")) {
                isBulletList = false
            } else if (currentLine.matches(Regex("\\d+\\.\\s"))) {
                isNumberedList = false
            }
            updateButtonStates()
        }
    }

    private fun applyListStyle() {
        val text = etNoteContent.text
        val selectionStart = etNoteContent.selectionStart
        val selectionEnd = etNoteContent.selectionEnd
        
        val lines = text.toString().substring(selectionStart, selectionEnd).split("\n")
        val styledText = SpannableStringBuilder()
        
        lines.forEachIndexed { index, line ->
            if (index > 0) styledText.append("\n")
            when {
                isBulletList -> styledText.append("• $line")
                isNumberedList -> styledText.append("${index + 1}. $line")
                else -> styledText.append(line)
            }
        }
        
        text.replace(selectionStart, selectionEnd, styledText)
    }

    private fun applyIndentation() {
        val text = etNoteContent.text
        val selectionStart = etNoteContent.selectionStart
        val lineStart = text.lastIndexOf('\n', selectionStart - 1) + 1
        text.insert(lineStart, "    ")
    }

    private fun saveNote() {
        val title = etNoteTitle.text.toString().trim()
        val content = etNoteContent.text.toString().trim()

        if (title.isNotEmpty() && content.isNotEmpty()) {
            val resultIntent = Intent().apply {
                putExtra(EXTRA_NOTE_ID, intent.getStringExtra(EXTRA_NOTE_ID))
                putExtra(EXTRA_NOTE_TITLE, title)
                putExtra(EXTRA_NOTE_CONTENT, content)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } else {
            Toast.makeText(this, "Title and content cannot be empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSpans() {
        val text = etNoteContent.text
        val selectionStart = etNoteContent.selectionStart
        val selectionEnd = etNoteContent.selectionEnd

        if (selectionStart != selectionEnd) {
            // Remove existing spans in the selection
            text.getSpans<StyleSpan>(selectionStart, selectionEnd).forEach { text.removeSpan(it) }
            text.getSpans<UnderlineSpan>(selectionStart, selectionEnd).forEach { text.removeSpan(it) }
            text.getSpans<StrikethroughSpan>(selectionStart, selectionEnd).forEach { text.removeSpan(it) }

            // Apply new spans
            applySpans(text, selectionStart, selectionEnd)
        } else {
            applyCurrentFormatting(text)
        }
    }

    companion object {
        const val EXTRA_NOTE_TITLE = "extra_note_title"
        const val EXTRA_NOTE_CONTENT = "extra_note_content"
    }
}
