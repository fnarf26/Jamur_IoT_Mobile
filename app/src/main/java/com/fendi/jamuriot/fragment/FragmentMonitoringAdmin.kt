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
import com.fendi.jamuriot.MainActivity
import com.fendi.jamuriot.R
import com.fendi.jamuriot.RiwayatNotifikasiActivity
import com.fendi.jamuriot.adapter.KumbungMonitoringAdapter
import com.fendi.jamuriot.databinding.FragmentMonitoringadminBinding
import com.fendi.jamuriot.model.KumbungData
import com.fendi.jamuriot.model.NotifikasiData
import com.google.android.material.tabs.TabLayoutMediator
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

    // --- PERUBAHAN 1: Tambahkan referensi untuk settings ---
    private lateinit var dbSettingsRef: DatabaseReference

    private var currentDeviceId: String? = null
    private var userSelectedPosition = 0
    private var isUserSwipe = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        thisParent = activity as MainActivity
        b = FragmentMonitoringadminBinding.inflate(inflater, container, false)

        dbRef = FirebaseDatabase.getInstance().getReference("devices")
        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications")

        // --- PERUBAHAN 2: Inisialisasi referensi settings ---
        dbSettingsRef = FirebaseDatabase.getInstance().getReference("settings")

        val prefs = thisParent.getSharedPreferences("user", Context.MODE_PRIVATE)
        val nama = prefs.getString("nama", "N/A")
        val role = prefs.getString("role", "N/A")

        b.tvGreeting.text = "Halo, ${nama}"

        if (role == "petugas") {
            b.tvControlSpray.visibility = View.VISIBLE
            b.cardSprayControl.visibility = View.VISIBLE
        } else {
            b.tvControlSpray.visibility = View.GONE
            b.cardSprayControl.visibility = View.GONE
        }

        b.btnNotification.setOnClickListener {
            startActivity(Intent(thisParent, RiwayatNotifikasiActivity::class.java))
        }

        setupViewPager()

        if (role == "petugas") {
            setupSprayControl()
        }

        // --- PERUBAHAN 3: Panggil fungsi untuk mengambil data threshold ---
        fetchThresholds()
        loadRegisteredDevices()

        return b.root
    }

    // --- PERUBAHAN 4: Tambahkan fungsi baru ini ---
    private fun fetchThresholds() {
        dbSettingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Gunakan metode yang aman untuk mengambil nilai
                    val tempThreshold = snapshot.child("batasSuhu").value.toString().toFloatOrNull() ?: 100f
                    val humidityThreshold = snapshot.child("batasKelembapan").value.toString().toFloatOrNull() ?: 0f

                    // Kirim nilai threshold ke adapter jika sudah siap
                    if (::kumbungAdapter.isInitialized) {
                        kumbungAdapter.setThresholds(tempThreshold, humidityThreshold)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MonitoringAdmin", "Gagal mengambil threshold: ${error.message}")
            }
        })
    }

    private fun setupViewPager() {
        kumbungAdapter = KumbungMonitoringAdapter(
            onDetailClickListener = { deviceId ->
                navigateToDetailKumbung(deviceId)
            }
        )
        b.viewPagerKumbung.adapter = kumbungAdapter
        TabLayoutMediator(b.tabIndicator, b.viewPagerKumbung) { _, _ -> }.attach()
        b.viewPagerKumbung.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentKumbungPosition = position
                userSelectedPosition = position
                isUserSwipe = true
                currentDeviceId = kumbungAdapter.getDeviceId(position)
                updateSprayControlAndInfo(position)
                currentDeviceId?.let { deviceId ->
                    loadNotificationsForDevice(deviceId)
                }
            }
        })
    }

    // Sisa kode di bawah ini tidak perlu diubah...
    private fun setupSprayControl() {
        try {
            b.switchSpray.setOnCheckedChangeListener { _, isChecked ->
                val deviceId = kumbungAdapter.getDeviceId(currentKumbungPosition)
                if (deviceId == null) {
                    Toast.makeText(thisParent, "Tidak dapat menemukan ID perangkat", Toast.LENGTH_SHORT).show()
                    return@setOnCheckedChangeListener
                }
                b.switchSpray.isEnabled = false
                dbRef.child(deviceId).child("manualSpray").setValue(isChecked)
                    .addOnSuccessListener {
                        updateSprayStatusUI(isChecked)
                        b.switchSpray.isEnabled = true
                    }
                    .addOnFailureListener {
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

    private fun updateSprayControlAndInfo(position: Int) {
        try {
            val deviceId = kumbungAdapter.getDeviceId(position) ?: return
            val prefs = thisParent.getSharedPreferences("user", Context.MODE_PRIVATE)
            val role = prefs.getString("role", "N/A")

            dbRef.child(deviceId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        if (!snapshot.exists()) return
                        if (role == "petugas") {
                            val sprayStatus = snapshot.child("manualSpray").getValue(Boolean::class.java) ?: false
                            b.switchSpray.isChecked = sprayStatus
                            updateSprayStatusUI(sprayStatus)
                        }
                        val harvestCount = snapshot.child("harvestCount").getValue(Long::class.java) ?: 0
                        val mediaCount = snapshot.child("mediaCount").getValue(Long::class.java) ?: 0
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
        val prefs = thisParent.getSharedPreferences("user", Context.MODE_PRIVATE)
        val role = prefs.getString("role", "N/A") ?: "N/A"
        val email = prefs.getString("email", "") ?: ""

        kumbungAdapter.updateData(emptyList())
        kumbungAdapter.clearDeviceIdMappings()

        if (role == "petugas") {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users").document(email).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val assignedKumbungs = document.get("kumbung") as? List<String> ?: emptyList()
                        if (assignedKumbungs.isEmpty()) {
                            Toast.makeText(thisParent, "Tidak ada kumbung yang ditugaskan", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                        loadFilteredDevices(assignedKumbungs)
                    } else {
                        Toast.makeText(thisParent, "Data petugas tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(thisParent, "Gagal memuat data petugas: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            loadAllDevices()
        }
    }

    private fun loadFilteredDevices(assignedDeviceIds: List<String>) {
        val currentPosition = b.viewPagerKumbung.currentItem
        val currentDeviceId = kumbungAdapter.getDeviceId(currentPosition)
        val assignedDeviceIdSet = assignedDeviceIds.map { it.trim() }.toSet()

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newKumbungList = ArrayList<KumbungData>()
                val newDeviceIdToNameMap = mutableMapOf<String, String>()
                val newNameToPositionMap = mutableMapOf<String, Int>()

                kumbungAdapter.clearDeviceIdMappings()

                for (deviceSnapshot in snapshot.children) {
                    val deviceId = deviceSnapshot.key ?: continue
                    if (deviceId in assignedDeviceIdSet) {
                        val register = deviceSnapshot.child("register").getValue(Boolean::class.java) ?: false
                        if (register) {
                            val name = deviceSnapshot.child("name").getValue(String::class.java) ?: "-"
                            val lastUpdateSnapshot = deviceSnapshot.child("lastUpdate").getValue(String::class.java) ?: "-"
                            val tempAvg = deviceSnapshot.child("temperatureAverage").getValue(Double::class.java) ?: 0.0
                            val humAvg = deviceSnapshot.child("humidityAverage").getValue(Double::class.java) ?: 0.0
                            newKumbungList.add(KumbungData(deviceId, name, lastUpdateSnapshot, tempAvg, humAvg))
                            newNameToPositionMap[name] = newKumbungList.size - 1
                            newDeviceIdToNameMap[deviceId] = name
                            kumbungAdapter.setDeviceId(name, deviceId)
                        }
                    }
                }
                kumbungAdapter.updateData(newKumbungList)
                if (newKumbungList.isEmpty()) {
                    return
                }
                var positionToRestore = 0
                if (isUserSwipe && userSelectedPosition < newKumbungList.size) {
                    positionToRestore = userSelectedPosition
                } else if (currentDeviceId != null) {
                    val deviceName = newDeviceIdToNameMap[currentDeviceId]
                    if (deviceName != null && newNameToPositionMap.containsKey(deviceName)) {
                        positionToRestore = newNameToPositionMap[deviceName] ?: 0
                    } else if (currentPosition < newKumbungList.size) {
                        positionToRestore = currentPosition
                    }
                }
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
                kumbungAdapter.updateData(newKumbungList)
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
        if (kumbungList.isNotEmpty() && b.viewPagerKumbung.currentItem < kumbungList.size) {
            val position = b.viewPagerKumbung.currentItem
            updateSprayControlAndInfo(position)
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

    private fun loadNotificationsForDevice(deviceId: String) {
        val deviceName = kumbungAdapter.getKumbungAt(currentKumbungPosition)?.name ?: ""
        notificationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    showEmptyNotifications()
                    return
                }
                val allNotifications = mutableListOf<NotifikasiData>()
                for (deviceGroupSnapshot in snapshot.children) {
                    for (notifSnapshot in deviceGroupSnapshot.children) {
                        try {
                            val message = notifSnapshot.child("message").getValue(String::class.java) ?: ""
                            val timestamp = notifSnapshot.child("timestamp").getValue(String::class.java) ?: ""
                            if (message.contains(deviceId) || message.contains(deviceName) || deviceGroupSnapshot.key == deviceId) {
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
                val sortedNotifications = allNotifications.sortedByDescending { it.timestamp }
                if (sortedNotifications.isEmpty()) {
                    showEmptyNotifications()
                    return
                }
                updateNotificationsUI(sortedNotifications.take(5))
            }
            override fun onCancelled(error: DatabaseError) {
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
            val notificationContainer = view?.findViewById<LinearLayout>(R.id.notification_items_container)
            if (notificationContainer == null) {
                return
            }
            if (notificationContainer.childCount > 1) {
                notificationContainer.removeViews(1, notificationContainer.childCount - 1)
            }
            notifications.forEach { notification ->
                try {
                    val notificationView = layoutInflater.inflate(R.layout.item_notifikasi, notificationContainer, false)
                    val messageTextView = notificationView.findViewById<TextView>(R.id.tv_notification_message)
                    val timestampTextView = notificationView.findViewById<TextView>(R.id.tv_notification_timestamp)
                    messageTextView.text = notification.message
                    timestampTextView.text = formatTimestamp(notification.timestamp)
                    notificationContainer.addView(notificationView)
                    if (notification != notifications.last()) {
                        val divider = View(context)
                        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
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