package com.example.neuralnotesproject

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
import com.example.neuralnotesproject.Source
import com.example.neuralnotesproject.SourceType
import java.io.File
import java.util.UUID
import android.provider.OpenableColumns
import com.google.android.material.floatingactionbutton.FloatingActionButton

interface SourceActionListener {
    fun onFileSelected(uri: Uri)
    fun onWebsiteUrlSelected(url: String)
}

class SourcesFragment : Fragment(), SourceActionListener {

    private lateinit var sourceAdapter: SourceAdapter
    private var sources = mutableListOf<Source>()
    // Removed: private lateinit var geminiTextExtractor: GeminiTextExtractor
    private lateinit var notebookId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Removed: geminiTextExtractor = GeminiTextExtractor(requireContext())
        notebookId = arguments?.getString("notebookId") ?: throw IllegalStateException("Notebook ID is required")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sources, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load sources from local storage
        sources = loadSourcesFromStorage(notebookId).toMutableList()

        val recyclerView: RecyclerView = view.findViewById(R.id.rv_sources)
        sourceAdapter = SourceAdapter(
            sources,
            onSourceClick = { /* Handle source click */ },
            onSelectionChanged = { selectedSources ->
                (activity as? NotebookInteractionActivity)?.onSelectedSourcesChanged(selectedSources)
            }
        )
        recyclerView.adapter = sourceAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        view.findViewById<FloatingActionButton>(R.id.btn_add_source).setOnClickListener {
            showAddSourceBottomSheet()
        }

        // Set up bottom action bar buttons
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

    fun addSource(source: Source) {
        sources.add(source)
        sourceAdapter.updateSources(sources)
        saveSourceToStorage(notebookId, source)
    }

    private fun deleteSelectedSources() {
        val selectedSources = sourceAdapter.getSelectedItems()
        if (selectedSources.isNotEmpty()) {
            sources.removeAll(selectedSources)
            sourceAdapter.updateSources(sources)
            sourceAdapter.unselectAll()
            deleteSourcesFromStorage(notebookId, selectedSources)
            Toast.makeText(context, "${selectedSources.size} source(s) deleted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No sources selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onFileSelected(uri: Uri) {
        val notebookId = arguments?.getString("notebookId") ?: return
        val sourceDir = File(requireContext().filesDir, "$notebookId/sources")
        sourceDir.mkdirs()

        val sourceId = UUID.randomUUID().toString()
        val source = Source(
            id = sourceId,
            name = uri.toString(),
            type = SourceType.FILE,
            content = uri.toString()  // Store the URI as a string
        )

        addSource(source)
        (activity as? NotebookInteractionActivity)?.onFileSelected(uri)
    }

    override fun onWebsiteUrlSelected(url: String) {
        val sourceId = UUID.randomUUID().toString()
        val source = Source(
            id = sourceId,
            name = url,
            type = SourceType.WEBSITE,
            content = url
        )
        addSource(source)
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
            content = content
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

    private fun loadSourcesFromStorage(notebookId: String): List<Source> {
        val sourceDir = File(requireContext().filesDir, "$notebookId/sources")
        val sources = mutableListOf<Source>()

        if (sourceDir.exists() && sourceDir.isDirectory) {
            sourceDir.listFiles()?.forEach { file ->
                if (file.isFile && file.extension == "txt") {
                    val lines = file.readLines()
                    if (lines.isNotEmpty()) {
                        val id = file.nameWithoutExtension
                        val name = lines[0]
                        val content = if (lines.size > 1) lines.subList(1, lines.size).joinToString("\n") else ""
                        val type = SourceType.FILE // Assuming all stored sources are files for now
                        sources.add(Source(id, name, type, content))
                    }
                }
            }
        }

        return sources
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
        val pasteNotesFragment = PasteNotesFragment().apply {
            arguments = Bundle().apply {
                putString("notebookId", this@SourcesFragment.notebookId)
            }
        }
        pasteNotesFragment.show(parentFragmentManager, "PasteNotesFragment")
    }
}