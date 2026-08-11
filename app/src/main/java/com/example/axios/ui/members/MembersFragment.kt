package com.example.axios.ui.members

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.axios.R
import com.example.axios.data.DataRepository
import com.example.axios.databinding.FragmentMembersBinding
import com.example.axios.databinding.ItemMemberBinding

class MembersFragment : Fragment() {

    private var _binding: FragmentMembersBinding? = null
    private val binding get() = _binding!!
    private var wingName: String = ""

    companion object {
        private const val ARG_WING_NAME = "wing_name"

        fun newInstance(wingName: String) = MembersFragment().apply {
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
        _binding = FragmentMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.Wing.text = wingName
        loadMembers()

        binding.fabAddMember.setOnClickListener {
            showAddMemberDialog()
        }
    }

    private fun loadMembers() {
        val filtered = DataRepository.members.filter { it.wingName.equals(wingName, ignoreCase = true) }
        val coordinators = filtered.filter { it.role.equals("Coordinator", ignoreCase = true) }
        val seniorMembers = filtered.filter { it.role.equals("Senior Member", ignoreCase = true) }
        val regularMembers = filtered.filter { it.role.equals("Member", ignoreCase = true) }

        populateCategory(binding.coordinatorsContainer, coordinators)
        populateCategory(binding.seniorMembersContainer, seniorMembers)
        populateCategory(binding.membersContainer, regularMembers)
    }

    private fun populateCategory(container: LinearLayout, list: List<DataRepository.Member>) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        for (member in list) {
            val itemBinding = ItemMemberBinding.inflate(inflater, container, false)
            itemBinding.root.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            itemBinding.memberName.text = member.name
            itemBinding.memberRollNo.text = member.rollNo
            itemBinding.memberContactInfo.text = member.contactInfo
            container.addView(itemBinding.root)
        }
    }

    private fun showAddMemberDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_member, null)
        val etName = dialogView.findViewById<EditText>(R.id.etMemberName)
        val etRollNo = dialogView.findViewById<EditText>(R.id.etMemberRollNo)
        val etContact = dialogView.findViewById<EditText>(R.id.etMemberContact)
        val spinnerRole = dialogView.findViewById<Spinner>(R.id.spinnerRole)

        val roles = listOf("Coordinator", "Senior Member", "Member")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = spinnerAdapter

        AlertDialog.Builder(requireContext())
            .setTitle("Add Wing Member")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val name = etName.text.toString().trim()
                val rollNo = etRollNo.text.toString().trim().uppercase()
                val contact = etContact.text.toString().trim()
                val role = spinnerRole.selectedItem?.toString() ?: "Member"

                if (name.isEmpty() || rollNo.isEmpty() || contact.isEmpty()) {
                    Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                DataRepository.addMember(requireContext(), wingName, name, rollNo, contact, role) {
                    activity?.runOnUiThread {
                        loadMembers()
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
