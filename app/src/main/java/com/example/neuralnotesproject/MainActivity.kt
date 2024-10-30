package com.example.neuralnotesproject

import android.content.Intent
import android.os.Build
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
import java.io.*
import androidx.activity.result.contract.ActivityResultContracts
import android.text.TextWatcher
import android.text.Editable
import androidx.annotation.RequiresApi
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.MotionEvent
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import androidx.cardview.widget.CardView
import android.widget.FrameLayout
import android.graphics.Rect
import com.example.neuralnotesproject.data.AppDatabase
import com.example.neuralnotesproject.repository.NotebookRepository
import com.example.neuralnotesproject.viewmodels.NotebookViewModelFactory
import com.example.neuralnotesproject.data.Notebook
import com.example.neuralnotesproject.viewmodels.NotebookViewModel
import com.google.android.material.textfield.TextInputEditText
import android.graphics.Color
import android.graphics.drawable.ColorDrawable

class MainActivity : AppCompatActivity() {

    private lateinit var notebookListContainer: RecyclerView
    private lateinit var notebookAdapter: NotebookAdapter
    private lateinit var notebookViewModel: NotebookViewModel
    private var userId: String = ""

    private lateinit var etSearch: EditText
    private var allNotebooks: List<Notebook> = emptyList()

    private val notebookInteractionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            when {
                data?.hasExtra("DELETED_NOTEBOOK_ID") == true -> {
                    val deletedNotebookId = data.getStringExtra("DELETED_NOTEBOOK_ID")
                    deletedNotebookId?.let { id ->
                        // Find the notebook with this ID and delete it
                        notebookViewModel.notebooks.value?.find { it.id == id }?.let { notebook ->
                            notebookViewModel.deleteNotebook(notebook)
                        }
                    }
                }
                data?.hasExtra("RENAMED_NOTEBOOK_ID") == true -> {
                    val renamedNotebookId = data.getStringExtra("RENAMED_NOTEBOOK_ID")
                    val newTitle = data.getStringExtra("NEW_TITLE")
                    if (renamedNotebookId != null && newTitle != null) {
                        notebookViewModel.renameNotebook(renamedNotebookId, newTitle)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get userId from intent
        userId = intent.getStringExtra("USER_ID") ?: return

        // Initialize database and repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = NotebookRepository(database.notebookDao())

        // Initialize ViewModel with factory
        notebookViewModel = ViewModelProvider(
            this,
            NotebookViewModelFactory(repository, userId)
        )[NotebookViewModel::class.java]

        setupRecyclerView()
        loadNotebooks()  // Load notebooks when the app starts
        observeNotebooks()

        val btnNewNotebook: MaterialButton = findViewById(R.id.btn_new_notebook)
        setupAddNotebookButtonAnimation(btnNewNotebook)
        btnNewNotebook.setOnClickListener {
            addNewNotebook()
        }

        etSearch = findViewById(R.id.et_search)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterNotebooks(s.toString())
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
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
            allNotebooks = notebooks
            filterNotebooks(etSearch.text.toString())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addNewNotebook() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_notebook, null)
        val notebookNameInput = dialogView.findViewById<TextInputEditText>(R.id.et_notebook_name)
        
        val dialog = AlertDialog.Builder(this, R.style.CustomMaterialDialog)
            .setView(dialogView)
            .create()

        // Set click listeners for buttons
        dialogView.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_create).setOnClickListener {
            val notebookName = notebookNameInput.text.toString()
            if (notebookName.isNotEmpty()) {
                val currentDateTime = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm:ss")
                val formattedDate = currentDateTime.format(formatter)
                
                val newNotebook = Notebook(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    title = notebookName,
                    creationDate = formattedDate
                )
                notebookViewModel.addNotebook(newNotebook)
                dialog.dismiss()
            } else {
                notebookNameInput.error = "Please enter a notebook name"
            }
        }

        // Remove default background and set custom background
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun saveNotebook(notebook: Notebook) {
        val folderName = notebook.id
        val fileName = "notebook_details.txt"
        
        try {
            val folder = File(filesDir, folderName)
            folder.mkdirs()
            
            // Create subfolders
            File(folder, "sources").mkdirs()
            File(folder, "chat").mkdirs()
            File(folder, "notes").mkdirs()
            
            val file = File(folder, fileName)
            val writer = FileWriter(file)
            writer.use {
                it.write("${notebook.id}\n")
                it.write("${notebook.title}\n")
                it.write("${notebook.creationDate}\n")
            }
        } catch (e: IOException) {
            Log.e("MainActivity", "Error saving notebook", e)
        }
    }

    private fun loadNotebooks() {
        // Remove the file-based loading since we're using Room database now
        // The notebooks will be automatically loaded through the ViewModel's LiveData
        notebookViewModel.notebooks.observe(this) { notebooks ->
            notebookAdapter.updateNotebooks(notebooks)
        }
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
        
        // Calculate the position of the popup
        val location = IntArray(2)
        anchorView.getLocationInWindow(location)
        val anchorRect = Rect(location[0], location[1], location[0] + anchorView.width, location[1] + anchorView.height)
        
        // Measure the popup view
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        
        // Calculate the x position (align right sides)
        val xPos = anchorRect.right - popupView.measuredWidth
        
        // Calculate the y position (slightly below the anchor view)
        val yPos = anchorRect.bottom + 10 // 10dp padding below the anchor view
        
        // Show the popup
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, xPos, yPos)
    }

