package com.example.neuralnotesproject

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.neuralnotesproject.data.Source
import com.example.neuralnotesproject.data.SourceType
import kotlin.collections.mutableSetOf

class SourceAdapter(
    private var sources: List<Source>,
    private val onSourceClick: (Source) -> Unit,
    private val onSelectionChanged: (List<Source>) -> Unit
) : RecyclerView.Adapter<SourceAdapter.SourceViewHolder>() {

    private val selectedSources = mutableSetOf<Source>()

    inner class SourceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.tv_source_title)
        private val typeTextView: TextView = itemView.findViewById(R.id.tv_source_type)
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkbox_source)

        fun bind(source: Source) {
            titleTextView.text = when (source.type) {
                SourceType.WEBSITE -> "Website: ${source.name}"
                else -> source.name
            }
            
            typeTextView.text = when (source.type) {
                SourceType.PASTE_TEXT -> "Pasted Text"
                SourceType.WEBSITE -> "Website"
                SourceType.FILE -> "File"
            }
            
            checkbox.isChecked = selectedSources.contains(source)
            
            itemView.setOnClickListener {
                if (source.type == SourceType.WEBSITE) {
                    // Open website in browser
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.content))
                    itemView.context.startActivity(intent)
                } else {
                    onSourceClick(source)
                }
            }
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedSources.add(source)
                } else {
                    selectedSources.remove(source)
                }
                onSelectionChanged(selectedSources.toList())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SourceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_source, parent, false)
        return SourceViewHolder(view)
    }

    override fun onBindViewHolder(holder: SourceViewHolder, position: Int) {
        holder.bind(sources[position])
    }

    override fun getItemCount() = sources.size

    fun updateSources(newSources: List<Source>) {
        sources = newSources
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectedSources.addAll(sources)
        notifyDataSetChanged()
    }

    fun unselectAll() {
        selectedSources.clear()
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<Source> {
        return selectedSources.toList()
    }
}
