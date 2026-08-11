package com.example.axios.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.axios.data.DataRepository
import com.example.axios.databinding.ItemResourceBinding

class ResourceAdapter(
    private var items: List<DataRepository.Resource>,
    private val onDelete: (DataRepository.Resource) -> Unit,
    private val onOpen: (DataRepository.Resource) -> Unit
) : RecyclerView.Adapter<ResourceAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemResourceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemResourceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Ensure layout height is wrap_content
        binding.root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvFileName.text = item.fileName
        holder.binding.btnDeleteResource.setOnClickListener { onDelete(item) }
        holder.binding.resourceCard.setOnClickListener { onOpen(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<DataRepository.Resource>) {
        items = newItems
        notifyDataSetChanged()
    }
}
