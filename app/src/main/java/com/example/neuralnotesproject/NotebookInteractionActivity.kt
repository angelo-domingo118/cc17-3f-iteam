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
import com.google.android.material.button.MaterialButton
import com.google.ai.client.generativeai.GenerativeModel
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.type.generationConfig
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
import com.google.android.material.tabs.TabLayout
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.example.neuralnotesproject.data.Message
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.example.neuralnotesproject.data.AppDatabase
import com.example.neuralnotesproject.repository.NotebookRepository
import com.example.neuralnotesproject.viewmodels.NotebookViewModelFactory
import com.example.neuralnotesproject.adapters.MessageAdapter
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.neuralnotesproject.ui.TypingIndicator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.neuralnotesproject.viewmodels.NoteViewModel
import com.example.neuralnotesproject.viewmodels.NoteViewModelFactory
import com.example.neuralnotesproject.repository.NoteRepository
import com.example.neuralnotesproject.util.AIConstants

class NotebookInteractionActivity : AppCompatActivity() {
    private lateinit var notebookViewModel: com.example.neuralnotesproject.viewmodels.NotebookViewModel
    private var notebookId: String = ""
    private var userId: String = ""
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var inputEditText: TextInputEditText
    private lateinit var sendButton: MaterialButton
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

    private lateinit var navigationTabs: TabLayout

    private lateinit var typingIndicator: TypingIndicator

    private lateinit var noteViewModel: NoteViewModel

    private var isTypingResponse = false

    private fun setupMessageHandling() {
        messageAdapter = MessageAdapter(
            chatContext,
            onSaveClick = { message -> handleMessageSave(message) },
            onCopyClick = { message -> handleMessageCopy(message) }
        )
        
        recyclerView.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(this@NotebookInteractionActivity).apply {
                stackFromEnd = true
            }
        }

