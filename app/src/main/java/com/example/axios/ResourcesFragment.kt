package com.example.axios

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
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
        adapter = ResourceAdapter(filteredList)
        binding.recyclerResources.adapter = adapter
        updateEmptyState(filteredList)

        binding.fabAddWing.setOnClickListener {
            showAddResourceDialog()
        }
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

    private fun showAddResourceDialog() {
        val etFileName = EditText(requireContext()).apply {
            hint = "Resource File Name (e.g. Kotlin_Basics.pdf)"
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Resource")
            .setView(etFileName)
            .setPositiveButton("Add") { dialog, _ ->
                val fileName = etFileName.text.toString().trim()
                if (fileName.isEmpty()) {
                    Toast.makeText(requireContext(), "File name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                DataRepository.addResource(requireContext(), wingName, fileName) {
                    activity?.runOnUiThread {
                        val updatedList = DataRepository.resources.filter { it.wingName.equals(wingName, ignoreCase = true) }
                        adapter.updateData(updatedList)
                        updateEmptyState(updatedList)
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
