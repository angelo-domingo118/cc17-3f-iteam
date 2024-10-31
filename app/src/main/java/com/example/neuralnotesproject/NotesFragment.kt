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
import androidx.lifecycle.ViewModelProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.neuralnotesproject.data.Note
import com.example.neuralnotesproject.data.AppDatabase
import com.example.neuralnotesproject.repository.NoteRepository
import com.example.neuralnotesproject.viewmodels.NoteViewModel
import com.example.neuralnotesproject.viewmodels.NoteViewModelFactory

class NotesFragment : Fragment() {

    private lateinit var noteAdapter: NoteAdapter
    private lateinit var noteViewModel: NoteViewModel
    private var notebookId: String = ""
    private lateinit var activity: NotebookInteractionActivity

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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as NotebookInteractionActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notebookId = arguments?.getString(ARG_NOTEBOOK_ID) ?: throw IllegalArgumentException("Notebook ID is required")
        
        // Initialize ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        val repository = NoteRepository(database.noteDao())
        noteViewModel = ViewModelProvider(
            this,
            NoteViewModelFactory(repository, notebookId)
        )[NoteViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        setupButtons(view)
        observeNotes()
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView: RecyclerView = view.findViewById(R.id.rv_notes)
        noteAdapter = NoteAdapter(
            notes = emptyList(),  // Specify parameter name
            onNoteClick = { note -> launchEditNoteActivity(note) },  // Specify parameter name
            onSelectionChanged = { selectedNotes -> onSelectedNotesChanged(selectedNotes) }  // Specify parameter name
        )
        recyclerView.apply {
            adapter = noteAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeNotes() {
        noteViewModel.notes.observe(viewLifecycleOwner) { notes ->
            noteAdapter.updateNotes(notes)
        }
    }

    private fun setupButtons(view: View) {
        view.findViewById<MaterialButton>(R.id.btn_add_note).setOnClickListener {
            launchEditNoteActivity()
        }
    }

    private fun onSelectedNotesChanged(selectedNotes: List<Note>) {
        activity.onSelectedNotesChanged(selectedNotes)
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
        val note = Note(
            id = UUID.randomUUID().toString(),
            notebookId = notebookId,
            title = title,
            content = content,
            creationDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())
        )
        noteViewModel.addNote(note)
    }

    private fun updateNote(id: String, title: String, content: String) {
        val currentNote = noteViewModel.notes.value?.find { it.id == id } ?: return
        val updatedNote = currentNote.copy(
            title = title,
            content = content
        )
        noteViewModel.updateNote(updatedNote)
    }

    private fun deleteSelectedNotes() {
        val selectedNotes = noteAdapter.getSelectedItems()
        if (selectedNotes.isNotEmpty()) {
            selectedNotes.forEach { note ->
                noteViewModel.deleteNote(note)
            }
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
