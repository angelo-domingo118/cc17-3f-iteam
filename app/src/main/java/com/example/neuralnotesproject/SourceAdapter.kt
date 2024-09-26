package com.example.neuralnotesproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.collections.mutableSetOf

class SourceAdapter(
    private var sources: List<Source>,
    private val onSourceClick: (Source) -> Unit
) : RecyclerView.Adapter<SourceAdapter.ViewHolder>() {

    private val selectedItems = mutableSetOf<String>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileIcon: ImageView = view.findViewById(R.id.iv_file_icon)
        val fileName: TextView = view.findViewById(R.id.tv_file_name)
        val sourceCheckbox: CheckBox = view.findViewById(R.id.cb_source)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_source, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val source = sources[position]
        holder.fileName.text = source.name
        
        // Set the appropriate icon based on the source type
        holder.fileIcon.setImageResource(
            when (source.type) {
                SourceType.FILE -> R.drawable.ic_file
                SourceType.WEBSITE -> R.drawable.ic_web
                SourceType.TEXT -> R.drawable.ic_text
            }
        )

        holder.sourceCheckbox.isChecked = selectedItems.contains(source.id)
        holder.sourceCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedItems.add(source.id)
            } else {
                selectedItems.remove(source.id)
            }
        }

        holder.itemView.setOnClickListener { onSourceClick(source) }
    }

    override fun getItemCount() = sources.size

    fun updateSources(newSources: List<Source>) {
        sources = newSources
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectedItems.addAll(sources.map { it.id })
        notifyDataSetChanged()
    }

    fun unselectAll() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<Source> {
        return sources.filter { selectedItems.contains(it.id) }
    }
}