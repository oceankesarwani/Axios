package com.example.axios.ui.resources

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            arguments = Bundle().apply {
                putString(ARG_WING_NAME, wingName)
            }
        }
    }

    // OpenDocument uses the system Storage Access Framework picker — no extra
    // permissions needed (unlike GetContent which fails for arbitrary file
    // types on Android 13+ without READ_MEDIA_DOCUMENTS).
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // Take a persistable permission so the background upload thread
            // can still read the URI after the picker dismisses.
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { /* not all providers support this */ }
            val fileName = getFileNameFromUri(uri) ?: "file_${System.currentTimeMillis()}"
            startUpload(uri, fileName)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wingName = arguments?.getString(ARG_WING_NAME) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResourcesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.heading.text = "$wingName Resources"

        binding.recyclerResources.layoutManager = LinearLayoutManager(requireContext())
        val filteredList = DataRepository.resources.filter { it.wingName.equals(wingName, ignoreCase = true) }
        adapter = ResourceAdapter(
            filteredList,
            onDelete = { resource -> confirmDelete(resource) },
            onOpen = { resource -> openFile(resource) }
        )
        binding.recyclerResources.adapter = adapter
        updateEmptyState(filteredList)

        // FAB opens file picker — pass all MIME types
        binding.fabAddWing.setOnClickListener {
            filePickerLauncher.launch(arrayOf("*/*"))
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = it.getString(idx)
            }
        }
        return name
    }

    private fun startUpload(uri: Uri, fileName: String) {
        // Build a progress dialog
        val progressBar = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val tvStatus = TextView(requireContext()).apply {
            text = "Uploading $fileName…"
            setPadding(48, 24, 48, 8)
        }
        val tvPercent = TextView(requireContext()).apply {
            text = "0%"
            setPadding(48, 4, 48, 24)
        }

        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
            addView(tvStatus)
            addView(progressBar)
            addView(tvPercent)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Uploading File")
            .setView(container)
            .setCancelable(false)
            .create()
        dialog.show()

        DataRepository.uploadFile(
            context = requireContext(),
            wingName = wingName,
            fileUri = uri,
            fileName = fileName,
            onProgress = { progress ->
                activity?.runOnUiThread {
                    progressBar.progress = progress
                    tvPercent.text = "$progress%"
                }
            },
            onSuccess = {
                activity?.runOnUiThread {
                    dialog.dismiss()
                    Toast.makeText(requireContext(), "\"$fileName\" uploaded!", Toast.LENGTH_SHORT).show()
                    refreshList()
                }
            },
            onFailure = { e ->
                activity?.runOnUiThread {
                    dialog.dismiss()
                    Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun confirmDelete(resource: DataRepository.Resource) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete File")
            .setMessage("Delete \"${resource.fileName}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                DataRepository.deleteResource(requireContext(), resource) {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "\"${resource.fileName}\" deleted", Toast.LENGTH_SHORT).show()
                        refreshList()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openFile(resource: DataRepository.Resource) {
        if (resource.downloadUrl.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resource.downloadUrl))
            startActivity(Intent.createChooser(intent, "Open with"))
        } else {
            Toast.makeText(requireContext(), "No download link available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshList() {
        val updatedList = DataRepository.resources.filter { it.wingName.equals(wingName, ignoreCase = true) }
        adapter.updateData(updatedList)
        updateEmptyState(updatedList)
    }

    private fun updateEmptyState(list: List<DataRepository.Resource>) {
        if (list.isEmpty()) {
            binding.tvEmptyResources.visibility = View.VISIBLE
            binding.recyclerResources.visibility = View.GONE
        } else {
            binding.tvEmptyResources.visibility = View.GONE
            binding.recyclerResources.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
