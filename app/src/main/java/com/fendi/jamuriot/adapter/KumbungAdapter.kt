package com.fendi.jamuriot.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.fendi.jamuriot.R
import com.fendi.jamuriot.databinding.ItemKumbungBinding
import com.fendi.jamuriot.model.KumbungData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KumbungAdapter(
    private val context: Context,
    private val list: List<KumbungData>
) : BaseAdapter() {

    override fun getCount(): Int = list.size
    override fun getItem(position: Int): Any = list[position]
    override fun getItemId(position: Int): Long = position.toLong()

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = ItemKumbungBinding.inflate(LayoutInflater.from(context), parent, false)
        val data = list[position]

        // Format timestamp
//        val sdf = SimpleDateFormat("HH:mm dd-MM-yyyy", Locale.getDefault())
//        val lastUpdateText = sdf.format(Date(data.lastUpdate))

        val inputFormat = SimpleDateFormat("dd/M/yyyy HH.mm.ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm dd-MM-yyyy", Locale.getDefault())

        val date = inputFormat.parse(data.lastUpdate)
        val lastUpdateText = date?.let { outputFormat.format(it) } ?: "Format salah"

        binding.tvTimestamp.text = lastUpdateText
        binding.tvLumbungName.text = data.name
        binding.tvTemperature.text = data.temperatureAverage.toInt().toString()
        binding.tvHumidity.text = data.humidityAverage.toInt().toString()

        // Format sesuai data kamu
        val sdf = SimpleDateFormat("dd/M/yyyy HH.mm.ss", Locale.getDefault())

        // Konversi string ke Date
        val lastUpdateDate = sdf.parse(data.lastUpdate)

        // Dapatkan waktu dalam millis
        val lastUpdateMillis = lastUpdateDate?.time ?: 0L

        // Hitung status online/offline
        val currentTime = System.currentTimeMillis()
        val isOnline = currentTime - lastUpdateMillis < 5 * 60 * 1000

        // Tampilkan status
        binding.tvStatus.text = if (isOnline) "Online" else "Offline"
        val statusDrawable = if (isOnline) R.drawable.circle_green else R.drawable.circle_blue
        binding.viewStatusIndicator.setBackgroundResource(statusDrawable)


        return binding.root
    }
}
