package your.`package`.name

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.getSpans
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.app.Activity
import android.content.Intent
import com.example.neuralnotesproject.NotesFragment
import com.example.neuralnotesproject.R

class EditNoteActivity : AppCompatActivity() {

    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteContent: EditText
    private lateinit var btnBold: ImageButton
    private lateinit var btnItalic: ImageButton
    private lateinit var btnUnderline: ImageButton
    private lateinit var btnStrikethrough: ImageButton
    private lateinit var btnBulletList: ImageButton
    private lateinit var btnNumberedList: ImageButton
    private lateinit var fabSave: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screen_edit_note)

        initViews()
        setupListeners()
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
        fabSave = findViewById(R.id.fab_save)
    }

    private fun setupListeners() {
        btnBold.setOnClickListener { applyStyle(Typeface.BOLD) }
        btnItalic.setOnClickListener { applyStyle(Typeface.ITALIC) }
        btnUnderline.setOnClickListener { applyUnderline() }
        btnStrikethrough.setOnClickListener { applyStrikethrough() }
        btnBulletList.setOnClickListener { applyBulletList() }
        btnNumberedList.setOnClickListener { applyNumberedList() }
        fabSave.setOnClickListener { saveNote() }
    }

    private fun applyStyle(style: Int) {
        val spannable = etNoteContent.text as Spannable
        val selectionStart = etNoteContent.selectionStart
        val selectionEnd = etNoteContent.selectionEnd

        if (selectionStart == selectionEnd) {
            // No text selected, apply to future typing
            etNoteContent.typeface = Typeface.create(etNoteContent.typeface, style)
        } else {
            val spans = spannable.getSpans<StyleSpan>(selectionStart, selectionEnd)
            val existingSpan = spans.find { it.style == style }

            if (existingSpan != null) {
                spannable.removeSpan(existingSpan)
            } else {
                spannable.setSpan(StyleSpan(style), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun applyUnderline() {
        val spannable = etNoteContent.text as Spannable
        val selectionStart = etNoteContent.selectionStart
        val selectionEnd = etNoteContent.selectionEnd

        if (selectionStart != selectionEnd) {
            val spans = spannable.getSpans<UnderlineSpan>(selectionStart, selectionEnd)
            if (spans.isNotEmpty()) {
                spans.forEach { spannable.removeSpan(it) }
            } else {
                spannable.setSpan(UnderlineSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun applyStrikethrough() {
        val spannable = etNoteContent.text as Spannable
        val selectionStart = etNoteContent.selectionStart
        val selectionEnd = etNoteContent.selectionEnd

        if (selectionStart != selectionEnd) {
            val spans = spannable.getSpans<StrikethroughSpan>(selectionStart, selectionEnd)
            if (spans.isNotEmpty()) {
                spans.forEach { spannable.removeSpan(it) }
            } else {
                spannable.setSpan(StrikethroughSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun applyBulletList() {
        // Implement bullet list logic here
        // This might involve adding bullet characters at the start of each line
        // or using a custom span for more advanced formatting
    }

    private fun applyNumberedList() {
        // Implement numbered list logic here
        // This might involve adding numbers at the start of each line
        // or using a custom span for more advanced formatting
    }

    private fun saveNote() {
        val title = etNoteTitle.text.toString().trim()
        val content = etNoteContent.text.toString().trim()

        if (title.isNotEmpty() && content.isNotEmpty()) {
            val resultIntent = Intent().apply {
                putExtra(NotesFragment.EXTRA_NOTE_TITLE, title)
                putExtra(NotesFragment.EXTRA_NOTE_CONTENT, content)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } else {
            // Show error message if title or content is empty
            // You can use a Toast or Snackbar here
        }
    }
}