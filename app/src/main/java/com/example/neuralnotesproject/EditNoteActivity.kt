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
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
    private lateinit var btnCheckbox: ImageButton

    private var isBold = false
    private var isItalic = false
    private var isUnderline = false
    private var isStrikethrough = false
    private var isBulletList = false
    private var isNumberedList = false
    private var isCheckboxList = false

    private var noteId: String? = null

    private var currentListNumber = 1

    companion object {
        const val EXTRA_NOTE_TITLE = "extra_note_title"
        const val EXTRA_NOTE_CONTENT = "extra_note_content"
        const val CHECKBOX_UNCHECKED = "⬜"
        const val CHECKBOX_CHECKED = "☑️"
        const val CHECKBOX_SIZE = 36f
        const val CHECKBOX_PADDING = 16f
    }

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
        val title = etNoteTitle.text.toString().trim()
        val content = etNoteContent.text.toString().trim()

        if (title.isNotEmpty() || content.isNotEmpty()) {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_unsaved_changes, null)
            val dialog = AlertDialog.Builder(this, R.style.CustomMaterialDialog)
                .setView(dialogView)
                .create()

            // Save button
            dialogView.findViewById<MaterialButton>(R.id.btn_save).setOnClickListener {
                saveNote()
                dialog.dismiss()
            }

            // Discard button
            dialogView.findViewById<MaterialButton>(R.id.btn_discard).setOnClickListener {
                dialog.dismiss()
                super.onBackPressed()
            }

            // Cancel button
            dialogView.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
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
        btnCheckbox = findViewById(R.id.btn_checkbox)

        etNoteContent.apply {
            textSize = 18f
            setLineSpacing(CHECKBOX_PADDING, 1.2f)
        }
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
        btnCheckbox.setOnClickListener { toggleCheckboxList() }

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

        etNoteContent.setOnClickListener { view ->
            val position = etNoteContent.selectionStart
            val text = etNoteContent.text
            val lineStart = text.lastIndexOf('\n', position - 1).let { if (it == -1) 0 else it + 1 }
            
            // Check if click was near checkbox
            if (position >= lineStart && position <= lineStart + 2) {
                val currentLine = text.substring(lineStart, text.indexOf('\n', position).let { if (it == -1) text.length else it })
                if (currentLine.startsWith(CHECKBOX_UNCHECKED) || currentLine.startsWith(CHECKBOX_CHECKED)) {
                    toggleCheckboxState(position)
                }
            }
        }
    }

    private fun toggleStyle(style: Int) {
        val text = etNoteContent.text as Spannable
        val selectionStart = etNoteContent.selectionStart
        val selectionEnd = etNoteContent.selectionEnd

        if (selectionStart == selectionEnd) {
            // No text selected, track state for future input
            when (style) {
                Typeface.BOLD -> isBold = !isBold
                Typeface.ITALIC -> isItalic = !isItalic
            }
            updateButtonStates()
        } else {
            // Text is selected, apply/remove formatting
            val spans = text.getSpans<StyleSpan>(selectionStart, selectionEnd)
            val hasStyle = spans.any { it.style == style }

            // Remove existing style spans in selection
            spans.filter { it.style == style }.forEach { text.removeSpan(it) }

            // Apply new span if style wasn't present
            if (!hasStyle) {
                text.setSpan(
                    StyleSpan(style),
                    selectionStart,
                    selectionEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            // Update button state based on current selection
            when (style) {
                Typeface.BOLD -> isBold = !hasStyle
                Typeface.ITALIC -> isItalic = !hasStyle
            }
            updateButtonStates()
        }
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
        val text = etNoteContent.text
        val selectionStart = etNoteContent.selectionStart
        val selectionEnd = etNoteContent.selectionEnd

        if (selectionStart == selectionEnd) {
            // Single line conversion
            val lineStart = text.lastIndexOf('\n', selectionStart - 1).let { if (it == -1) 0 else it + 1 }
            val lineEnd = text.indexOf('\n', selectionStart).let { if (it == -1) text.length else it }
            val currentLine = text.substring(lineStart, lineEnd)

            if (currentLine.startsWith("• ")) {
                // Remove bullet
                text.delete(lineStart, lineStart + 2)
                isBulletList = false
                currentListNumber = 1
            } else if (currentLine.matches(Regex("\\d+\\.\\s.*"))) {
                // Replace numbered list with bullet
                val numberEnd = currentLine.indexOf('.') + 2
                text.delete(lineStart, lineStart + numberEnd)
                text.insert(lineStart, "• ")
                isBulletList = true
                currentListNumber = 1
            } else {
                // Add bullet
                text.insert(lineStart, "• ")
                isBulletList = true
                currentListNumber = 1
            }
        } else {
            isBulletList = !isBulletList
            isNumberedList = false
            applyListStyle()
        }
        updateButtonStates()
    }

    private fun toggleNumberedList() {
        val text = etNoteContent.text
        val selectionStart = etNoteContent.selectionStart
        val selectionEnd = etNoteContent.selectionEnd

        if (selectionStart == selectionEnd) {
            // Single line conversion
            val lineStart = text.lastIndexOf('\n', selectionStart - 1).let { if (it == -1) 0 else it + 1 }
            val lineEnd = text.indexOf('\n', selectionStart).let { if (it == -1) text.length else it }
            val currentLine = text.substring(lineStart, lineEnd)

            if (currentLine.matches(Regex("\\d+\\.\\s.*"))) {
                // Remove number
                val numberEnd = currentLine.indexOf('.') + 2
                text.delete(lineStart, lineStart + numberEnd)
                isNumberedList = false
                currentListNumber = 1
            } else if (currentLine.startsWith("• ")) {
                // Replace bullet with number
                text.delete(lineStart, lineStart + 2)
                text.insert(lineStart, "$currentListNumber. ")
                isNumberedList = true
                currentListNumber++
            } else {
                // Add number
                text.insert(lineStart, "$currentListNumber. ")
                isNumberedList = true
                currentListNumber++
            }
        } else {
            isNumberedList = !isNumberedList
            isBulletList = false
            if (!isNumberedList) {
                currentListNumber = 1
            }
            applyListStyle()
        }
        updateButtonStates()
    }

    private fun toggleCheckboxList() {
        val text = etNoteContent.text
        val selectionStart = etNoteContent.selectionStart
        val selectionEnd = etNoteContent.selectionEnd

        if (selectionStart == selectionEnd) {
            val lineStart = text.lastIndexOf('\n', selectionStart - 1).let { if (it == -1) 0 else it + 1 }
            val lineEnd = text.indexOf('\n', selectionStart).let { if (it == -1) text.length else it }
            val currentLine = text.substring(lineStart, lineEnd)

            when {
                currentLine.startsWith(CHECKBOX_UNCHECKED) || currentLine.startsWith(CHECKBOX_CHECKED) -> {
                    val checkboxLength = if (currentLine.startsWith(CHECKBOX_UNCHECKED)) 
                        CHECKBOX_UNCHECKED.length else CHECKBOX_CHECKED.length
                    text.delete(lineStart, lineStart + checkboxLength + 1)
                    isCheckboxList = false
                }
                else -> {
                    text.insert(lineStart, "$CHECKBOX_UNCHECKED ")
                    isCheckboxList = true
                }
            }
        } else {
            isCheckboxList = !isCheckboxList
            if (isCheckboxList) {
                val lines = text.substring(selectionStart, selectionEnd).split("\n")
                val newText = StringBuilder()
                lines.forEachIndexed { index, line ->
                    if (index > 0) newText.append("\n")
                    newText.append("$CHECKBOX_UNCHECKED $line")
                }
                text.replace(selectionStart, selectionEnd, newText.toString())
            }
        }
        updateButtonStates()
    }

    private fun toggleCheckboxState(position: Int) {
        val text = etNoteContent.text
        val lineStart = text.lastIndexOf('\n', position - 1).let { if (it == -1) 0 else it + 1 }
        
        if (position >= lineStart) {
            val lineEnd = text.indexOf('\n', position).let { if (it == -1) text.length else it }
            val currentLine = text.substring(lineStart, lineEnd)
            
            when {
                currentLine.startsWith(CHECKBOX_UNCHECKED) -> {
                    text.replace(lineStart, lineStart + CHECKBOX_UNCHECKED.length, CHECKBOX_CHECKED)
                }
                currentLine.startsWith(CHECKBOX_CHECKED) -> {
                    text.replace(lineStart, lineStart + CHECKBOX_CHECKED.length, CHECKBOX_UNCHECKED)
                }
            }
        }
    }

    private fun updateButtonStates() {
        btnBold.isSelected = isBold
        btnItalic.isSelected = isItalic
        btnUnderline.isSelected = isUnderline
        btnStrikethrough.isSelected = isStrikethrough
        btnBulletList.isSelected = isBulletList
        btnNumberedList.isSelected = isNumberedList
        btnCheckbox.isSelected = isCheckboxList
    }

    private fun applyCurrentFormatting(editable: Editable?) {
        editable?.let { text ->
            val selectionStart = etNoteContent.selectionStart
            val selectionEnd = etNoteContent.selectionEnd
            
            if (selectionStart == selectionEnd && selectionStart > 0) {
                // Apply formatting to single character or continue previous formatting
                if (isBold) text.setSpan(StyleSpan(Typeface.BOLD), selectionStart - 1, selectionStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (isItalic) text.setSpan(StyleSpan(Typeface.ITALIC), selectionStart - 1, selectionStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (isUnderline) text.setSpan(UnderlineSpan(), selectionStart - 1, selectionStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (isStrikethrough) text.setSpan(StrikethroughSpan(), selectionStart - 1, selectionStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun handleNewLine(cursorPosition: Int) {
        val text = etNoteContent.text
        val lineStart = text.lastIndexOf('\n', cursorPosition - 2).let { if (it == -1) 0 else it + 1 }
        val currentLine = text.subSequence(lineStart, cursorPosition - 1).toString()
        
        when {
            currentLine.startsWith(CHECKBOX_UNCHECKED) || currentLine.startsWith(CHECKBOX_CHECKED) -> {
                if (currentLine.length <= 2) {
                    // Empty checkbox, remove it
                    text.delete(lineStart, cursorPosition)
                    isCheckboxList = false
                } else {
                    // Continue checkbox list with unchecked box
                    text.insert(cursorPosition, "\n$CHECKBOX_UNCHECKED ")
                }
            }
            currentLine.startsWith("• ") -> {
                if (currentLine.length <= 2) {
                    // Empty bullet point, remove it
                    text.delete(lineStart, cursorPosition)
                    isBulletList = false
                    currentListNumber = 1
                } else {
                    text.insert(cursorPosition, "\n• ")
                }
            }
            currentLine.matches(Regex("\\d+\\.\\s.*")) -> {
                if (currentLine.matches(Regex("\\d+\\.\\s*"))) {
                    // Empty numbered point, remove it
                    text.delete(lineStart, cursorPosition)
                    isNumberedList = false
                    currentListNumber = 1
                } else {
                    currentListNumber = currentLine.substringBefore('.').toIntOrNull()?.plus(1) ?: 1
                    text.insert(cursorPosition, "\n$currentListNumber. ")
                }
            }
            else -> {
                currentListNumber = 1
            }
        }
        updateButtonStates()
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
        
        val selectedText = text.toString().substring(selectionStart, selectionEnd)
        val lines = selectedText.split("\n")
        val styledText = SpannableStringBuilder()
        
        if (isNumberedList) {
            currentListNumber = 1
        }
        
        lines.forEachIndexed { index, line ->
            if (index > 0) styledText.append("\n")
            
            // Remove existing bullet or number if present
            val cleanLine = line.replace(Regex("^(•|\\d+\\.)\\s+"), "")
            
            when {
                isBulletList -> styledText.append("• $cleanLine")
                isNumberedList -> {
                    styledText.append("$currentListNumber. $cleanLine")
                    currentListNumber++
                }
                else -> styledText.append(cleanLine)
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

            // Apply new spans based on current formatting states
            if (isBold) text.setSpan(StyleSpan(Typeface.BOLD), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (isItalic) text.setSpan(StyleSpan(Typeface.ITALIC), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (isUnderline) text.setSpan(UnderlineSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (isStrikethrough) text.setSpan(StrikethroughSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else {
            applyCurrentFormatting(text)
        }
    }
}
