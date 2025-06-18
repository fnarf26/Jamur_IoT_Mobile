package com.fendi.jamuriot

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.fendi.jamuriot.databinding.ActivityRiwayatNontifikasiBinding
import com.fendi.jamuriot.model.NotifikasiData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class RiwayatNotifikasiActivity : AppCompatActivity() {

    private lateinit var b: ActivityRiwayatNontifikasiBinding
    private lateinit var notifikasiAdapter: NotifikasiAdapter
    private val notifikasiList = mutableListOf<NotifikasiData>()
    private var sortByTimestamp = false
    private val TAG = "RiwayatNotifikasi"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        b = ActivityRiwayatNontifikasiBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Initialize RecyclerView and adapter
        setupRecyclerView()

        // Set up back button
        b.btnBack.setOnClickListener {
            finish()
        }

        // Set up tab navigation
        setupTabs()

        // Load notifications based on user role
        loadNotifications()
    }

    private fun setupRecyclerView() {
        notifikasiAdapter = NotifikasiAdapter(notifikasiList)
        b.recyclerViewNotifikasi.apply {
            layoutManager = LinearLayoutManager(this@RiwayatNotifikasiActivity)
            adapter = notifikasiAdapter
        }
    }

    private fun setupTabs() {
        // Default tab selection
        b.tabKeterangan.setBackgroundResource(R.color.blue_primary)
        b.tabWaktu.setBackgroundResource(R.color.light_blue)

        b.tabKeterangan.setOnClickListener {
            if (sortByTimestamp) {
                sortByTimestamp = false
                sortAndDisplayNotifications()

                b.tabKeterangan.setBackgroundResource(R.color.blue_primary)
                b.tabWaktu.setBackgroundResource(R.color.light_blue)
            }
        }

        b.tabWaktu.setOnClickListener {
            if (!sortByTimestamp) {
                sortByTimestamp = true
                sortAndDisplayNotifications()

                b.tabKeterangan.setBackgroundResource(R.color.light_blue)
                b.tabWaktu.setBackgroundResource(R.color.blue_primary)
            }
        }
    }

    private fun loadNotifications() {
        // Show loading indicator
        b.progressBar.visibility = View.VISIBLE

        // Get user role from SharedPreferences
        val sharedPrefs = getSharedPreferences("user", Context.MODE_PRIVATE)
        val userRole = sharedPrefs.getString("role", "") ?: ""
        val userEmail = sharedPrefs.getString("email", "") ?: ""

        Log.d(TAG, "Loading notifications for user role: $userRole")

        if (userRole == "petugas") {
            // For petugas, first get assigned kumbungs, then load notifications
            loadAssignedKumbungs(userEmail)
        } else {
            // For admin or other roles, load all notifications
            loadAllNotifications()
        }
    }

    private fun loadAssignedKumbungs(userEmail: String) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("users").document(userEmail).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val assignedKumbungs = document.get("kumbung") as? List<String> ?: emptyList()

                    Log.d(TAG, "Assigned kumbungs: $assignedKumbungs")

                    if (assignedKumbungs.isEmpty()) {
                        showEmptyState("Tidak ada kumbung yang ditugaskan")
                        return@addOnSuccessListener
                    }

                    // Load notifications for assigned kumbungs
                    loadNotificationsForDevices(assignedKumbungs)
                } else {
                    showEmptyState("Data petugas tidak ditemukan")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading user data: ${e.message}", e)
                showEmptyState("Gagal memuat data petugas")
            }
    }

    private fun loadAllNotifications() {
        val notificationsRef = FirebaseDatabase.getInstance().getReference("notifications")

        notificationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notifikasiList.clear()

                if (!snapshot.exists()) {
                    showEmptyState("Tidak ada notifikasi")
                    return
                }

                var totalNotifications = 0

                // First level: device IDs
                for (deviceSnapshot in snapshot.children) {
                    val deviceId = deviceSnapshot.key ?: continue

                    // Second level: notification entries
                    for (notificationSnapshot in deviceSnapshot.children) {
                        try {
                            val notificationId = notificationSnapshot.key ?: continue
                            val message = notificationSnapshot.child("message").getValue(String::class.java) ?: ""
                            val timestamp = notificationSnapshot.child("timestamp").getValue(String::class.java) ?: ""

                            notifikasiList.add(
                                NotifikasiData(
                                    id = notificationId,
                                    message = message,
                                    timestamp = timestamp,
                                    deviceId = deviceId
                                )
                            )

                            totalNotifications++
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing notification: ${e.message}")
                        }
                    }
                }

                if (notifikasiList.isEmpty()) {
                    showEmptyState("Tidak ada notifikasi")
                } else {
                    Log.d(TAG, "Loaded $totalNotifications notifications")
                    sortAndDisplayNotifications()
                }

                b.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
                showEmptyState("Gagal memuat notifikasi")
            }
        })
    }

    private fun loadNotificationsForDevices(deviceIds: List<String>) {
        val notificationsRef = FirebaseDatabase.getInstance().getReference("notifications")

        notificationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notifikasiList.clear()

                if (!snapshot.exists()) {
                    showEmptyState("Tidak ada notifikasi")
                    return
                }

                var totalNotifications = 0

                // First level: device IDs
                for (deviceSnapshot in snapshot.children) {
                    val deviceId = deviceSnapshot.key ?: continue

                    // Skip if device is not in assigned list
                    if (!deviceIds.contains(deviceId)) continue

                    // Second level: notification entries
                    for (notificationSnapshot in deviceSnapshot.children) {
                        try {
                            val notificationId = notificationSnapshot.key ?: continue
                            val message = notificationSnapshot.child("message").getValue(String::class.java) ?: ""
                            val timestamp = notificationSnapshot.child("timestamp").getValue(String::class.java) ?: ""

                            notifikasiList.add(
                                NotifikasiData(
                                    id = notificationId,
                                    message = message,
                                    timestamp = timestamp,
                                    deviceId = deviceId
                                )
                            )

                            totalNotifications++
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing notification: ${e.message}")
                        }
                    }
                }

                if (notifikasiList.isEmpty()) {
                    showEmptyState("Tidak ada notifikasi untuk kumbung yang ditugaskan")
                } else {
                    Log.d(TAG, "Loaded $totalNotifications notifications for assigned devices")
                    sortAndDisplayNotifications()
                }

                b.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
                showEmptyState("Gagal memuat notifikasi")
            }
        })
    }

    private fun sortAndDisplayNotifications() {
        // Sort based on selected tab
        if (sortByTimestamp) {
            notifikasiList.sortByDescending { parseTimestamp(it.timestamp) }
        } else {
            // Sort by message (keterangan)
            notifikasiList.sortByDescending { parseTimestamp(it.timestamp) }
        }

        notifikasiAdapter.notifyDataSetChanged()

        // Show/hide empty state
        if (notifikasiList.isEmpty()) {
            showEmptyState("Tidak ada notifikasi")
        } else {
            b.emptyStateLayout.visibility = View.GONE
            b.recyclerViewNotifikasi.visibility = View.VISIBLE
        }
    }

    private fun parseTimestamp(timestamp: String): Date {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            dateFormat.parse(timestamp) ?: Date(0)
        } catch (e: Exception) {
            Date(0)
        }
    }

    private fun showEmptyState(message: String) {
        b.progressBar.visibility = View.GONE
        b.emptyStateLayout.visibility = View.VISIBLE
        b.emptyStateText.text = message
        b.recyclerViewNotifikasi.visibility = View.GONE
    }
}