    private fun deleteNotebook(position: Int) {
        val notebook = notebookViewModel.notebooks.value?.get(position) ?: return
        AlertDialog.Builder(this)
            .setTitle("Delete Notebook")
            .setMessage("Are you sure you want to delete this notebook?")
            .setPositiveButton("Delete") { _, _ ->
                notebookViewModel.deleteNotebook(notebook)
                
                // Delete from storage
                val folder = File(filesDir, notebook.id)
                if (folder.exists() && folder.isDirectory) {
                    folder.deleteRecursively()
                }
                
                Toast.makeText(this, "Notebook deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameDialog(position: Int) {
        val notebook = notebookViewModel.notebooks.value?.get(position) ?: return
        val input = EditText(this)
        input.setText(notebook.title)
        
        AlertDialog.Builder(this)
            .setTitle("Rename Notebook")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newTitle = input.text.toString()
                if (newTitle.isNotEmpty()) {
                    notebookViewModel.renameNotebook(notebook.id, newTitle)
                    Toast.makeText(this, "Notebook renamed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun onNotebookClick(position: Int) {
        val notebook = notebookViewModel.notebooks.value?.get(position)
        notebook?.let {
            val intent = Intent(this, NotebookInteractionActivity::class.java).apply {
                putExtra("NOTEBOOK_ID", it.id)
                putExtra("NOTEBOOK_TITLE", it.title)
            }
            notebookInteractionLauncher.launch(intent)
        }
    }

    private fun filterNotebooks(query: String) {
        val filteredList = if (query.isEmpty()) {
            allNotebooks
        } else {
            allNotebooks.filter { it.title.contains(query, ignoreCase = true) }
        }
        notebookAdapter.updateNotebooks(filteredList)
    }

    private fun setupAddNotebookButtonAnimation(button: MaterialButton) {
        val scaleUpX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.05f)
        val scaleUpY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.05f)
        val scaleDownX = ObjectAnimator.ofFloat(button, "scaleX", 1.05f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(button, "scaleY", 1.05f, 1f)

        val scaleUp = AnimatorSet().apply {
            playTogether(scaleUpX, scaleUpY)
            duration = 100
        }

        val scaleDown = AnimatorSet().apply {
            playTogether(scaleDownX, scaleDownY)
            duration = 100
        }

        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    scaleUp.start()
                    darkenButtonColor(button)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    scaleDown.start()
                    resetButtonColor(button)
                }
            }
            false
        }
    }

    private fun darkenButtonColor(button: MaterialButton) {
        val drawable = button.background as? GradientDrawable
        drawable?.let {
            val darkerStartColor = manipulateColor(ContextCompat.getColor(this, R.color.gradient_start), 0.8f)
            val darkerEndColor = manipulateColor(ContextCompat.getColor(this, R.color.gradient_end), 0.8f)
            it.colors = intArrayOf(darkerStartColor, darkerEndColor)
        }
    }

    private fun resetButtonColor(button: MaterialButton) {
        val drawable = button.background as? GradientDrawable
        drawable?.let {
            val startColor = ContextCompat.getColor(this, R.color.gradient_start)
            val endColor = ContextCompat.getColor(this, R.color.gradient_end)
            it.colors = intArrayOf(startColor, endColor)
        }
    }

    private fun manipulateColor(color: Int, factor: Float): Int {
        val a = color and 0xff000000.toInt()
        val r = (color and 0x00ff0000) shr 16
        val g = (color and 0x0000ff00) shr 8
        val b = color and 0x000000ff

        return a or
                ((r * factor).toInt() shl 16) or
                ((g * factor).toInt() shl 8) or
                (b * factor).toInt()
    }

