package com.fendi.jamuriot.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fendi.jamuriot.R
import com.fendi.jamuriot.databinding.ItemKumbungMonitoringBinding
import com.fendi.jamuriot.model.KumbungData
import java.text.SimpleDateFormat
import java.util.*

class KumbungMonitoringAdapter(
    private val kumbungList: MutableList<KumbungData> = mutableListOf(),
    private val deviceIds: MutableMap<String, String> = mutableMapOf(),
    private val onDetailClickListener: (String) -> Unit
) : RecyclerView.Adapter<KumbungMonitoringAdapter.KumbungViewHolder>() {

    inner class KumbungViewHolder(val binding: ItemKumbungMonitoringBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KumbungViewHolder {
        val binding = ItemKumbungMonitoringBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return KumbungViewHolder(binding)
    }

    override fun onBindViewHolder(holder: KumbungViewHolder, position: Int) {
        val data = kumbungList[position]

        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm dd-MM-yyyy", Locale.getDefault())

        val date = try {
            inputFormat.parse(data.lastUpdate)
        } catch (e: Exception) {
            null
        }

        val lastUpdateText = date?.let { outputFormat.format(it) } ?: "Format salah"

        with(holder.binding) {
            tvLumbungIMEI.text = data.imei
            tvLumbungName.text = data.name
            tvTemperature.text = data.temperatureAverage.toInt().toString()
            tvHumidity.text = data.humidityAverage.toInt().toString()
            tvTimestamp.text = lastUpdateText

            val lastUpdateMillis = date?.time ?: 0L
            val currentTime = System.currentTimeMillis()
            val isOnline = currentTime - lastUpdateMillis < 60_000 // 1 minute threshold

            tvStatus.text = if (isOnline) "Online" else "Offline"
            viewStatusIndicator.setBackgroundResource(
                if (isOnline) R.drawable.circle_green else R.drawable.circle_red
            )

            // Set click listener for detail button
            layoutDetailButton.setOnClickListener {
                deviceIds[data.name]?.let { deviceId ->
                    onDetailClickListener(deviceId)
                }
            }
        }
    }

    override fun getItemCount(): Int = kumbungList.size

    // Add this method to KumbungMonitoringAdapter.kt
    fun clearDeviceIdMappings() {
        deviceIds.clear()
    }

    fun updateData(newList: List<KumbungData>) {
        kumbungList.clear()
        kumbungList.addAll(newList)
        notifyDataSetChanged()
    }

    fun setDeviceId(name: String, deviceId: String) {
        deviceIds[name] = deviceId
    }

    fun getDeviceId(position: Int): String? {
        return if (position in 0 until kumbungList.size) {
            deviceIds[kumbungList[position].name]
        } else {
            null
        }
    }

    fun getKumbungAt(position: Int): KumbungData? {
        return kumbungList.getOrNull(position)
    }
}