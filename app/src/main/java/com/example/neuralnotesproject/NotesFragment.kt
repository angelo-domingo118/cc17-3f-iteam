package com.example.neuralnotesproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import android.widget.Toast
import java.util.UUID

class NotesFragment : Fragment() {

    private lateinit var noteAdapter: NoteAdapter
    private val notes = mutableListOf<Note>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.rv_notes)
        noteAdapter = NoteAdapter(notes) { note ->
            // Handle note click
        }
        recyclerView.adapter = noteAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        view.findViewById<FloatingActionButton>(R.id.btn_add_note).setOnClickListener {
            showAddNoteDialog()
        }

        // Set up bottom action bar buttons
        view.findViewById<MaterialButton>(R.id.btn_select_all).setOnClickListener {
            noteAdapter.selectAll()
        }

        view.findViewById<MaterialButton>(R.id.btn_unselect_all).setOnClickListener {
            noteAdapter.unselectAll()
        }

        view.findViewById<MaterialButton>(R.id.btn_delete).setOnClickListener {
            deleteSelectedNotes()
        }

        // For demonstration, let's add some sample notes
        addSampleNotes()
    }

    private fun showAddNoteDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_note, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.et_note_title)
        val contentInput = dialogView.findViewById<TextInputEditText>(R.id.et_note_content)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add New Note")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString()
                val content = contentInput.text.toString()
                if (title.isNotEmpty() && content.isNotEmpty()) {
                    addNote(title, content)
                } else {
                    Toast.makeText(context, "Title and content cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addNote(title: String, content: String) {
        val newNote = Note(UUID.randomUUID().toString(), title, content)
        notes.add(newNote)
        noteAdapter.updateNotes(notes)
    }

    private fun addSampleNotes() {
        notes.add(Note(UUID.randomUUID().toString(), "Sample Note 1", "This is the content of sample note 1"))
        notes.add(Note(UUID.randomUUID().toString(), "Sample Note 2", "This is the content of sample note 2"))
        noteAdapter.updateNotes(notes)
    }

    private fun deleteSelectedNotes() {
        val selectedNotes = noteAdapter.getSelectedItems()
        if (selectedNotes.isNotEmpty()) {
            notes.removeAll(selectedNotes)
            noteAdapter.updateNotes(notes)
            noteAdapter.unselectAll()
            Toast.makeText(context, "${selectedNotes.size} note(s) deleted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No notes selected", Toast.LENGTH_SHORT).show()
        }
    }
}