    fun onProfileIconClick(view: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun onOptionsClick(view: View, position: Int) {
        val notebook = notebookViewModel.notebooks.value?.get(position) ?: return
        
        val popupView = LayoutInflater.from(this).inflate(R.layout.popup_menu, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupView.findViewById<TextView>(R.id.menu_rename).setOnClickListener {
            popupWindow.dismiss()
            showRenameDialog(notebook)  // Pass the notebook object
        }

        popupView.findViewById<TextView>(R.id.menu_delete).setOnClickListener {
            popupWindow.dismiss()
            showDeleteDialog(notebook)
        }

        popupWindow.showAsDropDown(view)
    }

    private fun showRenameDialog(notebook: Notebook) {  // Changed to take Notebook parameter
        val input = EditText(this)
        input.setText(notebook.title)
        
        AlertDialog.Builder(this)
            .setTitle("Rename Notebook")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newTitle = input.text.toString()
                if (newTitle.isNotEmpty()) {
                    val updatedNotebook = notebook.copy(title = newTitle)
                    notebookViewModel.updateNotebook(updatedNotebook)
                    Toast.makeText(this, "Notebook renamed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(notebook: Notebook) {
        AlertDialog.Builder(this)
            .setTitle("Delete Notebook")
            .setMessage("Are you sure you want to delete this notebook?")
            .setPositiveButton("Delete") { _, _ ->
                notebookViewModel.deleteNotebook(notebook)
                Toast.makeText(this, "Notebook deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

class NotebookAdapter(
    private var notebooks: List<Notebook>,
    private val onOptionsClick: (View, Int) -> Unit,
    private val onNotebookClick: (Int) -> Unit
) : RecyclerView.Adapter<NotebookAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.tv_notebook_title)
        val dateTextView: TextView = view.findViewById(R.id.tv_creation_date)
        val optionsButton: ImageView = view.findViewById(R.id.iv_more_options)
        val notebookIcon: ImageView = view.findViewById(R.id.iv_notebook_icon)
        val notebookIconFrame: FrameLayout = view.findViewById(R.id.fl_notebook_icon)
        val cardView: CardView = view as CardView
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

        // Set up touch listener for the entire item view
        holder.itemView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    animateItem(holder, true)
                    v.isPressed = true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    animateItem(holder, false)
                    v.isPressed = false
                    if (event.action == MotionEvent.ACTION_UP) {
                        onNotebookClick(position)
                    }
                }
            }
            true
        }

        // Reset the item appearance when it's recycled
        holder.itemView.isPressed = false
        resetItemAppearance(holder)
    }

    private fun animateItem(holder: ViewHolder, isPressed: Boolean) {
        val context = holder.itemView.context
        if (isPressed) {
            // Animate notebook icon
            holder.notebookIcon.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(100)
                .start()

            // Change text color
            holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.gradient_end))
            holder.dateTextView.setTextColor(ContextCompat.getColor(context, R.color.gradient_start))

            // Increase card elevation
            holder.cardView.animate()
                .translationZ(8f)
                .setDuration(100)
                .start()

            // Change notebook icon background color
            (holder.notebookIconFrame.background as? GradientDrawable)?.colors = intArrayOf(
                ContextCompat.getColor(context, R.color.gradient_start),
                ContextCompat.getColor(context, R.color.gradient_end)
            )
        } else {
            resetItemAppearance(holder)
        }
    }

    private fun resetItemAppearance(holder: ViewHolder) {
        val context = holder.itemView.context
        // Reset notebook icon size
        holder.notebookIcon.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(100)
            .start()

        // Reset text color
        holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.primary_text))
        holder.dateTextView.setTextColor(ContextCompat.getColor(context, R.color.secondary_text))

        // Reset card elevation
        holder.cardView.animate()
            .translationZ(2f)
            .setDuration(100)
            .start()

        // Reset notebook icon background color
        (holder.notebookIconFrame.background as? GradientDrawable)?.colors = intArrayOf(
            ContextCompat.getColor(context, R.color.gradient_start),
            ContextCompat.getColor(context, R.color.gradient_end)
        )
    }

    override fun getItemCount() = notebooks.size

    fun updateNotebooks(newNotebooks: List<Notebook>) {
        notebooks = newNotebooks
        notifyDataSetChanged()
    }
}
