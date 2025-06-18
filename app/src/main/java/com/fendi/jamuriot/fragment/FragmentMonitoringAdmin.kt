package com.fendi.jamuriot.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.fendi.jamuriot.DetailKumbungActivity
import com.fendi.jamuriot.LoginActivity
import com.fendi.jamuriot.MainActivity
import com.fendi.jamuriot.PengaturanThresholdActivity
import com.fendi.jamuriot.R
import com.fendi.jamuriot.RiwayatNotifikasiActivity
import com.fendi.jamuriot.adapter.KumbungMonitoringAdapter
import com.fendi.jamuriot.databinding.FragmentMonitoringadminBinding
import com.fendi.jamuriot.model.KumbungData
import com.fendi.jamuriot.model.NotifikasiData
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class FragmentMonitoringAdmin : Fragment() {
    private lateinit var b: FragmentMonitoringadminBinding
    private lateinit var thisParent: MainActivity
    private lateinit var dbRef: DatabaseReference
    private lateinit var notificationsRef: DatabaseReference
    private lateinit var kumbungAdapter: KumbungMonitoringAdapter
    private var currentKumbungPosition = 0

    // Add this property to track the current device ID
    private var currentDeviceId: String? = null

    // Add this as a class property, near the top with other properties
    private var userSelectedPosition = 0
    private var isUserSwipe = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize binding and view
        thisParent = activity as MainActivity
        b = FragmentMonitoringadminBinding.inflate(inflater, container, false)

        dbRef = FirebaseDatabase.getInstance().getReference("devices")
        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications")

        // Get shared preferences
        val prefs = thisParent.getSharedPreferences("user", Context.MODE_PRIVATE)
        val nama = prefs.getString("nama", "N/A")
        val role = prefs.getString("role", "N/A")

        b.tvGreeting.text = "Halo, ${nama}"

        // Control spray section visibility based on role
        if (role == "petugas") {
            // Show spray control section for petugas only
            b.tvControlSpray.visibility = View.VISIBLE
            b.cardSprayControl.visibility = View.VISIBLE
        } else {
            // Hide spray control section for other roles
            b.tvControlSpray.visibility = View.GONE
            b.cardSprayControl.visibility = View.GONE
        }

        b.btnNotification.setOnClickListener {
            val intent = Intent(thisParent, RiwayatNotifikasiActivity::class.java).apply{}
            startActivity(intent)
        }

        // Set up ViewPager for kumbung cards
        setupViewPager()

        // Only set up spray control if the user is a petugas
        if (role == "petugas") {
            setupSprayControl()
        }

        // Load kumbung data
        loadRegisteredDevices()

        return b.root
    }

    private fun setupViewPager() {
        kumbungAdapter = KumbungMonitoringAdapter(
            onDetailClickListener = { deviceId ->
                navigateToDetailKumbung(deviceId)
            }
        )

        b.viewPagerKumbung.adapter = kumbungAdapter

        // Connect TabLayout (indicator) with ViewPager2
        TabLayoutMediator(b.tabIndicator, b.viewPagerKumbung) { _, _ ->
            // No text for the tabs
        }.attach()

        // Add page change listener to update spray control, info, and notifications
        b.viewPagerKumbung.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentKumbungPosition = position

                // Mark this as a user-initiated position change
                userSelectedPosition = position
                isUserSwipe = true

                // Get current device ID
                currentDeviceId = kumbungAdapter.getDeviceId(position)

                // Update spray control and info
                updateSprayControlAndInfo(position)

                // Update notifications for this device
                currentDeviceId?.let { deviceId ->
                    loadNotificationsForDevice(deviceId)
                }
            }
        })
    }

    // Updated setupSprayControl to handle potential errors
    private fun setupSprayControl() {
        try {
            // Add switch state change listener
            b.switchSpray.setOnCheckedChangeListener { _, isChecked ->
                val deviceId = kumbungAdapter.getDeviceId(currentKumbungPosition)
                if (deviceId == null) {
                    Toast.makeText(thisParent, "Tidak dapat menemukan ID perangkat", Toast.LENGTH_SHORT).show()
                    return@setOnCheckedChangeListener
                }

                // Disable the switch while updating
                b.switchSpray.isEnabled = false

                // Update spray status in Firebase
                dbRef.child(deviceId).child("manualSpray").setValue(isChecked)
                    .addOnSuccessListener {
                        updateSprayStatusUI(isChecked)

                        // Re-enable the switch
                        b.switchSpray.isEnabled = true
                    }
                    .addOnFailureListener {
                        // Revert the switch if failed
                        b.switchSpray.isChecked = !isChecked
                        Toast.makeText(thisParent, "Gagal memperbarui status spray", Toast.LENGTH_SHORT).show()
                        b.switchSpray.isEnabled = true
                    }
            }
        } catch (e: Exception) {
            Log.e("MonitoringAdmin", "Error setting up spray control: ${e.message}", e)
        }
    }

    private fun updateSprayStatusUI(isActive: Boolean) {
        b.tvSprayStatus.text = if (isActive) "AKTIF" else "TIDAK AKTIF"
        b.ivSprayStatus.setImageResource(if (isActive) R.drawable.ic_water_on else R.drawable.ic_water_off)
    }

    // Updated updateSprayControlAndInfo with more error handling
    private fun updateSprayControlAndInfo(position: Int) {
        try {
            val deviceId = kumbungAdapter.getDeviceId(position) ?: return

            // Get role from SharedPreferences
            val prefs = thisParent.getSharedPreferences("user", Context.MODE_PRIVATE)
            val role = prefs.getString("role", "N/A")

            dbRef.child(deviceId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        if (!snapshot.exists()) return

                        // Update spray status UI only for petugas
                        if (role == "petugas") {
                            val sprayStatus = snapshot.child("manualSpray").getValue(Boolean::class.java) ?: false
                            b.switchSpray.isChecked = sprayStatus
                            updateSprayStatusUI(sprayStatus)
                        }

                        // Update cultivation info for all roles
                        // Use harvestCount field instead of cultivationAge
                        val harvestCount = snapshot.child("harvestCount").getValue(Long::class.java) ?: 0
                        val mediaCount = snapshot.child("mediaCount").getValue(Long::class.java) ?: 0

                        // Update views by direct reference - without "Hari" suffix for harvest count
                        b.tvHarvest?.text = "$harvestCount Kali"
                        b.tvMedia?.text = "$mediaCount Pcs"

                    } catch (e: Exception) {
                        Log.e("MonitoringAdmin", "Error processing device data: ${e.message}", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MonitoringAdmin", "Database error: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("MonitoringAdmin", "Error updating spray control: ${e.message}", e)
        }
    }

    private fun loadRegisteredDevices() {
        // Get user info from SharedPreferences
        val prefs = thisParent.getSharedPreferences("user", Context.MODE_PRIVATE)
        val role = prefs.getString("role", "N/A") ?: "N/A"
        val email = prefs.getString("email", "") ?: ""
        
        Log.d("MonitoringAdmin", "📱 Loading devices for role: $role, email: $email")
        
        // Always clear previous data first
        kumbungAdapter.updateData(emptyList())
        kumbungAdapter.clearDeviceIdMappings()

        if (role == "petugas") {
            // Petugas role - only load assigned kumbungs from Firestore first
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users").document(email).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Get the assigned kumbung IDs
                        @Suppress("UNCHECKED_CAST")
                        val assignedKumbungs = document.get("kumbung") as? List<String> ?: emptyList()
                        Log.d("MonitoringAdmin", "🔑 Found ${assignedKumbungs.size} assigned kumbungs: $assignedKumbungs")
                        
                        // If no kumbungs assigned, show empty state
                        if (assignedKumbungs.isEmpty()) {
                            Toast.makeText(thisParent, "Tidak ada kumbung yang ditugaskan", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                        
                        // Load only assigned kumbungs from Firebase Realtime Database
                        loadFilteredDevices(assignedKumbungs)
                    } else {
                        Log.d("MonitoringAdmin", "⚠️ User document not found")
                        Toast.makeText(thisParent, "Data petugas tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MonitoringAdmin", "❌ Error loading user data: ", e)
                    Toast.makeText(thisParent, "Gagal memuat data petugas: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Admin role - load all registered devices
            Log.d("MonitoringAdmin", "👑 Loading all devices for admin")
            loadAllDevices()
        }
    }

    private fun loadFilteredDevices(assignedDeviceIds: List<String>) {
        // Remember current position before updating data
        val currentPosition = b.viewPagerKumbung.currentItem
        // Remember current device ID to try to restore position
        val currentDeviceId = kumbungAdapter.getDeviceId(currentPosition)
        
        Log.d("MonitoringAdmin", "Current position before update: $currentPosition, device: $currentDeviceId")
        
        // Convert to a Set for faster lookups
        val assignedDeviceIdSet = assignedDeviceIds.map { it.trim() }.toSet()
        
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newKumbungList = ArrayList<KumbungData>()
                val newDeviceIdToNameMap = mutableMapOf<String, String>()  // Track ID to name mapping
                val newNameToPositionMap = mutableMapOf<String, Int>()     // Track name to position mapping
                
                // First clear all previous device ID mappings in the adapter
                kumbungAdapter.clearDeviceIdMappings()
                
                // Check each device in the database
                for (deviceSnapshot in snapshot.children) {
                    val deviceId = deviceSnapshot.key ?: continue
                    
                    // Only process this device if it's in the assigned list
                    if (deviceId in assignedDeviceIdSet) {
                        val register = deviceSnapshot.child("register").getValue(Boolean::class.java) ?: false
                        
                        // Only include devices that are registered
                        if (register) {
                            val name = deviceSnapshot.child("name").getValue(String::class.java) ?: "-"
                            val lastUpdateSnapshot = deviceSnapshot.child("lastUpdate").getValue(String::class.java) ?: "-"
                            val tempAvg = deviceSnapshot.child("temperatureAverage").getValue(Double::class.java) ?: 0.0
                            val humAvg = deviceSnapshot.child("humidityAverage").getValue(Double::class.java) ?: 0.0

                            // Add to list and track position
                            newKumbungList.add(KumbungData(deviceId, name, lastUpdateSnapshot, tempAvg, humAvg))
                            val newPosition = newKumbungList.size - 1
                            newNameToPositionMap[name] = newPosition
                            newDeviceIdToNameMap[deviceId] = name
                            
                            // Add to adapter's mapping
                            kumbungAdapter.setDeviceId(name, deviceId)
                        }
                    }
                }

                if (newKumbungList.isEmpty()) {
                    Toast.makeText(thisParent, "Tidak ada kumbung terdaftar", Toast.LENGTH_SHORT).show()
                    kumbungAdapter.updateData(newKumbungList)
                    return
                }

                // Update adapter with the filtered list
                kumbungAdapter.updateData(newKumbungList)
                
                // Determine which position to restore to
                var positionToRestore = 0
                
                if (isUserSwipe && userSelectedPosition < newKumbungList.size) {
                    // User has manually swiped - prioritize their selection
                    positionToRestore = userSelectedPosition
                    Log.d("MonitoringAdmin", "Respecting user selection, position: $positionToRestore")
                } else if (currentDeviceId != null) {
                    // Try to find the same device by ID
                    val deviceName = newDeviceIdToNameMap[currentDeviceId]
                    if (deviceName != null && newNameToPositionMap.containsKey(deviceName)) {
                        positionToRestore = newNameToPositionMap[deviceName] ?: 0
                    } else if (currentPosition < newKumbungList.size) {
                        positionToRestore = currentPosition
                    }
                }
                
                // Only set position if actually changing or on first load
                if (b.viewPagerKumbung.currentItem != positionToRestore) {
                    b.viewPagerKumbung.setCurrentItem(positionToRestore, false)
                }
                
                updateCurrentSelection(newKumbungList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(thisParent, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadAllDevices() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newKumbungList = ArrayList<KumbungData>()

                for (deviceSnapshot in snapshot.children) {
                    val deviceId = deviceSnapshot.key ?: continue
                    val register = deviceSnapshot.child("register").getValue(Boolean::class.java) ?: false

                    if (register) {
                        val name = deviceSnapshot.child("name").getValue(String::class.java) ?: "-"
                        val lastUpdateSnapshot = deviceSnapshot.child("lastUpdate").getValue(String::class.java) ?: "-"
                        val tempAvg = deviceSnapshot.child("temperatureAverage").getValue(Double::class.java) ?: 0.0
                        val humAvg = deviceSnapshot.child("humidityAverage").getValue(Double::class.java) ?: 0.0

                        newKumbungList.add(KumbungData(deviceId, name, lastUpdateSnapshot, tempAvg, humAvg))
                        kumbungAdapter.setDeviceId(name, deviceId)
                    }
                }

                if (newKumbungList.isEmpty()) {
                    Toast.makeText(thisParent, "Tidak ada kumbung terdaftar", Toast.LENGTH_SHORT).show()
                }

                // Update adapter data
                kumbungAdapter.updateData(newKumbungList)
                
                // Maintain user's position if possible
                if (isUserSwipe && userSelectedPosition < newKumbungList.size) {
                    if (b.viewPagerKumbung.currentItem != userSelectedPosition) {
                        b.viewPagerKumbung.setCurrentItem(userSelectedPosition, false)
                    }
                }
                
                updateCurrentSelection(newKumbungList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(thisParent, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateCurrentSelection(kumbungList: List<KumbungData>) {
        // Update current kumbung info and load its notifications
        if (kumbungList.isNotEmpty() && b.viewPagerKumbung.currentItem < kumbungList.size) {
            val position = b.viewPagerKumbung.currentItem
            updateSprayControlAndInfo(position)

            // Set current device ID and load its notifications
            currentDeviceId = kumbungAdapter.getDeviceId(position)
            currentDeviceId?.let { deviceId ->
                loadNotificationsForDevice(deviceId)
            }
        }
    }

    private fun navigateToDetailKumbung(deviceId: String) {
        val intent = Intent(thisParent, DetailKumbungActivity::class.java).apply {
            putExtra("DEVICE_ID", deviceId)
        }
        startActivity(intent)
    }

    // Replace your existing loadNotifications() with this device-specific method
    private fun loadNotificationsForDevice(deviceId: String) {
        Log.d("MonitoringAdmin", "Loading notifications for device: $deviceId")

        // Get device name for better matching
        val deviceName = kumbungAdapter.getKumbungAt(currentKumbungPosition)?.name ?: ""

        notificationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    showEmptyNotifications()
                    return
                }

                val allNotifications = mutableListOf<NotifikasiData>()

                // First, iterate through all notification groups
                for (deviceGroupSnapshot in snapshot.children) {
                    Log.d("MonitoringAdmin", "Processing notifications for device group: ${deviceGroupSnapshot.key}")

                    // Process all notifications in this device group
                    for (notifSnapshot in deviceGroupSnapshot.children) {
                        try {
                            val message = notifSnapshot.child("message").getValue(String::class.java) ?: ""
                            val timestamp = notifSnapshot.child("timestamp").getValue(String::class.java) ?: ""

                            // Check if this notification is related to our device
                            if (message.contains(deviceId) ||
                                message.contains(deviceName) ||
                                deviceGroupSnapshot.key == deviceId) {

                                allNotifications.add(NotifikasiData(
                                    id = notifSnapshot.key ?: "",
                                    message = message,
                                    timestamp = timestamp
                                ))
                            }
                        } catch (e: Exception) {
                            Log.e("MonitoringAdmin", "Error processing notification: ${e.message}")
                        }
                    }
                }

                // Sort and limit notifications
                val sortedNotifications = allNotifications.sortedByDescending { it.timestamp }

                if (sortedNotifications.isEmpty()) {
                    showEmptyNotifications()
                    return
                }

                // Take at most 5 notifications
                updateNotificationsUI(sortedNotifications.take(5))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MonitoringAdmin", "Error loading notifications: ${error.message}")
                showEmptyNotifications()
            }
        })
    }

    private fun showEmptyNotifications() {
        val emptyList = listOf(NotifikasiData(
            id = "default",
            message = "Tidak ada notifikasi untuk kumbung ini",
            timestamp = "-"
        ))
        updateNotificationsUI(emptyList)
    }

    private fun updateNotificationsUI(notifications: List<NotifikasiData>) {
        try {
            // Find the LinearLayout container for notifications
            val notificationContainer = view?.findViewById<LinearLayout>(R.id.notification_items_container)
            if (notificationContainer == null) {
                Log.e("MonitoringAdmin", "Notification container not found")
                return
            }

            // Clear existing notifications (except header)
            if (notificationContainer.childCount > 1) {
                notificationContainer.removeViews(1, notificationContainer.childCount - 1)
            }

            // Create and add new notification items
            notifications.forEach { notification ->
                try {
                    // Inflate the notification item view
                    val notificationView = layoutInflater.inflate(
                        R.layout.item_notifikasi,
                        notificationContainer,
                        false
                    )

                    // Set message and timestamp
                    val messageTextView = notificationView.findViewById<TextView>(R.id.tv_notification_message)
                    val timestampTextView = notificationView.findViewById<TextView>(R.id.tv_notification_timestamp)

                    messageTextView.text = notification.message
                    timestampTextView.text = formatTimestamp(notification.timestamp)

                    // Add to container
                    notificationContainer.addView(notificationView)

                    // Add divider if not the last item
                    if (notification != notifications.last()) {
                        val divider = View(context)
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1
                        )
                        divider.layoutParams = params
                        divider.setBackgroundResource(R.color.divider)
                        notificationContainer.addView(divider)
                    }
                } catch (e: Exception) {
                    Log.e("MonitoringAdmin", "Error creating notification item: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("MonitoringAdmin", "Error updating notification UI: ${e.message}", e)
        }
    }

    private fun formatTimestamp(timestamp: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("HH:mm dd-MM-yyyy", Locale.getDefault())
            val date = inputFormat.parse(timestamp) ?: return timestamp
            return outputFormat.format(date)
        } catch (e: Exception) {
            return timestamp
        }
    }
}