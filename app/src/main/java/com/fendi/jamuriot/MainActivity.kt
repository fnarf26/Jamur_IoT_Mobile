package com.fendi.jamuriot

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import com.fendi.jamuriot.databinding.ActivityMainBinding
import com.fendi.jamuriot.fragment.FragmentKelolaPetugas
import com.fendi.jamuriot.fragment.FragmentKelolaKumbung
import com.fendi.jamuriot.fragment.FragmentMonitoringAdmin
import com.fendi.jamuriot.fragment.FragmentPengaturan
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener


class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener{

    lateinit var b : ActivityMainBinding
    lateinit var fmonitoringadmin : FragmentMonitoringAdmin
    lateinit var fkelolakumbung: FragmentKelolaKumbung
    lateinit var fkelolapetugas: FragmentKelolaPetugas
    lateinit var fpengaturan: FragmentPengaturan
    lateinit var ft : FragmentTransaction
    private var userRole: String = "admin" // Default role
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        
        // Get user role from SharedPreferences
        val prefs = getSharedPreferences("users", Context.MODE_PRIVATE)
        userRole = prefs.getString("role", "admin") ?: "admin"
        
        // Setup FCM topic subscriptions based on role
        setupFcmTopics()
        
        // Set up navigation and hide Kelola Petugas if user is petugas
        setupNavigation()
        
        fmonitoringadmin = FragmentMonitoringAdmin()
        fkelolakumbung = FragmentKelolaKumbung()
        fkelolapetugas = FragmentKelolaPetugas()
        fpengaturan = FragmentPengaturan()

        ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.frameLayout,fmonitoringadmin).commit()
        b.frameLayout.setBackgroundColor(Color.argb(255,255,255,255))
        b.frameLayout.visibility = View.VISIBLE
    }
    
    private fun setupFcmTopics() {
        Log.d(TAG, "Setting up FCM topics for role: $userRole")
        
        // First unsubscribe from all possible topics to prevent duplicate subscriptions
        FirebaseMessaging.getInstance().unsubscribeFromTopic("all_devices")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Unsubscribed from all_devices topic")
                }
            }
        
        // If admin - subscribe to all devices topic
        if (userRole == "admin") {
            // Admin subscribes to all device notifications
            FirebaseMessaging.getInstance().subscribeToTopic("all_devices")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Admin successfully subscribed to all_devices topic")
                    } else {
                        Log.e(TAG, "Failed to subscribe to all_devices topic", task.exception)
                    }
                }

        } else if (userRole == "petugas") {
            // Petugas should only subscribe to their assigned kumbung topics
            val email =
                getSharedPreferences("user", Context.MODE_PRIVATE).getString("email", "") ?: ""

            FirebaseMessaging.getInstance().subscribeToTopic(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Petugas successfully subscribed to email topic")
                    } else {
                        Log.e(TAG, "Failed to subscribe to email topic", task.exception)
                    }
                }
        }
    }
    
    private fun setupNavigation() {
        b.bottomNavigationView.setOnItemSelectedListener(this)
        
        // Hide Kelola Petugas menu item for petugas role
        if (userRole == "petugas") {
            val menu = b.bottomNavigationView.menu
            menu.findItem(R.id.itemKelolaPetugas)?.isVisible = false
        }
    }

    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        when(p0.itemId){
            R.id.itemMonitoring -> {
                ft = supportFragmentManager.beginTransaction()
                ft.replace(R.id.frameLayout,fmonitoringadmin).commit()
                b.frameLayout.setBackgroundColor(Color.argb(255,255,255,255))
                b.frameLayout.visibility = View.VISIBLE
            }
            R.id.itemKelolaKumbung -> {
                ft = supportFragmentManager.beginTransaction()
                ft.replace(R.id.frameLayout,fkelolakumbung).commit()
                b.frameLayout.setBackgroundColor(Color.argb(255,255,255,255))
                b.frameLayout.visibility = View.VISIBLE
            }
            R.id.itemKelolaPetugas -> {
                // Add extra security check in case someone bypasses the UI
                if (userRole != "petugas") {
                    ft = supportFragmentManager.beginTransaction()
                    ft.replace(R.id.frameLayout,fkelolapetugas).commit()
                    b.frameLayout.setBackgroundColor(Color.argb(255,255,255,255))
                    b.frameLayout.visibility = View.VISIBLE
                } else {
                    // If somehow a petugas tries to access this, redirect to monitoring
                    b.bottomNavigationView.selectedItemId = R.id.itemMonitoring
                    return false
                }
            }
            R.id.itemPengaturan -> {
                ft = supportFragmentManager.beginTransaction()
                ft.replace(R.id.frameLayout,fpengaturan).commit()
                b.frameLayout.setBackgroundColor(Color.argb(255,255,255,255))
                b.frameLayout.visibility = View.VISIBLE
            }
        }
        return true
    }

    override fun onStart() {
        super.onStart()
        // Memeriksa izin untuk notifikasi hanya di Android 13 dan lebih baru
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                // Jika izin belum diberikan, minta izin
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
    }

    // Menangani hasil dari permintaan izin
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Izin diterima, Anda bisa menampilkan notifikasi
                Toast.makeText(this, "Izin notifikasi diterima!", Toast.LENGTH_SHORT).show()
            } else {
                // Izin ditolak, beri tahu pengguna
                Toast.makeText(this, "Izin notifikasi ditolak.", Toast.LENGTH_SHORT).show()
                Toast.makeText(this, " Aplikasi tidak dapat menampilkan notifikasi.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}