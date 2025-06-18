package com.fendi.jamuriot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fendi.jamuriot.model.NotifikasiData
import java.text.SimpleDateFormat
import java.util.*

class NotifikasiAdapter(
    private val notifikasiList: List<NotifikasiData>
) : RecyclerView.Adapter<NotifikasiAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage: TextView = itemView.findViewById(R.id.tv_notification_message)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tv_notification_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notifikasi, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifikasiList[position]

        holder.tvMessage.text = "${notification.message}\n(${notification.deviceId})"
        holder.tvTimestamp.text = formatTimestamp(notification.timestamp)
    }

    private fun formatTimestamp(timestamp: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val date = inputFormat.parse(timestamp) ?: return timestamp
            return outputFormat.format(date)
        } catch (e: Exception) {
            return timestamp
        }
    }

    override fun getItemCount(): Int = notifikasiList.size
}