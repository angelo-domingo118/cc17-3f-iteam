package com.example.neuralnotesproject

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.ImageView
import androidx.lifecycle.ViewModelProvider
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.ai.client.generativeai.GenerativeModel
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.type.generationConfig
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import kotlin.random.Random
import java.io.File
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import android.view.Gravity
import android.view.ViewGroup
import android.content.Intent
import android.util.Log
import androidx.fragment.app.Fragment
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotebookInteractionActivity : AppCompatActivity() {
    private lateinit var notebookViewModel: NotebookViewModel
    private lateinit var notebookId: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var inputEditText: TextInputEditText
    private lateinit var sendButton: FloatingActionButton
    private lateinit var generativeModel: GenerativeModel

    private var currentFragment: Fragment? = null
    private var notesFragment: NotesFragment? = null
    private var sourcesFragment: SourcesFragment? = null

    private var selectedNotes: List<Note> = emptyList()
    private var selectedSources: List<Source> = emptyList()

    private var selectedNotesContent: String = ""
    private var selectedSourcesContent: String = ""

    private val chatContext = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notebook_interaction)

        // Retrieve the notebook title from the intent
        val notebookTitle = intent.getStringExtra("NOTEBOOK_TITLE") ?: "Notebook"

        // Find the TextView and set its text to the notebook title
        val titleTextView: TextView = findViewById(R.id.tv_title)
        titleTextView.text = notebookTitle

        notebookViewModel = ViewModelProvider(this)[NotebookViewModel::class.java]

        notebookId = intent.getStringExtra("NOTEBOOK_ID") ?: return

        notebookViewModel.getNotebook(notebookId).observe(this) { notebook ->
            notebook?.let {
                // You can use other notebook details as needed
            }
        }

        recyclerView = findViewById(R.id.recyclerView)
        inputEditText = findViewById(R.id.et_user_input)
        sendButton = findViewById(R.id.btn_send)

        messageAdapter = MessageAdapter(chatContext, ::onSaveButtonClick)
        recyclerView.adapter = messageAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize Gemini API with Flash model
        generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "AIzaSyBi_46ImoqYxa69XDTUA2fjSQQjhuFhfuY",
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 1024
            }
        )

        sendButton.setOnClickListener {
            val message = inputEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                // Check if we're in a fragment view
                if (findViewById<FrameLayout>(R.id.fragment_container).visibility == View.VISIBLE) {
                    // If in a fragment, switch to chat view before sending the message
                    showChatView()
                }
                sendMessage(message)
                inputEditText.text?.clear()
            }
        }

        // Implement back button functionality
        val backButton: ImageView = findViewById(R.id.iv_back)
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Implement more options button functionality
        val moreOptionsButton: ImageView = findViewById(R.id.iv_more_options)
        moreOptionsButton.setOnClickListener { view ->
            showPopupWindow(view)
        }

        // Find the Source button and set its click listener
        findViewById<MaterialButton>(R.id.btn_source).setOnClickListener {
            showSourcesFragment()
        }

        // Find the Notes button and set its click listener
        findViewById<MaterialButton>(R.id.btn_notes).setOnClickListener {
            showNotesFragment()
        }

        // Find the Chat button and set its click listener
        findViewById<MaterialButton>(R.id.btn_chat).setOnClickListener {
            showChatView()
        }
    }

    fun onSelectedNotesChanged(notes: List<Note>) {
        selectedNotes = notes
        selectedNotesContent = notes.joinToString("\n\n") { "${it.title}\n${it.content}" }
    }

    fun onSelectedSourcesChanged(sources: List<Source>) {
        selectedSources = sources
        selectedSourcesContent = sources.joinToString("\n\n") { "${it.name}\n${it.content}" }
    }

    private fun sendMessage(message: String) {
        if (message.isEmpty()) {
            showError("Message cannot be empty")
            return
        }

        val userMessage = Message(message, true)
        chatContext.add(userMessage)
        messageAdapter.notifyItemInserted(chatContext.size - 1)
        recyclerView.scrollToPosition(chatContext.size - 1)

        lifecycleScope.launch {
            try {
                val prompt = buildPromptWithContext(message)
                val response = generativeModel.generateContent(prompt)
                val aiResponse = response.text ?: "Sorry, I couldn't generate a response."
                val aiMessage = Message(aiResponse, false)
                chatContext.add(aiMessage)
                messageAdapter.notifyItemInserted(chatContext.size - 1)
                recyclerView.scrollToPosition(chatContext.size - 1)
            } catch (e: Exception) {
                showError("Failed to generate AI response: ${e.message}")
            }
        }
    }

    private fun buildPromptWithContext(userMessage: String): String {
        val contextPrompt = when {
            selectedNotesContent.isNotEmpty() && selectedSourcesContent.isNotEmpty() -> {
                "The following are selected notes and sources from the user's notebook:\n\n" +
                "Notes:\n$selectedNotesContent\n\n" +
                "Sources:\n$selectedSourcesContent\n\n" +
                "Please consider this information as context for your response. " +
                "If relevant, refer to or incorporate details from these notes and sources in your answer."
            }
            selectedNotesContent.isNotEmpty() -> {
                "The following are selected notes from the user's notebook:\n\n$selectedNotesContent\n\n" +
                "Please consider this information as context for your response. " +
                "If relevant, refer to or incorporate details from these notes in your answer."
            }
            selectedSourcesContent.isNotEmpty() -> {
                "The following are selected sources from the user's notebook:\n\n$selectedSourcesContent\n\n" +
                "Please consider this information as context for your response. " +
                "If relevant, refer to or incorporate details from these sources in your answer."
            }
            else -> {
                "You are an AI assistant in a note-taking app. " +
                "The user can create notebooks, add notes, and import sources. " +
                "They can also chat with you for assistance or insights related to their notes and sources."
            }
        }

        val chatHistory = chatContext.takeLast(10).joinToString("\n") { 
            if (it.isUser) "User: ${it.content}" else "AI: ${it.content}"
        }

        return "$contextPrompt\n\nChat history:\n$chatHistory\n\nUser: $userMessage\n\nAI:"
    }

    private fun onSaveButtonClick(message: Message) {
        if (!message.isUser) {
            saveMessageToNotes(message.content)
        }
    }

    private fun saveMessageToNotes(content: String) {
        val notesFolder = File(filesDir, "$notebookId/notes")
        if (!notesFolder.exists()) {
            notesFolder.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "note_$timestamp.txt"
        val noteFile = File(notesFolder, fileName)

        try {
            noteFile.writeText("Untitled Note\n$content")
            Toast.makeText(this, "Message saved to notes", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Log.e("NotebookInteractionActivity", "Error saving message to notes", e)
            Toast.makeText(this, "Failed to save message", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun getRandomResponse(): String {
        val responses = listOf(
            "Interesting thought!",
            "Tell me more about that.",
            "I see what you mean.",
            "That's a great point!",
            "I hadn't considered that before."
        )
        return responses[Random.nextInt(responses.size)]
    }

    private fun showPopupWindow(anchorView: View) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_notebook_options, null)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        val deleteOption = popupView.findViewById<LinearLayout>(R.id.delete_option)
        val renameOption = popupView.findViewById<LinearLayout>(R.id.rename_option)

        deleteOption.setOnClickListener {
            deleteNotebook()
            popupWindow.dismiss()
        }

        renameOption.setOnClickListener {
            showRenameDialog()
            popupWindow.dismiss()
        }

        popupWindow.elevation = 10f
        popupWindow.showAsDropDown(anchorView, 0, 0, Gravity.END)
    }

    private fun deleteNotebook() {
        AlertDialog.Builder(this)
            .setTitle("Delete Notebook")
            .setMessage("Are you sure you want to delete this notebook?")
            .setPositiveButton("Delete") { _, _ ->
                notebookViewModel.deleteNotebook(notebookId)
                // Delete from storage
                val folder = File(filesDir, notebookId)
                if (folder.exists() && folder.isDirectory) {
                    folder.deleteRecursively()
                }
                Toast.makeText(this, "Notebook deleted", Toast.LENGTH_SHORT).show()
                
                // Set result to indicate deletion
                setResult(RESULT_OK, Intent().putExtra("DELETED_NOTEBOOK_ID", notebookId))
                finish() // Close the activity after deleting
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameDialog() {
        val input = EditText(this)
        input.setText(notebookViewModel.getNotebook(notebookId).value?.title)
        
        AlertDialog.Builder(this)
            .setTitle("Rename Notebook")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newTitle = input.text.toString()
                if (newTitle.isNotEmpty()) {
                    renameNotebook(newTitle)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renameNotebook(newTitle: String) {
        notebookViewModel.renameNotebook(notebookId, newTitle)
        findViewById<TextView>(R.id.tv_title).text = newTitle
        Toast.makeText(this, "Notebook renamed", Toast.LENGTH_SHORT).show()

        // Update the notebook in storage
        updateNotebookInStorage(notebookId, newTitle)

        // Set result to indicate renaming
        setResult(RESULT_OK, Intent().putExtra("RENAMED_NOTEBOOK_ID", notebookId)
                                     .putExtra("NEW_TITLE", newTitle))
    }

    private fun updateNotebookInStorage(notebookId: String, newTitle: String) {
        val folder = File(filesDir, notebookId)
        val file = File(folder, "notebook_details.txt")
        if (file.exists()) {
            try {
                val lines = file.readLines()
                if (lines.size >= 3) {
                    val updatedLines = listOf(lines[0], newTitle, lines[2])
                    file.writeText(updatedLines.joinToString("\n"))
                }
            } catch (e: IOException) {
                Log.e("NotebookInteractionActivity", "Error updating notebook in storage", e)
            }
        }
    }

    private fun showSourcesFragment() {
        hideCurrentFragment()
        if (sourcesFragment == null) {
            sourcesFragment = SourcesFragment().apply {
                arguments = Bundle().apply {
                    putString("notebookId", notebookId)
                }
            }
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, sourcesFragment!!)
                .commit()
        } else {
            supportFragmentManager.beginTransaction()
                .show(sourcesFragment!!)
                .commit()
        }
        currentFragment = sourcesFragment
        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.GONE
    }

    private fun showNotesFragment() {
        hideCurrentFragment()
        if (notesFragment == null) {
            notesFragment = NotesFragment.newInstance(notebookId)
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, notesFragment!!)
                .commit()
        } else {
            supportFragmentManager.beginTransaction()
                .show(notesFragment!!)
                .commit()
        }
        currentFragment = notesFragment
        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.GONE
    }

    private fun showChatView() {
        hideCurrentFragment()
        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.VISIBLE
        currentFragment = null
    }

    private fun hideCurrentFragment() {
        currentFragment?.let { fragment ->
            supportFragmentManager.beginTransaction()
                .hide(fragment)
                .commit()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear chat context when leaving the notebook
        chatContext.clear()
        // Clear fragment instances to reset state when activity is destroyed
        notesFragment = null
        sourcesFragment = null
    }
}