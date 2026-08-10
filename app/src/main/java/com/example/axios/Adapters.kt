package com.example.axios

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.axios.databinding.ItemAnnouncementBinding
import com.example.axios.databinding.ItemMembersWingBinding
import com.example.axios.databinding.ItemResourcesWingBinding
import com.example.axios.databinding.ItemResourceBinding

class AnnouncementAdapter(private var items: List<DataRepository.Announcement>) :
    RecyclerView.Adapter<AnnouncementAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAnnouncementBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnnouncementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Adjust the layout height to wrap_content so items don't stretch to full screen height
        binding.root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.wingName.text = item.wingName
        holder.binding.message.text = item.message
        holder.binding.Info.text = item.info
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<DataRepository.Announcement>) {
        items = newItems
        notifyDataSetChanged()
    }
}

class WingAdapter(
    private var items: List<String>,
    private val layoutResId: Int, // R.layout.item_members_wing or R.layout.item_resources_wing
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(layoutResId, parent, false)
        // Ensure root view uses wrap_content
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        val textView = holder.itemView.findViewById<android.widget.TextView>(R.id.wingName)
        textView.text = item
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }
}

class ResourceAdapter(private var items: List<DataRepository.Resource>) :
    RecyclerView.Adapter<ResourceAdapter.ViewHolder>() {

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
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<DataRepository.Resource>) {
        items = newItems
        notifyDataSetChanged()
    }
}
