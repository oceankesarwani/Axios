package com.example.axios.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.axios.R
import com.example.axios.adapter.AnnouncementAdapter
import com.example.axios.data.DataRepository
import com.example.axios.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AnnouncementAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerAnnouncements.layoutManager = LinearLayoutManager(requireContext())
        adapter = AnnouncementAdapter(DataRepository.announcements)
        binding.recyclerAnnouncements.adapter = adapter
        updateEmptyState()

        binding.fabAddWing.setOnClickListener {
            showAddAnnouncementDialog()
        }
    }

    private fun updateEmptyState() {
        if (DataRepository.announcements.isEmpty()) {
            binding.tvEmptyAnnouncements.visibility = View.VISIBLE
            binding.recyclerAnnouncements.visibility = View.GONE
        } else {
            binding.tvEmptyAnnouncements.visibility = View.GONE
            binding.recyclerAnnouncements.visibility = View.VISIBLE
        }
    }

    private fun showAddAnnouncementDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_announcement, null)
        val spinnerWing = dialogView.findViewById<Spinner>(R.id.spinnerWing)
        val etMessage = dialogView.findViewById<EditText>(R.id.etMessage)
        val etInfo = dialogView.findViewById<EditText>(R.id.etInfo)

        val wingList = DataRepository.wings
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, wingList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWing.adapter = spinnerAdapter

        AlertDialog.Builder(requireContext())
            .setTitle("New Announcement")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val selectedWing = spinnerWing.selectedItem?.toString() ?: ""
                val message = etMessage.text.toString().trim()
                val info = etInfo.text.toString().trim()

                if (selectedWing.isEmpty()) {
                    Toast.makeText(requireContext(), "Please add a wing first!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (message.isEmpty() || info.isEmpty()) {
                    Toast.makeText(requireContext(), "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                DataRepository.addAnnouncement(requireContext(), selectedWing, message, info) {
                    activity?.runOnUiThread {
                        adapter.updateData(DataRepository.announcements)
                        updateEmptyState()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        adapter.updateData(DataRepository.announcements)
        updateEmptyState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
