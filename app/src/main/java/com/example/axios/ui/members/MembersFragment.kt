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
            arguments = Bundle().apply { putString(ARG_WING_NAME, wingName) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wingName = arguments?.getString(ARG_WING_NAME) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.Wing.text = wingName
        loadMembers()
        binding.fabAddMember.setOnClickListener { showAddMemberDialog() }
    }

    private fun loadMembers() {
        val filtered = DataRepository.members.filter { it.wingName.equals(wingName, ignoreCase = true) }
        populateCategory(binding.coordinatorsContainer, filtered.filter { it.role.equals("Coordinator", ignoreCase = true) })
        populateCategory(binding.seniorMembersContainer, filtered.filter { it.role.equals("Senior Member", ignoreCase = true) })
        populateCategory(binding.membersContainer, filtered.filter { it.role.equals("Member", ignoreCase = true) })
    }

    private fun populateCategory(container: LinearLayout, list: List<DataRepository.Member>) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        val density = resources.displayMetrics.density
        list.forEach { member ->
            val item = ItemMemberBinding.inflate(inflater, container, false)
            // Preserve margins from XML by using the inflated root's existing layoutParams
            // and only override width/height if needed — don't strip margins
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (14 * density).toInt()
                bottomMargin = (4 * density).toInt()
            }
            item.root.layoutParams = lp
            item.memberName.text = member.name
            item.memberRollNo.text = member.rollNo
            item.memberContactInfo.text = member.contactInfo
            item.memberAvatar.text = member.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            item.btnEditMember.setOnClickListener { showEditMemberDialog(member) }
            item.btnDeleteMember.setOnClickListener { confirmDeleteMember(member) }
            container.addView(item.root)
        }
    }

    private fun showEditMemberDialog(member: DataRepository.Member) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_member, null)
        val etName = dialogView.findViewById<EditText>(R.id.etMemberName)
        val etRollNo = dialogView.findViewById<EditText>(R.id.etMemberRollNo)
        val etContact = dialogView.findViewById<EditText>(R.id.etMemberContact)
        val spinnerRole = dialogView.findViewById<Spinner>(R.id.spinnerRole)

        etName.setText(member.name)
        etRollNo.setText(member.rollNo)
        etContact.setText(member.contactInfo)

        val roles = listOf("Coordinator", "Senior Member", "Member")
        spinnerRole.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerRole.setSelection(roles.indexOf(member.role).coerceAtLeast(0))

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Member")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val name = etName.text.toString().trim()
                val rollNo = etRollNo.text.toString().trim().uppercase()
                val contact = etContact.text.toString().trim()
                val role = spinnerRole.selectedItem?.toString() ?: member.role
                if (name.isEmpty() || rollNo.isEmpty() || contact.isEmpty()) {
                    Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                DataRepository.editMember(requireContext(), member, name, rollNo, contact, role) {
                    activity?.runOnUiThread { loadMembers() }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun confirmDeleteMember(member: DataRepository.Member) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Member")
            .setMessage("Remove \"${member.name}\" from ${member.wingName}? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                DataRepository.deleteMember(requireContext(), member) {
                    activity?.runOnUiThread {
                        loadMembers()
                        Toast.makeText(requireContext(), "\"${member.name}\" removed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showAddMemberDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_member, null)
        val etName = dialogView.findViewById<EditText>(R.id.etMemberName)
        val etRollNo = dialogView.findViewById<EditText>(R.id.etMemberRollNo)
        val etContact = dialogView.findViewById<EditText>(R.id.etMemberContact)
        val spinnerRole = dialogView.findViewById<Spinner>(R.id.spinnerRole)

        spinnerRole.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
            listOf("Coordinator", "Senior Member", "Member")).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

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
                    activity?.runOnUiThread { loadMembers() }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
