package com.fendi.jamuriot.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.fendi.jamuriot.MainActivity
import com.fendi.jamuriot.adapter.KumbungAdapter
import com.fendi.jamuriot.databinding.FragmentKelolaKumbungBinding
import com.fendi.jamuriot.model.KumbungData
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FragmentKelolaKumbung : Fragment() {
    private lateinit var b: FragmentKelolaKumbungBinding
    private lateinit var thisParent: MainActivity
    private lateinit var dbRef: DatabaseReference
    private val kumbungList = ArrayList<KumbungData>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        thisParent = activity as MainActivity
        b = FragmentKelolaKumbungBinding.inflate(inflater, container, false)

        dbRef = FirebaseDatabase.getInstance().getReference("devices")

        b.btnCheck.setOnClickListener {
            val imei = b.etImei.text.toString().trim()
            if (imei.isNotEmpty()) {
                dbRef.child(imei).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            showRegisterDialog(imei)
                        } else {
                            Toast.makeText(thisParent, "Perangkat tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(thisParent, "Terjadi kesalahan: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(thisParent, "IMEI tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        loadRegisteredDevices()

        return b.root
    }

    private fun showRegisterDialog(imei: String) {
        val input = EditText(thisParent)
        input.hint = "Nama perangkat"

        AlertDialog.Builder(thisParent)
            .setTitle("Daftarkan Perangkat")
            .setMessage("Masukkan nama untuk perangkat dengan IMEI: $imei")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val updates = mapOf(
                        "register" to true,
                        "name" to name
                    )
                    dbRef.child(imei).updateChildren(updates).addOnSuccessListener {
                        Toast.makeText(thisParent, "Berhasil didaftarkan", Toast.LENGTH_SHORT).show()
                        loadRegisteredDevices() // Refresh data
                    }.addOnFailureListener {
                        Toast.makeText(thisParent, "Gagal mendaftar perangkat", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(thisParent, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun loadRegisteredDevices() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                kumbungList.clear()
                for (deviceSnapshot in snapshot.children) {
                    val register = deviceSnapshot.child("register").getValue(Boolean::class.java) ?: false
                    if (register) {
                        val name = deviceSnapshot.child("name").getValue(String::class.java) ?: "-"
                        val lastUpdateSnapshot = deviceSnapshot.child("lastUpdate").getValue(String::class.java) ?: "-"
//                        val lastUpdate = when (val value = lastUpdateSnapshot.value) {
//                            is Long -> value
//                            is String -> value.toLongOrNull() ?: 0L
//                            else -> 0L
//                        }
                        val tempAvg = deviceSnapshot.child("temperatureAverage").getValue(Double::class.java) ?: 0.0
                        val humAvg = deviceSnapshot.child("humidityAverage").getValue(Double::class.java) ?: 0.0

                        kumbungList.add(KumbungData(name, lastUpdateSnapshot, tempAvg, humAvg))
                    }
                }

                b.rvKumbung.layoutManager = LinearLayoutManager(thisParent)
                b.rvKumbung.adapter = KumbungAdapter(kumbungList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(thisParent, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun convertToDate(dateString: String): Date? {
        val format = SimpleDateFormat("d/M/yyyy HH.mm.ss", Locale.getDefault())
        return try {
            format.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
}
