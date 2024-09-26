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

class NotebookInteractionActivity : AppCompatActivity() {
    private lateinit var notebookViewModel: NotebookViewModel
    private lateinit var notebookId: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var inputEditText: TextInputEditText
    private lateinit var sendButton: FloatingActionButton
    private lateinit var generativeModel: GenerativeModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notebook_interaction)

        notebookViewModel = ViewModelProvider(this)[NotebookViewModel::class.java]

        notebookId = intent.getStringExtra("NOTEBOOK_ID") ?: return

        notebookViewModel.getNotebook(notebookId).observe(this) { notebook ->
            notebook?.let {
                findViewById<TextView>(R.id.tv_title).text = it.title
                // You can use other notebook details as needed
            }
        }

        recyclerView = findViewById(R.id.recyclerView)
        inputEditText = findViewById(R.id.et_user_input)
        sendButton = findViewById(R.id.btn_send)

        messageAdapter = MessageAdapter(mutableListOf())
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

    private fun sendMessage(message: String) {
        if (message.isEmpty()) {
            showError("Message cannot be empty")
            return
        }

        messageAdapter.addMessage(Message(message, true))
        recyclerView.scrollToPosition(messageAdapter.itemCount - 1)

        // Use Gemini API to generate response
        lifecycleScope.launch {
            try {
                val response = generativeModel.generateContent(message)
                val aiResponse = response.text ?: "Sorry, I couldn't generate a response."
                messageAdapter.addMessage(Message(aiResponse, false))
                recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
            } catch (e: Exception) {
                showError("Failed to generate AI response: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_notebook_options, null)

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        // Set up click listeners for the options
        popupView.findViewById<LinearLayout>(R.id.delete_option).setOnClickListener {
            deleteNotebook()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.rename_option).setOnClickListener {
            renameNotebook()
            popupWindow.dismiss()
        }

        // Show the popup window
        popupWindow.elevation = 10f
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.popup_background))
        popupWindow.showAsDropDown(anchorView)
    }

    private fun deleteNotebook() {
        notebookViewModel.deleteNotebook(notebookId)
        Toast.makeText(this, "Notebook deleted", Toast.LENGTH_SHORT).show()
        finish() // Close the activity after deleting
    }

    private fun renameNotebook() {
        // Implement rename functionality
        // For now, we'll just show a toast. In a real app, you'd open a dialog to input the new name.
        Toast.makeText(this, "Rename functionality to be implemented", Toast.LENGTH_SHORT).show()
    }

    private fun showSourcesFragment() {
        // Hide the RecyclerView
        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.GONE

        // Show the fragment container
        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE

        // Create and show the SourcesFragment
        val sourcesFragment = SourcesFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, sourcesFragment)
            .commit()
    }

    private fun showNotesFragment() {
        // Hide the RecyclerView
        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.GONE

        // Show the fragment container
        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE

        // Create and show the NotesFragment
        val notesFragment = NotesFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, notesFragment)
            .commit()
    }

    private fun showChatView() {
        // Hide the fragment container
        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE

        // Show the RecyclerView
        findViewById<RecyclerView>(R.id.recyclerView).visibility = View.VISIBLE
    }
}