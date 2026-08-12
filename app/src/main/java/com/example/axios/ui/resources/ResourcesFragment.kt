package com.example.axios.ui.resources

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.axios.adapter.ResourceAdapter
import com.example.axios.data.DataRepository
import com.example.axios.databinding.FragmentResourcesBinding

class ResourcesFragment : Fragment() {

    private var _binding: FragmentResourcesBinding? = null
    private val binding get() = _binding!!
    private var wingName: String = ""
    private lateinit var adapter: ResourceAdapter

    companion object {
        private const val ARG_WING_NAME = "wing_name"
        fun newInstance(wingName: String) = ResourcesFragment().apply {
            arguments = Bundle().apply { putString(ARG_WING_NAME, wingName) }
        }
    }

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        try {
            requireContext().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {}
        startUpload(uri, getFileNameFromUri(uri) ?: "file_${System.currentTimeMillis()}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wingName = arguments?.getString(ARG_WING_NAME) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentResourcesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.heading.text = "$wingName Resources"
        binding.recyclerResources.layoutManager = LinearLayoutManager(requireContext())
        val filteredList = DataRepository.resources.filter { it.wingName.equals(wingName, ignoreCase = true) }
        adapter = ResourceAdapter(filteredList, onDelete = { confirmDelete(it) }, onOpen = { openFile(it) })
        binding.recyclerResources.adapter = adapter
        updateEmptyState(filteredList)
        binding.fabAddWing.setOnClickListener { filePickerLauncher.launch(arrayOf("*/*")) }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        return requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                .takeIf { it >= 0 }?.let { cursor.getString(it) }
            else null
        }
    }

    private fun startUpload(uri: Uri, fileName: String) {
        val progressBar = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val tvStatus = TextView(requireContext()).apply { text = "Uploading $fileName…"; setPadding(48, 24, 48, 8) }
        val tvPercent = TextView(requireContext()).apply { text = "0%"; setPadding(48, 4, 48, 24) }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
            addView(tvStatus); addView(progressBar); addView(tvPercent)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Uploading File").setView(container).setCancelable(false).create()
        dialog.show()

        DataRepository.uploadFile(requireContext(), wingName, uri, fileName,
            onProgress = { activity?.runOnUiThread { progressBar.progress = it; tvPercent.text = "$it%" } },
            onSuccess = { activity?.runOnUiThread { dialog.dismiss(); toast("\"$fileName\" uploaded!"); refreshList() } },
            onFailure = { e -> activity?.runOnUiThread { dialog.dismiss(); toast("Upload failed: ${e.message}", Toast.LENGTH_LONG) } }
        )
    }

    private fun confirmDelete(resource: DataRepository.Resource) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete File")
            .setMessage("Delete \"${resource.fileName}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                DataRepository.deleteResource(requireContext(), resource) {
                    activity?.runOnUiThread { toast("\"${resource.fileName}\" deleted"); refreshList() }
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun openFile(resource: DataRepository.Resource) {
        if (resource.downloadUrl.isNotEmpty())
            startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(resource.downloadUrl)), "Open with"))
        else toast("No download link available")
    }

    private fun refreshList() {
        val list = DataRepository.resources.filter { it.wingName.equals(wingName, ignoreCase = true) }
        adapter.updateData(list); updateEmptyState(list)
    }

    private fun updateEmptyState(list: List<DataRepository.Resource>) {
        val empty = list.isEmpty()
        binding.tvEmptyResources.visibility = if (empty) View.VISIBLE else View.GONE
        binding.recyclerResources.visibility = if (empty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun toast(msg: String, length: Int = Toast.LENGTH_SHORT) =
        Toast.makeText(requireContext(), msg, length).show()
}
