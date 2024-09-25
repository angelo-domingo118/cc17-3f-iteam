import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.neuralnotes.databinding.BottomSheetAddSourceBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

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
        binding.btnWebsiteLink.setOnClickListener { showWebsiteUrlInput() }
        binding.btnPasteText.setOnClickListener { /* Handle paste text */ }
    }

    private fun showWebsiteUrlInput() {
        val websiteUrlView = layoutInflater.inflate(R.layout.layout_website_url_input, null)
        dialog?.setContentView(websiteUrlView)

        websiteUrlView.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            dialog?.setContentView(binding.root)
        }

        websiteUrlView.findViewById<Button>(R.id.btn_next).setOnClickListener {
            val urlInput = websiteUrlView.findViewById<TextInputEditText>(R.id.et_url_input)
            val url = urlInput.text.toString()
            // Handle the URL input
            // You might want to validate the URL here before proceeding
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = AddSourceBottomSheetFragment()
    }
}