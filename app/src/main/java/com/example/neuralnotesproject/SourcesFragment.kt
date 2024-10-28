package com.example.neuralnotesproject

import android.app.Activity  // Add this import
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.material.button.MaterialButton
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.example.neuralnotesproject.data.Source
import com.example.neuralnotesproject.data.SourceType
import java.io.File
import java.util.UUID
import android.provider.OpenableColumns
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import com.example.neuralnotesproject.data.AppDatabase
import com.example.neuralnotesproject.repository.SourceRepository
import com.example.neuralnotesproject.viewmodels.SourceViewModel
import com.example.neuralnotesproject.viewmodels.SourceViewModelFactory

interface SourceActionListener {
    fun onFileSelected(uri: Uri)
    fun onWebsiteUrlSelected(url: String)
}

class SourcesFragment : Fragment(), SourceActionListener {

    private lateinit var sourceAdapter: SourceAdapter
    private var sources = mutableListOf<Source>()
    // Removed: private lateinit var geminiTextExtractor: GeminiTextExtractor
    private lateinit var notebookId: String
    private lateinit var sourceViewModel: SourceViewModel

    private val pasteNotesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pastedText = result.data?.getStringExtra(PasteNotesActivity.EXTRA_PASTED_TEXT)
            pastedText?.let { text ->
                val sourceId = UUID.randomUUID().toString()
                val title = extractTitle(text)
                
                val source = Source(
                    id = sourceId,
                    name = title,
                    type = SourceType.PASTE_TEXT,
                    content = text,
                    notebookId = notebookId
                )
                addSource(source)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notebookId = arguments?.getString("notebookId") ?: throw IllegalStateException("Notebook ID is required")

        // Initialize ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        val repository = SourceRepository(database.sourceDao())
        sourceViewModel = ViewModelProvider(
            this,
            SourceViewModelFactory(repository, notebookId)
        )[SourceViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sources, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up RecyclerView
        val recyclerView: RecyclerView = view.findViewById(R.id.rv_sources)
        sourceAdapter = SourceAdapter(
            emptyList(),  // Start with empty list
            onSourceClick = { /* Handle source click */ },
            onSelectionChanged = { selectedSources ->
                (activity as? NotebookInteractionActivity)?.onSelectedSourcesChanged(selectedSources)
            }
        )
        recyclerView.adapter = sourceAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Observe sources from ViewModel
        sourceViewModel.sources.observe(viewLifecycleOwner) { sources ->
            sourceAdapter.updateSources(sources)
        }

        // Set up buttons
        view.findViewById<MaterialButton>(R.id.btn_add_source).setOnClickListener {
            showAddSourceBottomSheet()
        }

        view.findViewById<MaterialButton>(R.id.btn_select_all).setOnClickListener {
            sourceAdapter.selectAll()
        }

        view.findViewById<MaterialButton>(R.id.btn_unselect_all).setOnClickListener {
            sourceAdapter.unselectAll()
        }

        view.findViewById<MaterialButton>(R.id.btn_delete).setOnClickListener {
            deleteSelectedSources()
        }
    }

    private fun showAddSourceBottomSheet() {
        val addSourceBottomSheet = AddSourceBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putString("notebookId", notebookId)
            }
        }
        addSourceBottomSheet.show(childFragmentManager, AddSourceBottomSheetFragment.TAG)
    }

    internal fun addSource(source: Source) {
        viewLifecycleOwner.lifecycleScope.launch {
            sourceViewModel.addSource(source)
        }
    }

    private fun deleteSelectedSources() {
        val selectedSources = sourceAdapter.getSelectedItems()
        if (selectedSources.isNotEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                selectedSources.forEach { source ->
                    sourceViewModel.deleteSource(source)
                }
                sourceAdapter.unselectAll()
            }
        }
    }

    override fun onFileSelected(uri: Uri) {
        val sourceId = UUID.randomUUID().toString()
        val fileContent = getFileContent(uri)

        val source = Source(
            id = sourceId,
            name = getFileName(uri) ?: "Unnamed File",
            type = SourceType.FILE,
            content = fileContent,
            notebookId = notebookId
        )

        addSource(source)
        (activity as? NotebookInteractionActivity)?.onFileSelected(uri)
    }

    // Helper function to read file content
    private fun getFileContent(uri: Uri): String {
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: "Unable to read file content"
        } catch (e: Exception) {
            Log.e("SourcesFragment", "Error reading file content", e)
            "Error reading file content: ${e.message}"
        }
    }

    override fun onWebsiteUrlSelected(url: String) {
        val sourceId = UUID.randomUUID().toString()
        val source = Source(
            id = sourceId,
            name = url,
            type = SourceType.WEBSITE,
            content = url,
            notebookId = notebookId
        )
        
        viewLifecycleOwner.lifecycleScope.launch {
            sourceViewModel.addSource(source)
        }
        
        (activity as? NotebookInteractionActivity)?.onWebsiteUrlSelected(url)
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex)
        }
    }

    private fun addNewSource(title: String, content: String, type: SourceType) {
        val newSource = Source(
            id = UUID.randomUUID().toString(),
            name = title,
            type = type,
            content = content,
            notebookId = notebookId
        )
        addSource(newSource)
    }

    private fun addNoteToNotesFragment(title: String, content: String) {
        val notesFragment = parentFragmentManager.findFragmentByTag("NotesFragment") as? NotesFragment
        notesFragment?.addNote(title, content)
    }

    fun getSelectedSourcesContent(): String {
        return sourceAdapter.getSelectedItems().joinToString("\n\n") { "${it.name}\n${it.content}" }
    }

    private fun saveSourceToStorage(notebookId: String, source: Source) {
        val sourceDir = File(requireContext().filesDir, "$notebookId/sources")
        sourceDir.mkdirs()

        val file = File(sourceDir, "${source.id}.txt")
        file.writeText("${source.name}\n${source.type}\n${source.content}")
    }

    private fun deleteSourcesFromStorage(notebookId: String, sourcesToDelete: List<Source>) {
        val sourceDir = File(requireContext().filesDir, "$notebookId/sources")
        sourcesToDelete.forEach { source ->
            val file = File(sourceDir, "${source.id}.txt")
            file.delete()
        }
    }

    fun showPasteNotes() {
        val intent = Intent(requireContext(), PasteNotesActivity::class.java)
        pasteNotesLauncher.launch(intent)
    }

    private fun extractTitle(text: String, maxLength: Int = 30): String {
        val firstLine = text.lineSequence().firstOrNull()?.trim() ?: ""
        return if (firstLine.length > maxLength) {
            firstLine.substring(0, maxLength) + "..."
        } else if (firstLine.isNotEmpty()) {
            firstLine
        } else {
            if (text.length > maxLength) {
                text.substring(0, maxLength) + "..."
            } else {
                text
            }
        }
    }

    companion object {
        fun newInstance(notebookId: String): SourcesFragment {
            return SourcesFragment().apply {
                arguments = Bundle().apply {
                    putString("notebookId", notebookId)
                }
            }
        }
    }
}
