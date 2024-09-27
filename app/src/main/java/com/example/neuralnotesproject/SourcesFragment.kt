package com.example.neuralnotesproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class SourcesFragment : Fragment() {

    private lateinit var sourceAdapter: SourceAdapter
    private val sources = mutableListOf<Source>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sources, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.rv_sources)
        sourceAdapter = SourceAdapter(sources) { source ->
            // Handle source click
        }
        recyclerView.adapter = sourceAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        view.findViewById<ImageButton>(R.id.btn_add_source).setOnClickListener {
            showAddSourceBottomSheet()
        }

        // Set up bottom action bar buttons
        view.findViewById<MaterialButton>(R.id.btn_select_all).setOnClickListener {
            sourceAdapter.selectAll()
        }

        view.findViewById<MaterialButton>(R.id.btn_unselect_all).setOnClickListener {
            sourceAdapter.unselectAll()
        }

        view.findViewById<MaterialButton>(R.id.btn_delete).setOnClickListener {
            deleteSelectedSources()
        }

        // Remove the call to addSampleSources()
    }

    private fun showAddSourceBottomSheet() {
        val addSourceBottomSheet = AddSourceBottomSheetFragment()
        addSourceBottomSheet.show(childFragmentManager, AddSourceBottomSheetFragment.TAG)
    }

    // Remove the addSampleSources() function

    fun addSource(source: Source) {
        sources.add(source)
        sourceAdapter.updateSources(sources)
    }

    private fun deleteSelectedSources() {
        val selectedSources = sourceAdapter.getSelectedItems()
        if (selectedSources.isNotEmpty()) {
            sources.removeAll(selectedSources)
            sourceAdapter.updateSources(sources)
            sourceAdapter.unselectAll()
            Toast.makeText(context, "${selectedSources.size} source(s) deleted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No sources selected", Toast.LENGTH_SHORT).show()
        }
    }
}