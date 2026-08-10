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
import com.example.axios.databinding.FragmentMembersWingnamesBinding

class MembersWingFragment : Fragment() {

    private var _binding: FragmentMembersWingnamesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: WingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMembersWingnamesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerWings.layoutManager = LinearLayoutManager(requireContext())
        adapter = WingAdapter(DataRepository.wings, R.layout.item_members_wing) { wingName ->
            // Open MembersFragment for this wing
            val fragment = MembersFragment.newInstance(wingName)
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.recyclerWings.adapter = adapter

        binding.fabAddWing.setOnClickListener {
            showAddWingDialog()
        }
    }

    private fun showAddWingDialog() {
        val etWingName = EditText(requireContext()).apply {
            hint = "Wing Name"
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Wing")
            .setView(etWingName)
            .setPositiveButton("Add") { dialog, _ ->
                val name = etWingName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                DataRepository.addWing(requireContext(), name) {
                    activity?.runOnUiThread {
                        adapter.updateData(DataRepository.wings)
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
        adapter.updateData(DataRepository.wings)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
