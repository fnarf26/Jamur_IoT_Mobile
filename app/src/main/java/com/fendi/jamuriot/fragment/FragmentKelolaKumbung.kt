package com.fendi.jamuriot.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fendi.jamuriot.DetailKumbungActivity
import com.fendi.jamuriot.MainActivity
import com.fendi.jamuriot.TambahPetugasActivity
import com.fendi.jamuriot.adapter.KumbungAdapter
import com.fendi.jamuriot.databinding.FragmentKelolaKumbungBinding
import com.fendi.jamuriot.model.KumbungData
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
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

        // Get user role from SharedPreferences
        val prefs = thisParent.getSharedPreferences("user", Context.MODE_PRIVATE)
        val userRole = prefs.getString("role", "petugas") ?: "petugas"

        // Control IMEI input section visibility based on role
        if (userRole == "admin") {
            // Show IMEI input section for admin
            b.inputSection.visibility = View.VISIBLE
        } else {
            // Hide IMEI input section for non-admin roles (like petugas)
            b.inputSection.visibility = View.GONE

            // Adjust the top constraint of the data header to connect directly to the divider
            // when input section is gone
            val params = b.tvDataHeader.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = 0
            b.tvDataHeader.layoutParams = params

            // Update constraint connections
            val constraintSet = ConstraintSet()
            constraintSet.clone(b.root as ConstraintLayout)
            constraintSet.connect(
                b.tvDataHeader.id,
                ConstraintSet.TOP,
                b.divider.id,
                ConstraintSet.BOTTOM,
                0
            )
            constraintSet.applyTo(b.root as ConstraintLayout)
        }

        // Setup button click listeners only for admin
        if (userRole == "admin") {
            b.btnCheck.setOnClickListener {
                val imei = b.etImei.text.toString().trim()
                if (imei.isNotEmpty()) {
                    dbRef.child(imei).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val isRegistered =
                                    snapshot.child("register").getValue(Boolean::class.java)
                                        ?: false
                                if (isRegistered) {
                                    Toast.makeText(
                                        thisParent,
                                        "Perangkat sudah terdaftar",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    showRegisterDialog(imei)
                                }
                            } else {
                                Toast.makeText(
                                    thisParent,
                                    "Perangkat tidak ditemukan",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(
                                thisParent,
                                "Terjadi kesalahan: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                } else {
                    Toast.makeText(thisParent, "IMEI tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Load registered devices for all roles
        loadRegisteredDevices()

        return b.root
    }


    private fun loadRegisteredDevices() {
        // Get user info from SharedPreferences
        val prefs = thisParent.getSharedPreferences("user", Context.MODE_PRIVATE)
        val userRole = prefs.getString("role", "petugas") ?: "petugas"
        val email = prefs.getString("email", "") ?: ""
        val isAdmin = userRole == "admin"

        Log.d("KelolaKumbung", "Loading devices for role: $userRole, email: $email")

        // Clear previous list
        kumbungList.clear()

        if (isAdmin) {
            // Admin can see all registered devices
            loadAllDevices(isAdmin)
        } else {
            // Petugas can only see assigned devices
            loadPetugasDevices(email, isAdmin)
        }
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
                        Toast.makeText(thisParent, "Berhasil didaftarkan", Toast.LENGTH_SHORT)
                            .show()
                        loadRegisteredDevices() // Refresh data
                    }.addOnFailureListener {
                        Toast.makeText(thisParent, "Gagal mendaftar perangkat", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(thisParent, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }


    private fun loadAllDevices(isAdmin: Boolean) {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Create a new list to hold devices
                val newKumbungList = ArrayList<KumbungData>()

                // Map to hold device IDs for the adapter
                val deviceIdMap = mutableMapOf<String, String>()

                // Populate the new list
                for (deviceSnapshot in snapshot.children) {
                    val deviceId = deviceSnapshot.key ?: continue
                    val register =
                        deviceSnapshot.child("register").getValue(Boolean::class.java) ?: false

                    if (register) {
                        val name = deviceSnapshot.child("name").getValue(String::class.java) ?: "-"
                        val lastUpdateSnapshot =
                            deviceSnapshot.child("lastUpdate").getValue(String::class.java) ?: "-"
                        val tempAvg =
                            deviceSnapshot.child("temperatureAverage").getValue(Double::class.java)
                                ?: 0.0
                        val humAvg =
                            deviceSnapshot.child("humidityAverage").getValue(Double::class.java)
                                ?: 0.0

                        newKumbungList.add(KumbungData(deviceId,name, lastUpdateSnapshot, tempAvg, humAvg))
                        deviceIdMap[name] = deviceId
                        Log.d("KelolaKumbung", "Added device: $name ($deviceId)")
                    }
                }

                // Check if data was loaded
                if (newKumbungList.isEmpty()) {
                    Toast.makeText(thisParent, "Tidak ada kumbung terdaftar", Toast.LENGTH_SHORT)
                        .show()
                }

                // Update the main list
                kumbungList.clear()
                kumbungList.addAll(newKumbungList)

                // Create and configure adapter
                setupKumbungAdapter(deviceIdMap, isAdmin)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("KelolaKumbung", "Database error: ${error.message}")
                Toast.makeText(
                    thisParent,
                    "Gagal memuat data: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                setupKumbungAdapter(emptyMap(), isAdmin)
            }
        })
    }

    private fun loadPetugasDevices(email: String, isAdmin: Boolean) {
        // Get petugas assigned kumbungs from Firestore
        FirebaseFirestore.getInstance().collection("users").document(email).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Get the assigned kumbung IDs
                    @Suppress("UNCHECKED_CAST")
                    val assignedKumbungs = document.get("kumbung") as? List<String> ?: emptyList()

                    Log.d(
                        "KelolaKumbung",
                        "Found ${assignedKumbungs.size} assigned kumbungs: $assignedKumbungs"
                    )

                    if (assignedKumbungs.isEmpty()) {
                        // No kumbungs assigned
                        Toast.makeText(
                            thisParent,
                            "Tidak ada kumbung yang ditugaskan",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Create empty adapter
                        setupKumbungAdapter(emptyMap(), isAdmin)
                        return@addOnSuccessListener
                    }

                    // Filter devices by assigned kumbungs
                    loadFilteredDevices(assignedKumbungs, isAdmin)
                } else {
                    // User document not found
                    Toast.makeText(thisParent, "Data petugas tidak ditemukan", Toast.LENGTH_SHORT)
                        .show()
                    setupKumbungAdapter(emptyMap(), isAdmin)
                }
            }
            .addOnFailureListener { e ->
                Log.e("KelolaKumbung", "Error loading user data: ", e)
                Toast.makeText(
                    thisParent,
                    "Gagal memuat data petugas: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                setupKumbungAdapter(emptyMap(), isAdmin)
            }
    }

    private fun navigateToDetailKumbung(deviceId: String) {
        val intent = Intent(thisParent, DetailKumbungActivity::class.java).apply {
            putExtra("DEVICE_ID", deviceId)
        }

        startActivity(intent)
    }

    private fun loadFilteredDevices(assignedKumbungs: List<String>, isAdmin: Boolean) {
        // Convert to a Set for faster lookups
        val assignedKumbungSet = assignedKumbungs.map { it.trim() }.toSet()

        Log.d("KelolaKumbung", "Filtering for device IDs: $assignedKumbungSet")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Create a new list to hold devices
                val newKumbungList = ArrayList<KumbungData>()

                // Map to hold device IDs for the adapter
                val deviceIdMap = mutableMapOf<String, String>()

                // Check each device in the database
                for (deviceSnapshot in snapshot.children) {
                    val deviceId = deviceSnapshot.key ?: continue

                    // Debug log to see what we're checking
                    Log.d("KelolaKumbung", "Checking device: $deviceId, assigned: ${deviceId in assignedKumbungSet}")

                    // Only process this device if it's in the assigned list
                    if (deviceId in assignedKumbungSet) {
                        val register = deviceSnapshot.child("register").getValue(Boolean::class.java) ?: false

                        // Only include devices that are registered
                        if (register) {
                            val name = deviceSnapshot.child("name").getValue(String::class.java) ?: "-"
                            val lastUpdateSnapshot = deviceSnapshot.child("lastUpdate").getValue(String::class.java) ?: "-"
                            val tempAvg = deviceSnapshot.child("temperatureAverage").getValue(Double::class.java) ?: 0.0
                            val humAvg = deviceSnapshot.child("humidityAverage").getValue(Double::class.java) ?: 0.0

                            newKumbungList.add(KumbungData(deviceId, name, lastUpdateSnapshot, tempAvg, humAvg))
                            deviceIdMap[name] = deviceId
                            Log.d("KelolaKumbung", "✅ Added assigned device: $name ($deviceId)")
                        } else {
                            Log.d("KelolaKumbung", "⚠️ Device not registered: $deviceId")
                        }
                    } else {
                        Log.d("KelolaKumbung", "❌ Skipped unassigned device: $deviceId")
                    }
                }

                // Check if data was loaded
                if (newKumbungList.isEmpty()) {
                    Toast.makeText(thisParent, "Tidak ada kumbung terdaftar untuk Anda", Toast.LENGTH_SHORT).show()
                }

                // Update the main list
                kumbungList.clear()
                kumbungList.addAll(newKumbungList)

                // Create and configure adapter
                setupKumbungAdapter(deviceIdMap, isAdmin)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("KelolaKumbung", "Database error: ${error.message}")
                Toast.makeText(thisParent, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
                setupKumbungAdapter(emptyMap(), isAdmin)
            }
        })
    }

    private fun setupKumbungAdapter(deviceIdMap: Map<String, String>, isAdmin: Boolean) {
        // Create adapter with the populated list
        val adapter = KumbungAdapter(
            kumbungList,
            { deletedData ->
                // Delete handler
                dbRef.orderByChild("name").equalTo(deletedData.name)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (item in snapshot.children) {
                                item.ref.removeValue()
                            }
                            Toast.makeText(thisParent, "${deletedData.name} dihapus", Toast.LENGTH_SHORT).show()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(thisParent, "Gagal menghapus: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            },
            { deviceId ->
                // Click handler
                navigateToDetailKumbung(deviceId)
            },
            isAdmin,
            thisParent
        )

        // Set device IDs in the adapter from the map
        for ((name, deviceId) in deviceIdMap) {
            adapter.setDeviceId(name, deviceId)
        }

        b.rvKumbung.layoutManager = LinearLayoutManager(thisParent)
        b.rvKumbung.adapter = adapter

        // Only setup swipe-to-delete if user is admin
        if (isAdmin) {
            setupSwipeToDelete(adapter)
        }
    }

    private fun setupSwipeToDelete(adapter: KumbungAdapter) {
        val itemTouchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.getItemAt(position)

                if (item != null) {
                    // Show confirmation dialog
                    AlertDialog.Builder(thisParent).apply {
                        setTitle("Konfirmasi Hapus")
                        setMessage("Apakah Anda yakin ingin menghapus kumbung \"${item.name}\"?")
                        setPositiveButton("Hapus") { _, _ ->
                            adapter.deleteItem(position)
                        }
                        setNegativeButton("Batal") { dialog, _ ->
                            dialog.dismiss()
                            adapter.notifyItemChanged(position) // Reset position
                        }
                        setCancelable(false)
                        show()
                    }
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(b.rvKumbung)
    }
}
                           

