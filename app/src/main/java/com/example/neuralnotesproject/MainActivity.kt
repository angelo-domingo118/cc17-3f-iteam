package com.example.neuralnotesproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.Observer

class MainActivity : AppCompatActivity() {

    private lateinit var notebookListContainer: RecyclerView
    private lateinit var notebookAdapter: NotebookAdapter
    private lateinit var notebookViewModel: NotebookViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        notebookViewModel = ViewModelProvider(this)[NotebookViewModel::class.java]

        setupRecyclerView()
        observeNotebooks()

        val btnNewNotebook: MaterialButton = findViewById(R.id.btn_new_notebook)
        btnNewNotebook.setOnClickListener {
            addNewNotebook()
        }
    }

    private fun setupRecyclerView() {
        notebookListContainer = findViewById(R.id.notebook_list_container)
        notebookAdapter = NotebookAdapter(emptyList(), ::showNotebookOptions, ::onNotebookClick)
        notebookListContainer.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = notebookAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeNotebooks() {
        notebookViewModel.notebooks.observe(this) { notebooks ->
            notebookAdapter.updateNotebooks(notebooks)
        }
    }

    private fun addNewNotebook() {
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm:ss")
        val formattedDate = currentDateTime.format(formatter)
        
        val newNotebook = Notebook(
            id = UUID.randomUUID().toString(),
            title = "Notebook ${notebookViewModel.notebooks.value?.size?.plus(1) ?: 1}",
            creationDate = formattedDate
        )
        notebookViewModel.addNotebook(newNotebook)
    }

    private fun showNotebookOptions(anchorView: View, position: Int) {
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
            deleteNotebook(position)
            popupWindow.dismiss()
        }

        renameOption.setOnClickListener {
            popupWindow.dismiss()
            showRenameDialog(position)
        }

        popupWindow.elevation = 10f
        popupWindow.showAsDropDown(anchorView, 0, 0, Gravity.END)
    }

    private fun deleteNotebook(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Notebook")
            .setMessage("Are you sure you want to delete this notebook?")
            .setPositiveButton("Delete") { _, _ ->
                notebookViewModel.deleteNotebook(position)
                Toast.makeText(this, "Notebook deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameDialog(position: Int) {
        val input = EditText(this)
        input.setText(notebookViewModel.notebooks.value?.get(position)?.title)
        
        AlertDialog.Builder(this)
            .setTitle("Rename Notebook")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newTitle = input.text.toString()
                if (newTitle.isNotEmpty()) {
                    renameNotebook(position, newTitle)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renameNotebook(position: Int, newTitle: String) {
        notebookViewModel.renameNotebook(position, newTitle)
        Toast.makeText(this, "Notebook renamed", Toast.LENGTH_SHORT).show()
    }

    private fun onNotebookClick(position: Int) {
        try {
            val notebook = notebookViewModel.notebooks.value?.get(position)
            notebook?.let {
                val intent = Intent(this, NotebookInteractionActivity::class.java).apply {
                    putExtra("NOTEBOOK_ID", it.id)
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting NotebookInteractionActivity", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

data class Notebook(val id: String, var title: String, val creationDate: String)

class NotebookAdapter(
    private var notebooks: List<Notebook>,
    private val onOptionsClick: (View, Int) -> Unit,
    private val onNotebookClick: (Int) -> Unit
) : RecyclerView.Adapter<NotebookAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.tv_notebook_title)
        val dateTextView: TextView = view.findViewById(R.id.tv_creation_date)
        val optionsButton: ImageView = view.findViewById(R.id.iv_more_options)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notebook, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notebook = notebooks[position]
        holder.titleTextView.text = notebook.title
        holder.dateTextView.text = notebook.creationDate
        holder.optionsButton.setOnClickListener { view ->
            onOptionsClick(view, position)
        }
        holder.itemView.setOnClickListener {
            onNotebookClick(position)
        }
    }

    override fun getItemCount() = notebooks.size

    fun updateNotebooks(newNotebooks: List<Notebook>) {
        notebooks = newNotebooks
        notifyDataSetChanged()
    }
}