package com.example.axios.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.axios.R

class WingAdapter(
    private var items: List<String>,
    private val layoutResId: Int,
    private val onItemClick: (String) -> Unit,
    private val onDelete: ((String) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false).also {
            it.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        holder.itemView.findViewById<TextView>(R.id.wingName).text = item
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.itemView.findViewById<ImageButton?>(R.id.btnDeleteWing)
            ?.takeIf { onDelete != null }
            ?.setOnClickListener { onDelete?.invoke(item) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }
}
