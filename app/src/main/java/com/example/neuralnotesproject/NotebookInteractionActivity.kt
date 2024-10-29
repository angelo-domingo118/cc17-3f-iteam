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
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import android.graphics.Paint
import android.widget.ProgressBar
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.neuralnotesproject.data.Note
import com.example.neuralnotesproject.data.Source
import com.example.neuralnotesproject.data.SourceType
import com.example.neuralnotesproject.util.FileStorageManager
import java.util.UUID

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
    private var selectedFileUri: Uri? = null
    private var selectedWebsiteUrl: String? = null

    private lateinit var btnSource: MaterialButton
    private lateinit var btnNotes: MaterialButton
    private lateinit var btnChat: MaterialButton

    private lateinit var progressBar: ProgressBar
    private var isLoading = false

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
        initializeGeminiApi()

        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE

        sendButton.setOnClickListener {
            val message = inputEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                // Check if we're in a fragment view
                if (findViewById<FrameLayout>(R.id.fragment_container).visibility == View.VISIBLE) {
                    // If in a fragment, switch to chat view before sending the message
                    showChatView()
                }
                sendMessage()
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

        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        btnSource = findViewById(R.id.btn_source)
        btnNotes = findViewById(R.id.btn_notes)
        btnChat = findViewById(R.id.btn_chat)
    }

    private fun setupListeners() {
        btnSource.setOnClickListener {
            showSourcesFragment()
        }

        btnNotes.setOnClickListener {
            showNotesFragment()
        }

        btnChat.setOnClickListener {
            showChatView()
        }
    }

    private fun updateButtonStates(selectedButton: MaterialButton) {
        btnSource.isSelected = selectedButton == btnSource
        btnNotes.isSelected = selectedButton == btnNotes
        btnChat.isSelected = selectedButton == btnChat
    }

    fun onSelectedNotesChanged(notes: List<Note>) {
        selectedNotes = notes
        selectedNotesContent = notes.joinToString("\n\n") { "${it.title}\n${it.content}" }
    }

    fun onSelectedSourcesChanged(sources: List<Source>) {
        selectedSources = sources
        selectedSourcesContent = sources.joinToString("\n\n") { "${it.name}\n${it.content}" }
    }

    fun onFileSelected(uri: Uri) {
        selectedFileUri = uri
    }

    fun onWebsiteUrlSelected(url: String) {
        selectedWebsiteUrl = url
        lifecycleScope.launch {
            try {
                val content = withContext(Dispatchers.IO) {
                    URL(url).readText()
                }
                // Create a source with the fetched content
                val source = Source(
                    id = UUID.randomUUID().toString(),
                    name = url,
                    type = SourceType.WEBSITE,
                    content = content,
                    filePath = null, // Add null filePath for WEBSITE type
                    notebookId = notebookId
                )
                // Add to selected sources
                selectedSources = selectedSources + source
                selectedSourcesContent = buildSelectedSourcesContent()
            } catch (e: Exception) {
                Log.e("NotebookInteraction", "Error fetching website content", e)
                Toast.makeText(this@NotebookInteractionActivity, 
                    "Failed to load website content", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildSelectedSourcesContent(): String {
        return selectedSources.joinToString("\n\n") { source ->
            when (source.type) {
                SourceType.PASTE_TEXT -> "${source.name}\n${source.content}"
                SourceType.WEBSITE -> "Website: ${source.name}\n${source.content}"
                SourceType.FILE -> {
                    try {
                        val fileContent = source.filePath?.let { path ->
                            File(path).readText()
                        } ?: "Unable to read file content"
                        "File: ${source.name}\n$fileContent"
                    } catch (e: Exception) {
                        Log.e("NotebookInteraction", "Error reading file: ${e.message}")
                        "Error reading file ${source.name}: ${e.message}"
                    }
                }
            }
        }
    }

    private fun sendMessage() {
        val message = inputEditText.text.toString().trim()
        if (message.isNotEmpty()) {
            setLoadingState(true)
            
            val userMessage = Message(message, true)
            messageAdapter.addMessage(userMessage)
            // Remove this line to prevent duplication
            // chatContext.add(userMessage)
            inputEditText.text?.clear()

            lifecycleScope.launch {
                try {
                    val prompt = buildPromptWithContext(message)
                    val response = generativeModel.generateContent(prompt)
                    val aiResponse = response.text ?: "Sorry, I couldn't generate a response."
                    val aiMessage = Message(aiResponse, false)
                    messageAdapter.addMessage(aiMessage)
                    // Remove this line to prevent duplication
                    // chatContext.add(aiMessage)
                    recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                    
                    // Clear the selected file and website URL after processing
                    selectedFileUri = null
                    selectedWebsiteUrl = null
                } catch (e: Exception) {
                    showError("Failed to generate AI response: ${e.message}")
                } finally {
                    setLoadingState(false)
                }
            }
        }
    }

    private fun setLoadingState(loading: Boolean) {
        isLoading = loading
        inputEditText.isEnabled = !loading
        sendButton.isEnabled = !loading
        
        if (loading) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (isLoading) {
                    progressBar.visibility = View.VISIBLE
                }
            }, 300) // Show loading indicator after 300ms if still loading
        } else {
            progressBar.visibility = View.GONE
        }
    }

    private suspend fun buildPromptWithContext(userMessage: String): String {
        val sourcesFragment = supportFragmentManager.fragments
            .firstOrNull { it is SourcesFragment } as? SourcesFragment
            
        val selectedSources = sourcesFragment?.getSelectedItems() ?: emptyList()  // Use the new public method
        
        val contextBuilder = StringBuilder()
        
        // Add selected sources content
        selectedSources.forEach { source ->
            when (source.type) {
                SourceType.FILE -> {
                    try {
                        val fileContent = source.filePath?.let { path ->
                            File(path).readText()
                        } ?: source.content
                        contextBuilder.append("Content from file '${source.name}':\n")
                        contextBuilder.append(fileContent)
                        contextBuilder.append("\n\n")
                    } catch (e: Exception) {
                        Log.e("NotebookInteraction", "Error reading file: ${e.message}")
                    }
                }
                SourceType.WEBSITE -> {
                    contextBuilder.append("Content from website '${source.name}':\n")
                    contextBuilder.append(source.content)
                    contextBuilder.append("\n\n")
                }
                SourceType.PASTE_TEXT -> {
                    contextBuilder.append("Content from text '${source.name}':\n")
                    contextBuilder.append(source.content)
                    contextBuilder.append("\n\n")
                }
            }
        }

        return if (contextBuilder.isNotEmpty()) {
            """
            Context:
            ${contextBuilder}
            
            Based on the above context, please respond to this message:
            $userMessage
            """.trimIndent()
        } else {
            userMessage
        }
    }

    private suspend fun fetchWebsiteContent(url: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = URL(url).readText()
                response
            } catch (e: Exception) {
                Log.e("NotebookInteractionActivity", "Error fetching website content", e)
                "Error fetching website content: ${e.message}"
            }
        }
    }

    private fun getFileContent(uri: Uri): String {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: "Unable to read file content"
        } catch (e: Exception) {
            Log.e("NotebookInteractionActivity", "Error reading file content", e)
            "Error reading file content: ${e.message}"
        }
    }

    private fun onSaveButtonClick(message: Message) {
        if (!message.isUser) {
            saveMessageToNotes(message.content)
        }
    }

    private fun saveMessageToNotes(content: String) {
        try {
            // Use the NotesFragment's addNote method instead of file operations
            notesFragment?.addNote(
                title = "Untitled Note",
                content = content
            )
            Toast.makeText(this, "Message saved to notes", Toast.LENGTH_SHORT).show()
            // Remove refreshNotes() call since LiveData will handle updates
        } catch (e: Exception) {
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
            sourcesFragment = SourcesFragment.newInstance(notebookId)
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, sourcesFragment!!)
                .commit()
        } else {
            supportFragmentManager.beginTransaction()
                .show(sourcesFragment!!)
                .commit()
        }
        supportFragmentManager.executePendingTransactions()
        currentFragment = sourcesFragment
        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.GONE
        updateButtonStates(btnSource)
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
        supportFragmentManager.executePendingTransactions()
        // Notes will be automatically updated through LiveData observation
        currentFragment = notesFragment
        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.GONE
        updateButtonStates(btnNotes)
    }

    private fun showChatView() {
        hideCurrentFragment()
        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.VISIBLE
        currentFragment = null
        updateButtonStates(btnChat)
    }

    private fun hideCurrentFragment() {
        currentFragment?.let { fragment ->
            if (fragment.isAdded) { // Add this check
                supportFragmentManager.beginTransaction()
                    .hide(fragment)
                    .commit()
            }
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

    private fun initializeGeminiApi() {
        generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash-002",
            apiKey = "AIzaSyBi_46ImoqYxa69XDTUA2fjSQQjhuFhfuY",
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 1024
            }
        )
    }
}
