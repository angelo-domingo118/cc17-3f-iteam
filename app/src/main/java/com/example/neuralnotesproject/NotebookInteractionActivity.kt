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

class NotebookInteractionActivity : AppCompatActivity() {
    private lateinit var notebookViewModel: NotebookViewModel
    private lateinit var notebookId: String
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
        messageAdapter = MessageAdapter(
            messages = chatContext,
            onMessageClick = { message -> handleMessageSave(message) }
        )
        recyclerView.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(this@NotebookInteractionActivity)
        }
    }

    private fun handleMessageSave(message: Message) {
        // Handle message save
        Toast.makeText(this, "Message saved", Toast.LENGTH_SHORT).show()
    }

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

        // Initialize Gemini API with Flash model
        initializeGeminiApi()

        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE

        initializeViews()
        setupTabLayout()
        setupMessageHandling()
        setupRecyclerView()
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
        updateTabStates(0)
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
        currentFragment = notesFragment
        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.GONE
        updateTabStates(1)
    }

    private fun showChatView() {
        hideCurrentFragment()
        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.VISIBLE
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

    private fun sendMessage(message: String) {
        // Add message to chat
        val userMessage = Message(message, true)
        chatContext.add(userMessage)
        messageAdapter.addMessage(userMessage)
        
        // Show loading state
        isLoading = true
        progressBar.visibility = View.VISIBLE
        
        // Generate AI response
        lifecycleScope.launch {
            try {
                val response = generateResponse(message)
                val aiMessage = Message(response, false)
                chatContext.add(aiMessage)
                messageAdapter.addMessage(aiMessage)
            } catch (e: Exception) {
                Toast.makeText(this@NotebookInteractionActivity, 
                    "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
                progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun generateResponse(userMessage: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildString {
                    append("Context:\n")
                    if (selectedNotesContent.isNotEmpty()) {
                        append("Selected Notes:\n$selectedNotesContent\n\n")
                    }
                    if (selectedSourcesContent.isNotEmpty()) {
                        append("Selected Sources:\n$selectedSourcesContent\n\n")
                    }
                    append("User Message: $userMessage")
                }

                // Use generateContent with String parameter
                val response = generativeModel.generateContent(prompt)
                response.text ?: "Sorry, I couldn't generate a response."
            } catch (e: Exception) {
                Log.e("AI Response", "Error generating response", e)
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
}
