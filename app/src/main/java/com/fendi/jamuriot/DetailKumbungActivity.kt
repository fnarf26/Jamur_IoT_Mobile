package com.fendi.jamuriot

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.fendi.jamuriot.databinding.ActivityDetailKumbungBinding
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class DetailKumbungActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailKumbungBinding
    private lateinit var dbRef: DatabaseReference
    private lateinit var settingsRef: DatabaseReference
    private var deviceId: String = ""
    
    // Default threshold values (used if settings not available)
    private var batasSuhu: Int = 33
    private var batasKelembapan: Int = 50

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

        // Control IMEI input section visibility based on role
        if (userRole == "admin") {
            binding.btnEditName.visibility = View.GONE
            binding.btnEditMedia.visibility = View.GONE
            binding.btnPanen.visibility = View.GONE
        }
        // Load settings first
        loadSettings()
        
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
                
                // Update panen status text (this would be more dynamic in a real app)
                binding.tvPanenStatus.text = "BERHASIL PANEN $harvestCount KALI"
                
                // Update media tanam with actual media count
                binding.tvMediaTanam.text = "$mediaCount Pcs"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DetailKumbungActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
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
}