package com.fendi.jamuriot

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.fendi.jamuriot.databinding.ActivityPengaturanThresholdBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PengaturanThresholdActivity : AppCompatActivity() {

    private lateinit var b: ActivityPengaturanThresholdBinding
    private lateinit var database: FirebaseDatabase
    
    // Default values if no data exists in the database
    private val defaultTempThreshold = 35
    private val defaultHumidityThreshold = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        b = ActivityPengaturanThresholdBinding.inflate(layoutInflater)
        setContentView(b.root)
        
        // Initialize Firebase Realtime Database
        database = FirebaseDatabase.getInstance()
        
        // Back button handler
        b.btnBack.setOnClickListener {
            finish()
        }
        
        // Load current threshold values
        loadThresholdValues()
        
        // Set up save button click listener
        b.btnAtur.setOnClickListener {
            updateThresholdValues()
        }
    }
    
    private fun loadThresholdValues() {
        val settingsRef = database.getReference("settings")
        
        settingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempThreshold = snapshot.child("batasSuhu").getValue(Int::class.java) ?: defaultTempThreshold
                val humidityThreshold = snapshot.child("batasKelembapan").getValue(Int::class.java) ?: defaultHumidityThreshold
                
                // Update UI with current values
                b.tvCurrentTemp.text = "$tempThreshold°C"
                b.tvCurrentHumidity.text = "$humidityThreshold %"
                
                // Set hint text for input fields with current values
                b.etTemperature.hint = "Masukkan batas suhu (saat ini: $tempThreshold°C)"
                b.etHumidity.hint = "Masukkan batas kelembapan (saat ini: $humidityThreshold%)"
            }
            
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@PengaturanThresholdActivity,
                    "Gagal memuat data: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
    
    private fun updateThresholdValues() {
        val tempInput = b.etTemperature.text.toString().trim()
        val humidityInput = b.etHumidity.text.toString().trim()
        
        // Validate inputs
        if (tempInput.isEmpty() && humidityInput.isEmpty()) {
            Toast.makeText(this, "Masukkan nilai batas suhu atau kelembapan", Toast.LENGTH_SHORT).show()
            return
        }
        
        val settingsRef = database.getReference("settings")
        
        // Get current values first
        settingsRef.get().addOnSuccessListener { snapshot ->
            val currentTemp = snapshot.child("batasSuhu").getValue(Int::class.java) ?: defaultTempThreshold
            val currentHumidity = snapshot.child("batasKelembapan").getValue(Int::class.java) ?: defaultHumidityThreshold
            
            // Update only if values were entered
            val updates = HashMap<String, Any>()
            
            if (tempInput.isNotEmpty()) {
                try {
                    val newTemp = tempInput.toInt()
                    // Basic validation
                    if (newTemp < 10 || newTemp > 50) {
                        Toast.makeText(this, "Batas suhu harus antara 10-50°C", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    updates["batasSuhu"] = newTemp
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Masukkan nilai suhu yang valid", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
            }
            
            if (humidityInput.isNotEmpty()) {
                try {
                    val newHumidity = humidityInput.toInt()
                    // Basic validation
                    if (newHumidity < 0 || newHumidity > 100) {
                        Toast.makeText(this, "Batas kelembapan harus antara 0-100%", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    updates["batasKelembapan"] = newHumidity
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Masukkan nilai kelembapan yang valid", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
            }
            
            // If there are updates to make
            if (updates.isNotEmpty()) {
                settingsRef.updateChildren(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Pengaturan threshold berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        // Clear input fields
                        b.etTemperature.text.clear()
                        b.etHumidity.text.clear()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal memperbarui: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Gagal mengakses database: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}