package com.fendi.jamuriot.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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

    private var tempThreshold: Float = 100f
    private var humidityThreshold: Float = 0f // Default diubah agar tidak salah picu

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
            tvTimestamp.text = lastUpdateText

            tvTemperature.text = "${data.temperatureAverage.toInt()}°C"
            tvHumidity.text = "${data.humidityAverage.toInt()}%"

            val lastUpdateMillis = date?.time ?: 0L
            val currentTime = System.currentTimeMillis()
            val isOnline = currentTime - lastUpdateMillis < 60_000

            tvStatus.text = if (isOnline) "Online" else "Offline"
            viewStatusIndicator.setBackgroundResource(
                if (isOnline) R.drawable.circle_green else R.drawable.circle_red
            )

            val context = holder.itemView.context
            val yellowColor = ContextCompat.getColor(context, R.color.warning_yellow)
            val defaultColor = Color.WHITE

            // Peringatan untuk SUHU (jika di atas atau sama dengan batas)
            if (data.temperatureAverage >= tempThreshold) {
                tvTemperature.setTextColor(yellowColor)
            } else {
                tvTemperature.setTextColor(defaultColor)
            }

            // --- PERUBAHAN LOGIKA DI SINI ---
            // Peringatan untuk KELEMBAPAN (jika di bawah atau sama dengan batas)
            if (data.humidityAverage <= humidityThreshold) {
                tvHumidity.setTextColor(yellowColor)
            } else {
                tvHumidity.setTextColor(defaultColor)
            }
            // --- AKHIR PERUBAHAN ---

            layoutDetailButton.setOnClickListener {
                deviceIds[data.name]?.let { deviceId ->
                    onDetailClickListener(deviceId)
                }
            }
        }
    }

    override fun getItemCount(): Int = kumbungList.size

    fun setThresholds(temp: Float, humidity: Float) {
        this.tempThreshold = temp
        this.humidityThreshold = humidity
        notifyDataSetChanged()
    }

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