        sendButton.setOnClickListener {
            val userMessage = inputEditText.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                sendMessage(userMessage)
                inputEditText.text?.clear()
            }
        }
    }

    private fun handleMessageCopy(message: Message) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AI Response", message.content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun handleMessageSave(message: Message) {
        val note = Note(
            id = UUID.randomUUID().toString(),
            title = "AI Response - ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())}",
            content = message.content,
            notebookId = notebookId,
            creationDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
        
        noteViewModel.addNote(note)
        Toast.makeText(this, "Saved to notes", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notebook_interaction)

        notebookId = intent.getStringExtra("NOTEBOOK_ID") ?: return
        userId = intent.getStringExtra("USER_ID") ?: return

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView)
        inputEditText = findViewById(R.id.et_user_input)
        sendButton = findViewById(R.id.btn_send)
        navigationTabs = findViewById(R.id.tab_layout)

        typingIndicator = findViewById(R.id.typing_indicator)

        // Initialize ViewModel
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = NotebookRepository(database.notebookDao())
        notebookViewModel = ViewModelProvider(
            this,
            NotebookViewModelFactory(repository, userId)
        )[com.example.neuralnotesproject.viewmodels.NotebookViewModel::class.java]

        // Initialize NoteViewModel
        val noteRepository = NoteRepository(database.noteDao())
        noteViewModel = ViewModelProvider(
            this,
            NoteViewModelFactory(noteRepository, notebookId)
        )[NoteViewModel::class.java]

        // Initialize Gemini API
        initializeGeminiApi()

        // Setup message handling
        setupMessageHandling()

        // Setup navigation
        setupNavigation()

        // Setup click listeners
        setupClickListeners()

        // Select chat tab immediately and show welcome message
        navigationTabs.getTabAt(2)?.select()
        showChatView()
        showWelcomeMessage()

        // Observe notebook changes
        notebookViewModel.getNotebook(notebookId).observe(this) { notebook ->
            notebook?.let {
                findViewById<TextView>(R.id.tv_title).text = it.title
            }
        }

        // Initially show Sources fragment
        showFragment(SourcesFragment.newInstance(notebookId))
    }

    private fun setupNavigation() {
        navigationTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // Sources
                        showSourcesFragment()
                        typingIndicator.stopAnimation()
                    }
                    1 -> { // Notes
                        showNotesFragment()
                        typingIndicator.stopAnimation()
                    }
                    2 -> { // Chat
                        showChatView()
                        // Restore typing indicator state if we were in the middle of a response
                        if (isTypingResponse) {
                            typingIndicator.startAnimation()
                        }
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showFragment(fragment: Fragment) {
        currentFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        findViewById<ImageView>(R.id.iv_more_options).setOnClickListener {
            showPopupMenu(it)
        }
    }

    private fun initializeViews() {
        navigationTabs = findViewById(R.id.tab_layout)
    }

    private fun setupTabLayout() {
        navigationTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showSourcesFragment()
                    1 -> showNotesFragment()
                    2 -> showChatView()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateTabStates(selectedPosition: Int) {
        navigationTabs.getTabAt(selectedPosition)?.select()
    }

    private fun showSourcesFragment() {
        if (sourcesFragment == null) {
            sourcesFragment = SourcesFragment.newInstance(notebookId)
        }
        
        val transaction = supportFragmentManager.beginTransaction()
        
        // Hide current fragment
        currentFragment?.let { transaction.hide(it) }
        
        // Add or show sources fragment
        if (!sourcesFragment!!.isAdded) {
            transaction.add(R.id.fragment_container, sourcesFragment!!)
        } else {
            transaction.show(sourcesFragment!!)
        }
        
        transaction.commit()
        
        currentFragment = sourcesFragment
        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.GONE
        findViewById<ConstraintLayout>(R.id.bottom_user_input_navigation).visibility = View.GONE
    }

    private fun showNotesFragment() {
        if (notesFragment == null) {
            notesFragment = NotesFragment.newInstance(notebookId)
        }
        
        val transaction = supportFragmentManager.beginTransaction()
        
        // Hide current fragment
        currentFragment?.let { transaction.hide(it) }
        
        // Add or show notes fragment
        if (!notesFragment!!.isAdded) {
            transaction.add(R.id.fragment_container, notesFragment!!)
        } else {
            transaction.show(notesFragment!!)
        }
        
        transaction.commit()
        
        currentFragment = notesFragment
        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.GONE
        findViewById<ConstraintLayout>(R.id.bottom_user_input_navigation).visibility = View.GONE
    }

    private fun showChatView() {
        val transaction = supportFragmentManager.beginTransaction()
        currentFragment?.let { transaction.hide(it) }
        transaction.commit()
        
        currentFragment = null
        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.VISIBLE
        findViewById<ConstraintLayout>(R.id.bottom_user_input_navigation).visibility = View.VISIBLE
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
        val config = generationConfig {
            temperature = 0.7f  // Balanced between creativity and consistency
            topK = 40
            topP = 0.8f
            maxOutputTokens = 1024
        }

        generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash-002",  // Updated to use flash model
            apiKey = "AIzaSyDCIbJjWPmeG5FULSuaSQIp7UuWT6yzRqo",
            generationConfig = config
        )
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        messageAdapter = com.example.neuralnotesproject.adapters.MessageAdapter(
            messages = chatContext,
            onSaveClick = { message -> handleMessageSave(message) },
            onCopyClick = { message -> handleMessageCopy(message) }
        )
        recyclerView.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(this@NotebookInteractionActivity)
        }
    }

    private fun sendMessage(userMessage: String) {
        // Add user message to context
        val message = Message(content = userMessage, isUser = true, notebookId = notebookId)
        chatContext.add(message)
        messageAdapter.notifyItemInserted(chatContext.size - 1)
        
        // Show typing indicator and set state
        isTypingResponse = true
        typingIndicator.startAnimation()
        
        lifecycleScope.launch {
            try {
                val response = generateAiResponse(userMessage)
                
                // Hide typing indicator and reset state
                isTypingResponse = false
                typingIndicator.stopAnimation()
                
                // Add AI response to context
                val aiMessage = Message(content = response, isUser = false, notebookId = notebookId)
                chatContext.add(aiMessage)
                messageAdapter.notifyItemInserted(chatContext.size - 1)
                recyclerView.scrollToPosition(chatContext.size - 1)
            } catch (e: Exception) {
                // Handle error and reset state
                isTypingResponse = false
                typingIndicator.stopAnimation()
                Toast.makeText(this@NotebookInteractionActivity, 
                    "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun generateAiResponse(userMessage: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // Build context from selected notes with more detail
                val contextBuilder = StringBuilder()
                
                if (selectedNotes.isNotEmpty()) {
                    contextBuilder.append("Selected notes (${selectedNotes.size}):\n")
                    selectedNotes.forEachIndexed { index, note ->
                        contextBuilder.append("Note ${index + 1}: '${note.title}'\n")
                        contextBuilder.append("Content: ${note.content}\n")
                        contextBuilder.append("Created: ${note.creationDate}\n\n")
                    }
                }
                
                if (selectedSources.isNotEmpty()) {
                    contextBuilder.append("Selected sources (${selectedSources.size}):\n")
                    selectedSources.forEachIndexed { index, source ->
                        contextBuilder.append("Source ${index + 1}: '${source.name}'\n")
                        contextBuilder.append("Type: ${source.type.name}\n")
                        contextBuilder.append("Content: ${source.content}\n\n")
                    }
                }
                
                // Add previous messages to maintain context
                if (chatContext.isNotEmpty()) {
                    contextBuilder.append("Previous conversation:\n")
                    chatContext.takeLast(4).forEach { message ->
                        contextBuilder.append(if (message.isUser) "User: " else "Assistant: ")
                        contextBuilder.append(message.content)
                        contextBuilder.append("\n")
                    }
                    contextBuilder.append("\n")
                }

                // Build the complete prompt with system instructions
                val prompt = """
                    ${AIConstants.SYSTEM_PROMPT}
                    
                    Current context:
                    ${contextBuilder}
                    
                    User question: $userMessage
                    
                    Assistant:
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                response.text ?: "Sorry, I couldn't generate a response."
            } catch (e: Exception) {
                "Sorry, there was an error generating the response: ${e.message}"
            }
        }
    }

    private fun showPopupWindow(anchorView: View) {
        val popupView = LayoutInflater.from(this)
            .inflate(R.layout.popup_menu, null)
        
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Update the popup menu item finding
        popupView.findViewById<TextView>(R.id.save_button)?.setOnClickListener {
            onSaveButtonClick()
            popupWindow.dismiss()
        }
        
        popupWindow.showAsDropDown(anchorView)
    }

    private fun onSaveButtonClick() {
        // Implement save functionality
        Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show()
        // Add your save logic here
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
    }

    private fun showPopupMenu(anchorView: View) {
        val popupView = LayoutInflater.from(this).inflate(R.layout.popup_notebook_options, null)
        
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 10f
            setBackgroundDrawable(null)
            isOutsideTouchable = true
        }

        // Set up click listeners for popup menu items
        popupView.findViewById<LinearLayout>(R.id.rename_option).setOnClickListener {
            // Handle rename action
            popupWindow.dismiss()
            showRenameDialog()
        }

        popupView.findViewById<LinearLayout>(R.id.delete_option).setOnClickListener {
            // Handle delete action
            popupWindow.dismiss()
            showDeleteDialog()
        }

        // Calculate position for popup
        val location = IntArray(2)
        anchorView.getLocationInWindow(location)
        
        // Show popup below the anchor view with proper alignment
        popupWindow.showAsDropDown(
            anchorView,
            -popupWindow.width + anchorView.width,  // Align right edge with anchor
            0
        )
    }

    private fun showRenameDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rename_notebook, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.et_notebook_name)
        editText.setText(findViewById<TextView>(R.id.tv_title).text)

        val dialog = AlertDialog.Builder(this, R.style.CustomMaterialDialog)
            .setView(dialogView)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_rename).setOnClickListener {
            val newTitle = editText.text.toString().trim()
            if (newTitle.isNotEmpty()) {
                // Get current notebook and update it
                notebookViewModel.getNotebook(notebookId).value?.let { notebook ->
                    val updatedNotebook = notebook.copy(title = newTitle)
                    notebookViewModel.updateNotebook(updatedNotebook)
                    
                    // Send result back to MainActivity for sync
                    val intent = Intent().apply {
                        putExtra("RENAMED_NOTEBOOK_ID", notebookId)
                        putExtra("NEW_TITLE", newTitle)
                    }
                    setResult(RESULT_OK, intent)
                    
                    Toast.makeText(this, "Notebook renamed", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun showDeleteDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_notebook, null)
        
        val dialog = AlertDialog.Builder(this, R.style.CustomMaterialDialog)
            .setView(dialogView)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_delete).setOnClickListener {
            // Get current notebook and delete it
            notebookViewModel.getNotebook(notebookId).value?.let { notebook ->
                notebookViewModel.deleteNotebook(notebook)
                
                // Delete associated files
                val folder = File(filesDir, notebookId)
                if (folder.exists() && folder.isDirectory) {
                    folder.deleteRecursively()
                }
                
                Toast.makeText(this, "Notebook deleted", Toast.LENGTH_SHORT).show()
                
                // Set result and finish activity
                setResult(RESULT_OK, Intent().apply {
                    putExtra("DELETED_NOTEBOOK_ID", notebookId)
                })
                finish()
            }
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun showWelcomeMessage() {
        val welcomeMessage = Message(
            content = """
                👋 Welcome to your NeuroNotes AI assistant! I'm here to help you organize and understand your notes and sources.

                To get started:
                - Select any notes or sources you'd like to discuss
                - Ask me questions about your content
                - I can help summarize, analyze, or explain anything you've selected

                What would you like to explore today?
            """.trimIndent(),
            isUser = false,
            notebookId = notebookId
        )
        
        chatContext.add(welcomeMessage)
        messageAdapter.notifyItemInserted(chatContext.size - 1)
        recyclerView.scrollToPosition(chatContext.size - 1)
    }
}
