package com.intervelo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MultiSetConfigAdapter(
    private val configs: MutableList<MultiSetConfig>,
    private val onItemClick: (MultiSetConfig) -> Unit,
    private val onDeleteClick: (MultiSetConfig) -> Unit
) : RecyclerView.Adapter<MultiSetConfigAdapter.MultiSetConfigViewHolder>() {

    class MultiSetConfigViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val configName: TextView = itemView.findViewById(R.id.textViewMultiSetConfigName)
        val configDetails: TextView = itemView.findViewById(R.id.textViewMultiSetConfigDetails)
        val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDeleteMultiSetConfig)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiSetConfigViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_multiset_config, parent, false)
        return MultiSetConfigViewHolder(view)
    }

    override fun onBindViewHolder(holder: MultiSetConfigViewHolder, position: Int) {
        val config = configs[position]
        holder.configName.text = config.name
        
        // Generate details text showing each config in the sequence
        val details = config.configs.joinToString(" → ") { subConfig ->
            "${subConfig.sets} sets (${subConfig.workTime}s/${subConfig.restTime}s)"
        }
        holder.configDetails.text = details
        
        holder.itemView.setOnClickListener { onItemClick(config) }
        holder.deleteButton.setOnClickListener { onDeleteClick(config) }
    }

    override fun getItemCount(): Int = configs.size
}
