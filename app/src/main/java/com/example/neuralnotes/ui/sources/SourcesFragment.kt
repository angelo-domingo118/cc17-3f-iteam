import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.neuralnotes.databinding.FragmentSourcesBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SourcesFragment : Fragment() {

    private lateinit var binding: FragmentSourcesBinding
    private lateinit var viewModel: SourcesViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSourcesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(SourcesViewModel::class.java)
        binding.sourcesList.layoutManager = LinearLayoutManager(requireContext())
        binding.sourcesList.adapter = SourcesAdapter(viewModel)

        viewModel.sources.observe(viewLifecycleOwner, Observer { sources ->
            (binding.sourcesList.adapter as SourcesAdapter).submitList(sources)
        })

        binding.btnAddSource.setOnClickListener {
            showAddSourceBottomSheet()
        }
    }

    private fun showAddSourceBottomSheet() {
        val addSourceBottomSheet = AddSourceBottomSheetFragment.newInstance()
        addSourceBottomSheet.show(childFragmentManager, "AddSourceBottomSheet")
    }
}

class AddSourceBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddSourceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddSourceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAddFromFiles.setOnClickListener { /* Handle add from files */ }
        binding.btnWebsiteLink.setOnClickListener { /* Handle website link */ }
        binding.btnPasteText.setOnClickListener { /* Handle paste text */ }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = AddSourceBottomSheetFragment()
    }
}

class SourcesAdapter(private val viewModel: SourcesViewModel) :
    RecyclerView.Adapter<SourcesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FragmentSourcesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val source = viewModel.sources.value?.get(position)
        holder.bind(source)
    }

    override fun getItemCount(): Int {
        return viewModel.sources.value?.size ?: 0
    }

    inner class ViewHolder(private val binding: FragmentSourcesBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(source: Source?) {
            binding.sourceName.text = source?.name
            binding.sourceDescription.text = source?.description
        }
    }
}

class SourcesViewModel(private val repository: SourceRepository) : ViewModel() {

    private val _sources = MutableLiveData<List<Source>>()
    val sources: LiveData<List<Source>> = _sources

    init {
        refreshSources()
    }

    fun refreshSources() {
        viewModelScope.launch(Dispatchers.IO) {
            val sources = repository.getAllSources()
            _sources.postValue(sources)
        }
    }
}

class SourceRepository(private val dataSource: DataSource) {

    suspend fun getAllSources(): List<Source> {
        return dataSource.getAllSources()
    }
}

interface DataSource {
    suspend fun getAllSources(): List<Source>
}

data class Source(val id: Int, val name: String, val description: String)
