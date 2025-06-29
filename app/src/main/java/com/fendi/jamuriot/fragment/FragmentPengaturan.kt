package com.fendi.jamuriot.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.fendi.jamuriot.LoginActivity
import com.fendi.jamuriot.PengaturanThresholdActivity
import com.fendi.jamuriot.ProfilActivity
import com.fendi.jamuriot.databinding.FragmentPengaturanBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging

class FragmentPengaturan : Fragment() {
    private lateinit var b: FragmentPengaturanBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        b = FragmentPengaturanBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance() // Inisialisasi Firebase Auth

        // Get shared preferences
        val prefs = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE)

        // Check if user has admin role
        val userRole = prefs.getString("role", "petugas") ?: "petugas"

        // Only show threshold button for admin users
        if (userRole == "admin") {
            b.btnThreshold.visibility = View.VISIBLE
        } else {
            b.btnThreshold.visibility = View.GONE
        }

        b.btnProfil.setOnClickListener {
            val intent = Intent(requireActivity(), ProfilActivity::class.java)
            startActivity(intent)
        }

        b.btnThreshold.setOnClickListener {
            val intent = Intent(requireActivity(), PengaturanThresholdActivity::class.java)
            startActivity(intent)
        }

        b.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireActivity())
                .setTitle("Konfirmasi Logout")
                .setMessage("Apakah Anda yakin ingin logout?")
                .setPositiveButton("Ya") { _, _ ->
                    logout()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        return b.root
    }

    private fun logout() {
        // --- PERBAIKAN 1: Gunakan SharedPreferences "user" (tanpa 's') ---
        val prefs = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE)
        val role = prefs.getString("role", "") ?: ""
        val assignedKumbungs = prefs.getString("assigned_kumbungs", "") ?: ""

        // Unsubscribe dari topik-topik FCM berdasarkan role
        // (Logika ini sudah benar, hanya saja role-nya sebelumnya salah)
        FirebaseMessaging.getInstance().unsubscribeFromTopic("all_devices")

        if (role == "admin") {
            val dbRef = FirebaseDatabase.getInstance().getReference("devices")
            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (deviceSnapshot in snapshot.children) {
                        val deviceId = deviceSnapshot.key ?: continue
                        val topic = "device_$deviceId"
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Logout", "Error unsubscribing admin: ${error.message}")
                }
            })
        } else if (role == "petugas" && assignedKumbungs.isNotEmpty()) {
            val kumbungList = assignedKumbungs.split(",")
            kumbungList.forEach { kumbungId ->
                if (kumbungId.isNotBlank()) {
                    val topic = "kumbung_${kumbungId.trim()}"
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                }
            }
        }

        // --- PERBAIKAN 2: Tambahkan signOut dari Firebase Auth ---
        auth.signOut()

        // Hapus semua data dari SharedPreferences "user"
        prefs.edit().clear().apply()

        // Arahkan ke halaman login dan bersihkan activity stack
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}