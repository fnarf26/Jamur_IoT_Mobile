package com.fendi.jamuriot.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fendi.jamuriot.R
import com.fendi.jamuriot.databinding.ItemKumbungBinding
import com.fendi.jamuriot.model.KumbungData
import java.text.SimpleDateFormat
import java.util.*

class KumbungAdapter(
    private val list: MutableList<KumbungData>,
    private val onItemDeleted: (KumbungData) -> Unit
) : RecyclerView.Adapter<KumbungAdapter.KumbungViewHolder>() {

    inner class KumbungViewHolder(val binding: ItemKumbungBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KumbungViewHolder {
        val binding = ItemKumbungBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return KumbungViewHolder(binding)
    }

    override fun onBindViewHolder(holder: KumbungViewHolder, position: Int) {
        val data = list[position]

        val inputFormat = SimpleDateFormat("dd/M/yyyy HH.mm.ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm dd-MM-yyyy", Locale.getDefault())

        val date = try {
            inputFormat.parse(data.lastUpdate)
        } catch (e: Exception) {
            null
        }

        val lastUpdateText = date?.let { outputFormat.format(it) } ?: "Format salah"

        with(holder.binding) {
            tvTimestamp.text = lastUpdateText
            tvLumbungName.text = data.name
            tvTemperature.text = data.temperatureAverage.toInt().toString()
            tvHumidity.text = data.humidityAverage.toInt().toString()

            val lastUpdateMillis = date?.time ?: 0L
            val currentTime = System.currentTimeMillis()
            val isOnline = currentTime - lastUpdateMillis < 60_000

            tvStatus.text = if (isOnline) "Online" else "Offline"
            viewStatusIndicator.setBackgroundResource(
                if (isOnline) R.drawable.circle_green else R.drawable.circle_blue
            )
        }
    }

    override fun getItemCount(): Int = list.size

    fun deleteItem(position: Int) {
        if (position in list.indices) {
            val item = list[position]
            list.removeAt(position)
            notifyItemRemoved(position)
            onItemDeleted(item)
        }
    }

    fun getItemAt(position: Int): KumbungData? {
        return list.getOrNull(position)
    }
}
