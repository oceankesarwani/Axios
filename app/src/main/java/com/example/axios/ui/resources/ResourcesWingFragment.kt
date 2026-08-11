package com.example.axios.ui.resources

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.axios.R
import com.example.axios.adapter.WingAdapter
import com.example.axios.data.DataRepository
import com.example.axios.databinding.FragmentResourcesWingnamesBinding

class ResourcesWingFragment : Fragment() {

    private var _binding: FragmentResourcesWingnamesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: WingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResourcesWingnamesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerWings.layoutManager = LinearLayoutManager(requireContext())
        adapter = WingAdapter(
            DataRepository.wings,
            R.layout.item_resources_wing,
            onItemClick = { wingName ->
                val fragment = ResourcesFragment.newInstance(wingName)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onDelete = { wingName -> confirmDeleteWing(wingName) }
        )
        binding.recyclerWings.adapter = adapter
    }

    private fun confirmDeleteWing(wingName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Wing")
            .setMessage("Delete \"$wingName\"? This will permanently remove all its resources, members, and announcements.")
            .setPositiveButton("Delete") { _, _ ->
                DataRepository.deleteWing(requireContext(), wingName) {
                    activity?.runOnUiThread {
                        adapter.updateData(DataRepository.wings)
                        Toast.makeText(requireContext(), "\"$wingName\" deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
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
