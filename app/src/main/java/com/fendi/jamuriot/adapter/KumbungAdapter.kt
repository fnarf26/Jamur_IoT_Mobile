package com.fendi.jamuriot.adapter

import android.content.Context
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
    private val onItemDeleted: (KumbungData) -> Unit,
    private val onItemClicked: (String) -> Unit, // Add click listener for device ID
    private val isAdmin: Boolean = false, // Add admin flag with default value
    private val context: Context? = null  // Add optional context parameter
) : RecyclerView.Adapter<KumbungAdapter.KumbungViewHolder>() {

    // Map to store device IDs corresponding to names
    private val deviceIds = mutableMapOf<String, String>()

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
            tvTemperature.text = data.temperatureAverage.toInt().toString()
            tvHumidity.text = data.humidityAverage.toInt().toString()

            val lastUpdateMillis = date?.time ?: 0L
            val currentTime = System.currentTimeMillis()
            val isOnline = currentTime - lastUpdateMillis < 60_000

            tvStatus.text = if (isOnline) "Online" else "Offline"
            viewStatusIndicator.setBackgroundResource(
                if (isOnline) R.drawable.circle_green else R.drawable.circle_red
            )

            // Set click listener on the root layout or on a specific button
            layoutDetailButton.setOnClickListener {
                deviceIds[data.name]?.let { deviceId ->
                    onItemClicked(deviceId)
                }
            }

            // Alternative: Set click listener on the entire item
            root.setOnClickListener {
                deviceIds[data.name]?.let { deviceId ->
                    onItemClicked(deviceId)
                }
            }
        }
    }

    override fun getItemCount(): Int = list.size

    // Add method to check deletion permission
    fun canDelete(): Boolean {
        return isAdmin
    }

    fun deleteItem(position: Int) {
        // Check if position is valid and user is admin
        if (position in list.indices && isAdmin) {
            // Double-check admin status if context is available
            if (context != null) {
                val prefs = context.getSharedPreferences("user", Context.MODE_PRIVATE)
                val currentRole = prefs.getString("role", "")

                // Only delete if still admin
                if (currentRole == "admin") {
                    val item = list[position]
                    list.removeAt(position)
                    notifyItemRemoved(position)
                    onItemDeleted(item)
                } else {
                    // Role changed, refresh the item
                    notifyItemChanged(position)
                }
            } else {
                // No context available, use the isAdmin flag only
                val item = list[position]
                list.removeAt(position)
                notifyItemRemoved(position)
                onItemDeleted(item)
            }
        } else {
            // Not admin or invalid position
            if (position in list.indices) {
                notifyItemChanged(position)
            }
        }
    }

    fun getItemAt(position: Int): KumbungData? {
        return list.getOrNull(position)
    }

    // Add method to update device IDs
    fun setDeviceId(name: String, deviceId: String) {
        deviceIds[name] = deviceId
    }
}
