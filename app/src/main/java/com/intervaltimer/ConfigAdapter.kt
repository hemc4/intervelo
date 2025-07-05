package com.intervaltimer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ConfigAdapter(
    private val configs: MutableList<TimerConfig>,
    private val onItemClick: (TimerConfig) -> Unit,
    private val onDeleteClick: (TimerConfig) -> Unit
) : RecyclerView.Adapter<ConfigAdapter.ConfigViewHolder>() {

    class ConfigViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val configName: TextView = itemView.findViewById(R.id.textViewConfigName)
        val configDetails: TextView = itemView.findViewById(R.id.textViewConfigDetails)
        val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDeleteConfig)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConfigViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_config, parent, false)
        return ConfigViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConfigViewHolder, position: Int) {
        val config = configs[position]
        holder.configName.text = config.name
        holder.configDetails.text = "Sets: ${config.sets}, Work: ${config.workTime}s, Rest: ${config.restTime}s"
        holder.itemView.setOnClickListener { onItemClick(config) }
        holder.deleteButton.setOnClickListener { onDeleteClick(config) }
    }

    override fun getItemCount(): Int = configs.size
}