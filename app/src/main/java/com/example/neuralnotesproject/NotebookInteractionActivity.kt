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

    private lateinit var progressBar: ProgressBar
    private var isLoading = false

    private fun setupMessageHandling() {
        messageAdapter = MessageAdapter(chatContext) { message ->
            handleMessageSave(message)
        }
        
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

    private fun handleMessageSave(message: Message) {
        // Handle message save
        Toast.makeText(this, "Message saved", Toast.LENGTH_SHORT).show()
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
        progressBar = findViewById(R.id.progressBar)
        navigationTabs = findViewById(R.id.tab_layout)

        // Initialize ViewModel
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = NotebookRepository(database.notebookDao())
        notebookViewModel = ViewModelProvider(
            this,
            NotebookViewModelFactory(repository, userId)
        )[com.example.neuralnotesproject.viewmodels.NotebookViewModel::class.java]

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
                        hideCurrentFragment()
                        if (sourcesFragment == null) {
                            sourcesFragment = SourcesFragment.newInstance(notebookId)
                        }
                        showFragment(sourcesFragment!!)
                        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
                        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.GONE
                        findViewById<ConstraintLayout>(R.id.bottom_user_input_navigation).visibility = View.GONE
                    }
                    1 -> { // Notes
                        hideCurrentFragment()
                        if (notesFragment == null) {
                            notesFragment = NotesFragment.newInstance(notebookId)
                        }
                        showFragment(notesFragment!!)
                        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
                        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.GONE
                        findViewById<ConstraintLayout>(R.id.bottom_user_input_navigation).visibility = View.GONE
                    }
                    2 -> { // Chat
                        hideCurrentFragment()
                        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
                        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.VISIBLE
                        findViewById<ConstraintLayout>(R.id.bottom_user_input_navigation).visibility = View.VISIBLE
                        currentFragment = null
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
        hideCurrentFragment()
        if (sourcesFragment == null) {
            sourcesFragment = SourcesFragment.newInstance(notebookId)
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, sourcesFragment!!)
            .commit()
        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.GONE
        findViewById<ConstraintLayout>(R.id.bottom_user_input_navigation).visibility = View.GONE
        updateTabStates(0)
    }

    private fun showNotesFragment() {
        hideCurrentFragment()
        if (notesFragment == null) {
            notesFragment = NotesFragment.newInstance(notebookId)
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, notesFragment!!)
            .commit()
        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.GONE
        findViewById<ConstraintLayout>(R.id.bottom_user_input_navigation).visibility = View.GONE
        updateTabStates(1)
    }

    private fun showChatView() {
        hideCurrentFragment()
        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.VISIBLE
        findViewById<ConstraintLayout>(R.id.bottom_user_input_navigation).visibility = View.VISIBLE
        currentFragment = null
        updateTabStates(2)
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
            temperature = 0.7f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 1024
        }

        generativeModel = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = "AIzaSyBi_46ImoqYxa69XDTUA2fjSQQjhuFhfuY",
            generationConfig = config
        )
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        messageAdapter = MessageAdapter(
            messages = chatContext,
            onMessageClick = { message -> 
                // Handle message click if needed
            }
        )
        recyclerView.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(this@NotebookInteractionActivity)
        }
    }

    private fun sendMessage(userMessage: String) {
        // Add user message to chat
        val userMsg = Message(
            content = userMessage,
            isUser = true,
            notebookId = notebookId
        )
        chatContext.add(userMsg)
        messageAdapter.updateMessages(chatContext)
        recyclerView.scrollToPosition(chatContext.size - 1)

        // Show loading
        progressBar.visibility = View.VISIBLE
        isLoading = true

        // Generate AI response
        lifecycleScope.launch {
            try {
                val response = generateAiResponse(userMessage)
                
                // Add AI response to chat
                val aiMsg = Message(
                    content = response,
                    isUser = false,
                    notebookId = notebookId
                )
                chatContext.add(aiMsg)
                
                withContext(Dispatchers.Main) {
                    messageAdapter.updateMessages(chatContext)
                    recyclerView.scrollToPosition(chatContext.size - 1)
                    progressBar.visibility = View.GONE
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@NotebookInteractionActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    progressBar.visibility = View.GONE
                    isLoading = false
                }
            }
        }
    }

    private suspend fun generateAiResponse(userMessage: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = "User: $userMessage\nAssistant:"
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
                👋 Welcome to Neural Notes! I'm your AI assistant. Here's what I can help you with:

                📝 Notes Management:
                - Summarize your notes
                - Extract key points
                - Generate study questions
                
                📚 Source Analysis:
                - Analyze documents and websites
                - Extract relevant information
                - Compare different sources

                💡 Study Help:
                - Answer questions about your notes
                - Explain complex topics
                - Help with research

                Just select your notes/sources and ask me anything! How can I help you today?
            """.trimIndent(),
            isUser = false,
            notebookId = notebookId
        )
        
        chatContext.add(welcomeMessage)
        messageAdapter.notifyItemInserted(chatContext.size - 1)
        recyclerView.scrollToPosition(chatContext.size - 1)
    }
}
