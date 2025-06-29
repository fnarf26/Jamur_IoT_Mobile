package com.fendi.jamuriot.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.fendi.jamuriot.R
import com.fendi.jamuriot.databinding.ItemKumbungBinding
import com.fendi.jamuriot.model.KumbungData
import java.text.SimpleDateFormat
import java.util.*

class KumbungAdapter(
    private val list: MutableList<KumbungData>,
    private val onItemDeleted: (KumbungData) -> Unit,
    private val onItemClicked: (String) -> Unit,
    private val isAdmin: Boolean = false,
    private val context: Context? = null
) : RecyclerView.Adapter<KumbungAdapter.KumbungViewHolder>() {

    private val deviceIds = mutableMapOf<String, String>()
    private var tempThreshold: Float = 100f
    private var humidityThreshold: Float = 0f // Default diubah agar tidak salah picu

    inner class KumbungViewHolder(val binding: ItemKumbungBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KumbungViewHolder {
        val binding = ItemKumbungBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return KumbungViewHolder(binding)
    }

    override fun onBindViewHolder(holder: KumbungViewHolder, position: Int) {
        val data = list[position]

        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm:ss dd-MM-yyyy", Locale.getDefault())

        val date = try {
            inputFormat.parse(data.lastUpdate)
        } catch (e: Exception) {
            null
        }

        val lastUpdateText = date?.let { outputFormat.format(it) } ?: "Format salah"

        with(holder.binding) {
            tvTimestamp.text = lastUpdateText
            tvLumbungIMEI.text = data.imei
            tvLumbungName.text = data.name

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
                deviceIds[data.name]?.let { deviceId -> onItemClicked(deviceId) }
            }

            root.setOnClickListener {
                deviceIds[data.name]?.let { deviceId -> onItemClicked(deviceId) }
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun setThresholds(temp: Float, humidity: Float) {
        this.tempThreshold = temp
        this.humidityThreshold = humidity
        notifyDataSetChanged()
    }

    fun canDelete(): Boolean = isAdmin

    fun deleteItem(position: Int) {
        if (position in list.indices && isAdmin) {
            val item = list[position]
            list.removeAt(position)
            notifyItemRemoved(position)
            onItemDeleted(item)
        }
    }

    fun getItemAt(position: Int): KumbungData? = list.getOrNull(position)

    fun setDeviceId(name: String, deviceId: String) {
        deviceIds[name] = deviceId
    }
}