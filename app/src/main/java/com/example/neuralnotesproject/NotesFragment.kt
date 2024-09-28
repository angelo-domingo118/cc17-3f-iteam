package com.example.neuralnotesproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.button.MaterialButton
import android.widget.Toast
import java.util.UUID
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.content.Context

class NotesFragment : Fragment() {

    private lateinit var noteAdapter: NoteAdapter
    private var notes = mutableListOf<Note>()
    private lateinit var notebookId: String

    private val editNoteActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.let { data ->
                val id = data.getStringExtra(EXTRA_NOTE_ID)
                val title = data.getStringExtra(EXTRA_NOTE_TITLE) ?: ""
                val content = data.getStringExtra(EXTRA_NOTE_CONTENT) ?: ""
                if (title.isNotEmpty() && content.isNotEmpty()) {
                    if (id != null) {
                        updateNote(id, title, content)
                    } else {
                        addNote(title, content)
                    }
                }
            }
        }
    }

    private val selectedNotes = mutableSetOf<Note>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notebookId = arguments?.getString(ARG_NOTEBOOK_ID) ?: throw IllegalArgumentException("Notebook ID is required")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load notes from local storage
        notes = FileUtils.loadNotes(requireContext(), notebookId).toMutableList()

        val recyclerView: RecyclerView = view.findViewById(R.id.rv_notes)
        noteAdapter = NoteAdapter(
            notes,
            { note -> launchEditNoteActivity(note) },
            { selectedNotes -> onSelectedNotesChanged(selectedNotes) }
        )
        recyclerView.adapter = noteAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        view.findViewById<FloatingActionButton>(R.id.btn_add_note).setOnClickListener {
            launchEditNoteActivity()
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
    }

    private fun onSelectedNotesChanged(selectedNotes: List<Note>) {
        (activity as? NotebookInteractionActivity)?.onSelectedNotesChanged(selectedNotes)
    }

    // Remove this method as it's no longer needed
    // fun getSelectedNotesContent(): String {
    //     return selectedNotes.joinToString("\n\n") { "${it.title}\n${it.content}" }
    // }

    private fun launchEditNoteActivity(note: Note? = null) {
        val intent = Intent(requireContext(), EditNoteActivity::class.java).apply {
            note?.let {
                putExtra(EXTRA_NOTE_ID, it.id)
                putExtra(EXTRA_NOTE_TITLE, it.title)
                putExtra(EXTRA_NOTE_CONTENT, it.content)
            }
        }
        editNoteActivityLauncher.launch(intent)
    }

    fun addNote(title: String, content: String) {
        val newNote = Note(UUID.randomUUID().toString(), title, content)
        notes.add(newNote)
        FileUtils.saveNote(requireContext(), notebookId, newNote)
        noteAdapter.updateNotes(notes)
    }

    private fun updateNote(id: String, title: String, content: String) {
        val noteIndex = notes.indexOfFirst { it.id == id }
        if (noteIndex != -1) {
            val updatedNote = Note(id, title, content)
            notes[noteIndex] = updatedNote
            FileUtils.saveNote(requireContext(), notebookId, updatedNote)
            noteAdapter.updateNotes(notes)
        }
    }

    private fun deleteSelectedNotes() {
        val selectedNotes = noteAdapter.getSelectedItems()
        if (selectedNotes.isNotEmpty()) {
            notes.removeAll(selectedNotes)
            selectedNotes.forEach { note ->
                FileUtils.deleteNote(requireContext(), notebookId, note.id)
            }
            noteAdapter.updateNotes(notes)
            noteAdapter.unselectAll()
            Toast.makeText(context, "${selectedNotes.size} note(s) deleted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No notes selected", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_NOTE_TITLE = "extra_note_title"
        const val EXTRA_NOTE_CONTENT = "extra_note_content"
        private const val ARG_NOTEBOOK_ID = "arg_notebook_id"

        fun newInstance(notebookId: String): NotesFragment {
            return NotesFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NOTEBOOK_ID, notebookId)
                }
            }
        }
    }
}