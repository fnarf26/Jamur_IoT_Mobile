package com.fendi.jamuriot

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fendi.jamuriot.databinding.ActivityDetailKumbungBinding
import com.fendi.jamuriot.model.SensorHistoryData
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.data.Entry
import androidx.core.content.ContextCompat

class DetailKumbungActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailKumbungBinding
    private lateinit var dbRef: DatabaseReference
    private lateinit var settingsRef: DatabaseReference
    private var deviceId: String = ""
    
    // Default threshold values (used if settings not available)
    private var batasSuhu: Int = 33
    private var batasKelembapan: Int = 50

    private lateinit var lineChart: LineChart
    private var showingTemperatureChart = true  // true = temperature, false = humidity
    private val historyItems = mutableListOf<SensorHistoryData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailKumbungBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get device ID from intent
        deviceId = intent.getStringExtra("DEVICE_ID") ?: ""
        if (deviceId.isEmpty()) {
            Toast.makeText(this, "Error: Device ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize database references
        dbRef = FirebaseDatabase.getInstance().getReference("devices").child(deviceId)
        settingsRef = FirebaseDatabase.getInstance().getReference("settings")

        // Get user role from SharedPreferences
        val prefs = this.getSharedPreferences("user", Context.MODE_PRIVATE)
        val userRole = prefs.getString("role", "petugas") ?: "petugas"

        // Control visibility based on role
        if (userRole == "admin") {
            binding.btnEditName.visibility = View.GONE
            binding.btnEditMedia.visibility = View.GONE
            binding.btnPanen.visibility = View.GONE
        }
        
        // Load settings first
        loadSettings()
        
        // Initialize chart
        lineChart = binding.lineChart
        setupChart()
        
        // Set up button listeners
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnPanen.setOnClickListener {
            // Show confirmation dialog
            android.app.AlertDialog.Builder(this)
                .setTitle("Konfirmasi Panen")
                .setMessage("Apakah Anda yakin ingin menambah jumlah panen untuk kumbung ini?")
                .setPositiveButton("Ya") { _, _ ->
                    // Get current harvest count
                    dbRef.child("harvestCount").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val currentHarvestCount = snapshot.getValue(Long::class.java) ?: 0
                            val newHarvestCount = currentHarvestCount + 1
                            
                            // Update harvest count in Firebase
                            dbRef.child("harvestCount").setValue(newHarvestCount)
                                .addOnSuccessListener {
                                    Toast.makeText(this@DetailKumbungActivity, 
                                        "Jumlah panen berhasil diperbarui: $newHarvestCount kali", 
                                        Toast.LENGTH_SHORT).show()
                                    
                                    // Update the UI immediately
                                    binding.tvPanenStatus.text = "BERHASIL PANEN $newHarvestCount KALI"
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this@DetailKumbungActivity, 
                                        "Gagal memperbarui jumlah panen: ${e.message}", 
                                        Toast.LENGTH_SHORT).show()
                                }
                        }
                        
                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@DetailKumbungActivity, 
                                "Gagal memuat data panen: ${error.message}", 
                                Toast.LENGTH_SHORT).show()
                        }
                    })
                }
                .setNegativeButton("Tidak", null)
                .show()
        }
        
        binding.btnHapus.setOnClickListener {
            // Show confirmation dialog before deleting
            android.app.AlertDialog.Builder(this)
                .setTitle("Hapus Kumbung")
                .setMessage("Apakah Anda yakin ingin menghapus kumbung ini?")
                .setPositiveButton("Ya") { _, _ ->
                    deleteKumbung()
                }
                .setNegativeButton("Tidak", null)
                .show()
        }
        
        binding.btnEditName.setOnClickListener {
            showEditNameDialog()
        }
        
        binding.btnEditMedia.setOnClickListener {
            showEditMediaDialog()
        }
        
        // Load sensor history
        loadSensorHistory()
        
        // Set up chart tabs
        binding.tabTemperature.setOnClickListener {
            showingTemperatureChart = true
            updateTabsUI()
            updateChart()
        }
        
        binding.tabHumidity.setOnClickListener {
            showingTemperatureChart = false
            updateTabsUI()
            updateChart()
        }
    }
    
    private fun loadSettings() {
        settingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    batasSuhu = snapshot.child("batasSuhu").getValue(Int::class.java) ?: 33
                    batasKelembapan = snapshot.child("batasKelembapan").getValue(Int::class.java) ?: 50
                    
                    // Now that we have settings, load the kumbung data
                    loadKumbungData()
                } else {
                    // If settings don't exist, still load the data with default values
                    loadKumbungData()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DetailKumbungActivity, "Gagal memuat pengaturan: ${error.message}", Toast.LENGTH_SHORT).show()
                // Still try to load data with default values
                loadKumbungData()
            }
        })
    }

    private fun loadKumbungData() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@DetailKumbungActivity, "Data tidak ditemukan", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }

                val name = snapshot.child("name").getValue(String::class.java) ?: "Kumbung"
                val temp1 = snapshot.child("temperature1").getValue(Double::class.java) ?: 0.0
                val temp2 = snapshot.child("temperature2").getValue(Double::class.java) ?: 0.0
                val tempAvg = snapshot.child("temperatureAverage").getValue(Double::class.java) ?: 0.0
                
                val hum1 = snapshot.child("humidity1").getValue(Double::class.java) ?: 0.0
                val hum2 = snapshot.child("humidity2").getValue(Double::class.java) ?: 0.0
                val humAvg = snapshot.child("humidityAverage").getValue(Double::class.java) ?: 0.0
                
                val lastUpdate = snapshot.child("lastUpdate").getValue(String::class.java) ?: ""
                
                // Get harvest and media counts
                val harvestCount = snapshot.child("harvestCount").getValue(Long::class.java) ?: 0
                val mediaCount = snapshot.child("mediaCount").getValue(Long::class.java) ?: 0
                
                // Set title and header
                binding.tvDetailTitle.text = "Detail $name"
                
                // Set name in editable field
                binding.tvKumbungName.text = name
                
                // Set sensor 1 data
                binding.includeSensor1.tvSensorName.text = "Sensor 1"
                binding.includeSensor1.tvSensorType.text = "DHT22"
                binding.includeSensor1.tvTemperature.text = "$temp1 °C"
                binding.includeSensor1.tvHumidity.text = "$hum1 %"
                binding.includeSensor1.tvTimestamp.text = formatDateTime(lastUpdate)
                
                // Set sensor 2 data
                binding.includeSensor2.tvSensorName.text = "Sensor 2"
                binding.includeSensor2.tvSensorType.text = "DHT22"
                binding.includeSensor2.tvTemperature.text = "$temp2 °C"
                binding.includeSensor2.tvHumidity.text = "$hum2 %"
                binding.includeSensor2.tvTimestamp.text = formatDateTime(lastUpdate)
                
                // Set average data
                binding.includeAverage.tvAverageTemperature.text = "$tempAvg °C"
                binding.includeAverage.tvAverageHumidity.text = "$humAvg %"
                binding.includeAverage.tvAverageTimestamp.text = formatDateTime(lastUpdate)
                
                // Hide warning by default
                binding.includeAverage.tvStatusWarning.visibility = View.GONE
                
                // Check if temperature is out of normal range using settings from Firebase
                if (tempAvg > batasSuhu) {
                    binding.includeAverage.tvStatusWarning.text = "Suhu diluar batas normal! (>$batasSuhu°C)"
                    binding.includeAverage.tvStatusWarning.visibility = View.VISIBLE
                }
                
                // Check if humidity is out of normal range using settings from Firebase
                else if (humAvg < batasKelembapan) {
                    binding.includeAverage.tvStatusWarning.text = "Kelembapan diluar batas normal! (<$batasKelembapan%)"
                    binding.includeAverage.tvStatusWarning.visibility = View.VISIBLE
                }
                
                // Update panen status text
                binding.tvPanenStatus.text = "BERHASIL PANEN $harvestCount KALI"
                
                // Update media tanam with actual media count
                binding.tvMediaTanam.text = "$mediaCount Pcs"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DetailKumbungActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun loadSensorHistory() {
        val riwayatRef = dbRef.child("riwayat")
        
        riwayatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Clear existing history items
                val historyContainer = binding.historyItemsContainer
                
                // Keep only the header row (first child)
                val headerView = historyContainer.getChildAt(0)
                historyContainer.removeAllViews()
                historyContainer.addView(headerView)
                
                // Clear history items list
                historyItems.clear()
                
                if (!snapshot.exists() || snapshot.childrenCount.toInt() == 0) {
                    // Add empty state message
                    addEmptyHistoryMessage(historyContainer)
                    return
                }

                // Convert to history items and sort by timestamp (newest first)
                for (historySnapshot in snapshot.children) {
                    val sensorData = historySnapshot.child("sensor").getValue(String::class.java) ?: ""
                    val timestamp = historySnapshot.child("timestamp").getValue(String::class.java) ?: ""
                    
                    historyItems.add(SensorHistoryData(sensorData, timestamp))
                }
                
                // Sort by timestamp (newest first)
                historyItems.sortByDescending { parseTimestamp(it.timestamp) }
                
                // Limit to 15 items for both chart and list display
                val limitedItems = if (historyItems.size > 15) {
                    historyItems.take(15)
                } else {
                    historyItems
                }
                
                // Add history items to container
                for (history in limitedItems) {
                    addHistoryItem(historyContainer, history)
                }
                
                // Update the chart with new data
                updateChart()
            }
            
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@DetailKumbungActivity, 
                    "Gagal memuat riwayat: ${error.message}", 
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
    
    private fun addHistoryItem(container: LinearLayout, history: SensorHistoryData) {
        val inflater = LayoutInflater.from(this)
        val historyView = inflater.inflate(R.layout.item_history_sensor, container, false)
        
        val messageTextView = historyView.findViewById<TextView>(R.id.tv_notification_message)
        val timestampTextView = historyView.findViewById<TextView>(R.id.tv_notification_timestamp)
        
        messageTextView.text = history.sensor
        timestampTextView.text = formatDateTime(history.timestamp)
        
        // Add alternating background colors for better readability
        if (container.childCount % 2 == 0) {
            historyView.setBackgroundResource(R.color.light_blue_card)
        } else {
            historyView.setBackgroundResource(R.color.white)
        }
        
        container.addView(historyView)
    }
    
    private fun addEmptyHistoryMessage(container: LinearLayout) {
        val emptyView = TextView(this)
        emptyView.text = "Belum ada riwayat sensor"
        emptyView.textSize = 14f
        emptyView.setPadding(32, 32, 32, 32)
        emptyView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        container.addView(emptyView)
    }
    
    private fun parseTimestamp(timestamp: String): Date {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            dateFormat.parse(timestamp) ?: Date(0)
        } catch (e: Exception) {
            Date(0)
        }
    }
    
    private fun formatDateTime(dateTimeStr: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("HH:mm dd-MM-yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateTimeStr) ?: return dateTimeStr
            return outputFormat.format(date)
        } catch (e: Exception) {
            return dateTimeStr
        }
    }
    
    private fun showEditNameDialog() {
        val input = android.widget.EditText(this)
        input.setText(binding.tvKumbungName.text)
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Edit Nama Kumbung")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    dbRef.child("name").setValue(newName)
                        .addOnSuccessListener {
                            binding.tvKumbungName.text = newName
                            binding.tvDetailTitle.text = "Detail $newName"
                            Toast.makeText(this, "Nama kumbung diperbarui", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Gagal memperbarui nama", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    
    private fun showEditMediaDialog() {
        val input = android.widget.EditText(this)
        input.setText(binding.tvMediaTanam.text.toString().replace(" Pcs", ""))
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Edit Media Tanam")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val mediaCountStr = input.text.toString().trim()
                if (mediaCountStr.isNotEmpty()) {
                    val mediaCount = mediaCountStr.toIntOrNull() ?: 0
                    
                    // Update Firebase with new media count
                    dbRef.child("mediaCount").setValue(mediaCount)
                        .addOnSuccessListener {
                            // Update UI
                            binding.tvMediaTanam.text = "$mediaCount Pcs"
                            Toast.makeText(this, "Media tanam berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal memperbarui media tanam: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    
    private fun saveChanges() {
        Toast.makeText(this, "Perubahan disimpan", Toast.LENGTH_SHORT).show()
        // Implement actual save logic if needed
    }
    
    private fun deleteKumbung() {
        dbRef.child("register").setValue(false)
            .addOnSuccessListener {
                Toast.makeText(this, "Kumbung dihapus", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menghapus kumbung", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun setupChart() {
        // Basic chart setup
        with(lineChart) {
            description.isEnabled = false  // Hide description text
            legend.isEnabled = true        // Show legend
            setTouchEnabled(true)
            setDrawGridBackground(false)
            
            // Enable scaling and dragging
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            
            // X-axis settings
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.labelRotationAngle = -45f
            
            // Left Y-axis settings (for values)
            axisLeft.setDrawGridLines(true)
            
            // Right Y-axis settings (hide)
            axisRight.isEnabled = false
            
            // Animation
            animateX(1000)
        }
    }
    
    private fun updateTabsUI() {
        if (showingTemperatureChart) {
            binding.tabTemperature.setBackgroundResource(R.color.blue_primary)
            binding.tabHumidity.setBackgroundResource(R.color.light_blue)
        } else {
            binding.tabTemperature.setBackgroundResource(R.color.light_blue)
            binding.tabHumidity.setBackgroundResource(R.color.blue_primary)
        }
    }

    private fun updateChart() {
        if (historyItems.isEmpty()) return
        
        // Create entries for the chart
        val entries = ArrayList<Entry>()
        
        // Get reversed items to show oldest first on X-axis
        val reversedItems = historyItems.reversed()
        
        // Add data points - we're taking up to 15 items
        for (i in reversedItems.indices) {
            val value = if (showingTemperatureChart) {
                reversedItems[i].temperature
            } else {
                reversedItems[i].humidity
            }
            entries.add(Entry(i.toFloat(), value))
        }
        
        // Create dataset
        val dataSet = LineDataSet(entries, if (showingTemperatureChart) "Suhu (°C)" else "Kelembapan (%)")
        
        // Style the dataset
        dataSet.color = if (showingTemperatureChart) 
            ContextCompat.getColor(this, R.color.temperature_color) 
        else 
            ContextCompat.getColor(this, R.color.humidity_color)
        dataSet.setCircleColor(dataSet.color)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(false)
        dataSet.valueTextSize = 10f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = dataSet.color
        dataSet.fillAlpha = 50
        
        // Create line data and set to chart
        val lineData = LineData(dataSet)
        lineChart.data = lineData
        
        // Set X-axis labels to timestamps
        lineChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                if (index >= 0 && index < reversedItems.size) {
                    // Return short time format
                    val timestamp = reversedItems[index].timestamp
                    return formatShortTime(timestamp)
                }
                return ""
            }
        }
        
        // Refresh chart
        lineChart.invalidate()
    }

    private fun formatShortTime(timestamp: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = inputFormat.parse(timestamp) ?: return ""
            return outputFormat.format(date)
        } catch (e: Exception) {
            return ""
        }
    }
}