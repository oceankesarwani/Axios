package com.example.axios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
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
        adapter = WingAdapter(DataRepository.wings, R.layout.item_resources_wing) { wingName ->
            // Open ResourcesFragment for this wing
            val fragment = ResourcesFragment.newInstance(wingName)
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.recyclerWings.adapter = adapter
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
