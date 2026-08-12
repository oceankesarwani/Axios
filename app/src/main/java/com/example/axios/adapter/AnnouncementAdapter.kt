package com.example.axios.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.axios.data.DataRepository
import com.example.axios.databinding.ItemAnnouncementBinding

class AnnouncementAdapter(
    private var items: List<DataRepository.Announcement>,
    private val onDelete: (DataRepository.Announcement) -> Unit
) : RecyclerView.Adapter<AnnouncementAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAnnouncementBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAnnouncementBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
            it.root.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.wingName.text = item.wingName
        holder.binding.message.text = item.message
        holder.binding.Info.text = item.info
        holder.binding.btnDeleteAnnouncement.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<DataRepository.Announcement>) {
        items = newItems
        notifyDataSetChanged()
    }
}
