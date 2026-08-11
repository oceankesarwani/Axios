package com.example.axios.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class WingAdapter(
    private var items: List<String>,
    private val layoutResId: Int, // R.layout.item_members_wing or R.layout.item_resources_wing
    private val onItemClick: (String) -> Unit,
    private val onDelete: ((String) -> Unit)? = null
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
        val textView = holder.itemView.findViewById<android.widget.TextView>(com.example.axios.R.id.wingName)
        textView.text = item
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
        // Wire up delete button if present in this layout
        val deleteBtn = holder.itemView.findViewById<android.widget.ImageButton?>(com.example.axios.R.id.btnDeleteWing)
        if (deleteBtn != null && onDelete != null) {
            deleteBtn.setOnClickListener { onDelete.invoke(item) }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }
}
