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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.axios.R
import com.example.axios.adapter.WingAdapter
import com.example.axios.data.DataRepository
import com.example.axios.databinding.FragmentMembersWingnamesBinding
import com.example.axios.databinding.ItemMemberBinding

class MembersWingFragment : Fragment() {

    private var _binding: FragmentMembersWingnamesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: WingAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMembersWingnamesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerWings.layoutManager = LinearLayoutManager(requireContext())
        adapter = WingAdapter(DataRepository.wings, R.layout.item_members_wing,
            onItemClick = { wingName ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, MembersFragment.newInstance(wingName))
                    .addToBackStack(null).commit()
            },
            onDelete = { confirmDeleteWing(it) }
        )
        binding.recyclerWings.adapter = adapter
        binding.fabAddWing.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Add")
                .setItems(arrayOf("Overall Coordinator", "Wing")) { _, which ->
                    if (which == 0) showAddOCDialog() else showAddWingDialog()
                }
                .show()
        }

        loadOverallCoordinators()
    }

    private fun loadOverallCoordinators() {
        val container = binding.overallCoordinatorsContainer
        container.removeAllViews()
        val overallCoords = DataRepository.members.filter {
            it.role.equals("Overall Coordinator", ignoreCase = true)
        }
        val inflater = LayoutInflater.from(requireContext())
        val density = resources.displayMetrics.density
        overallCoords.forEach { member ->
            val item = ItemMemberBinding.inflate(inflater, container, false)
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
            item.btnEditMember.setOnClickListener { showEditOCDialog(member) }
            item.btnDeleteMember.setOnClickListener { confirmDeleteOC(member) }
            container.addView(item.root)
        }
    }

    private fun showEditOCDialog(member: DataRepository.Member) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_member, null)
        val etName = dialogView.findViewById<EditText>(R.id.etMemberName)
        val etRollNo = dialogView.findViewById<EditText>(R.id.etMemberRollNo)
        val etContact = dialogView.findViewById<EditText>(R.id.etMemberContact)
        val spinnerRole = dialogView.findViewById<Spinner>(R.id.spinnerRole)

        etName.setText(member.name)
        etRollNo.setText(member.rollNo)
        etContact.setText(member.contactInfo)

        // OC can only stay as Overall Coordinator when edited from this screen
        spinnerRole.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
            listOf("Overall Coordinator")).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Overall Coordinator")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val name = etName.text.toString().trim()
                val rollNo = etRollNo.text.toString().trim().uppercase()
                val contact = etContact.text.toString().trim()
                if (name.isEmpty() || rollNo.isEmpty() || contact.isEmpty()) {
                    Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                DataRepository.editMember(requireContext(), member, name, rollNo, contact, "Overall Coordinator") {
                    activity?.runOnUiThread { loadOverallCoordinators() }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun confirmDeleteOC(member: DataRepository.Member) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Overall Coordinator")
            .setMessage("Remove \"${member.name}\"? This cannot be undone.")
            .setPositiveButton("Remove") { _, _ ->
                DataRepository.deleteMember(requireContext(), member) {
                    activity?.runOnUiThread {
                        loadOverallCoordinators()
                        Toast.makeText(requireContext(), "\"${member.name}\" removed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showAddOCDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_member, null)
        val etName = dialogView.findViewById<EditText>(R.id.etMemberName)
        val etRollNo = dialogView.findViewById<EditText>(R.id.etMemberRollNo)
        val etContact = dialogView.findViewById<EditText>(R.id.etMemberContact)
        val spinnerRole = dialogView.findViewById<Spinner>(R.id.spinnerRole)

        spinnerRole.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
            listOf("Overall Coordinator")).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Overall Coordinator")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val name = etName.text.toString().trim()
                val rollNo = etRollNo.text.toString().trim().uppercase()
                val contact = etContact.text.toString().trim()
                if (name.isEmpty() || rollNo.isEmpty() || contact.isEmpty()) {
                    Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                // Store OC with a generic "Overall" wing tag (not tied to any specific wing)
                DataRepository.addMember(requireContext(), "Overall", name, rollNo, contact, "Overall Coordinator") {
                    activity?.runOnUiThread { loadOverallCoordinators() }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun confirmDeleteWing(wingName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Wing")
            .setMessage("Delete \"$wingName\"? This will permanently remove all its members, announcements, and resources.")
            .setPositiveButton("Delete") { _, _ ->
                DataRepository.deleteWing(requireContext(), wingName) {
                    activity?.runOnUiThread {
                        adapter.updateData(DataRepository.wings)
                        Toast.makeText(requireContext(), "\"$wingName\" deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showAddWingDialog() {
        val etWingName = EditText(requireContext()).apply {
            hint = "Wing Name"
            val p = (16 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Add New Wing")
            .setView(etWingName)
            .setPositiveButton("Add") { dialog, _ ->
                val name = etWingName.text.toString().trim()
                if (name.isEmpty()) { Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                DataRepository.addWing(requireContext(), name) {
                    activity?.runOnUiThread { adapter.updateData(DataRepository.wings) }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null).show()
    }

    override fun onResume() {
        super.onResume()
        adapter.updateData(DataRepository.wings)
        loadOverallCoordinators()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
