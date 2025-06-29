package com.fendi.jamuriot.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.fendi.jamuriot.LoginActivity
import com.fendi.jamuriot.MainActivity
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
    private lateinit var thisParent: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        thisParent = activity as MainActivity
        b = FragmentPengaturanBinding.inflate(inflater, container, false)

        // Get shared preferences
        val prefs = thisParent.getSharedPreferences("user", Context.MODE_PRIVATE)

        // Check if user has admin role
        val userRole = prefs.getString("role", "user") ?: "user"

        // Only show threshold button for admin users
        if (userRole == "admin") {
            b.btnThreshold.visibility = View.VISIBLE
        } else {
            b.btnThreshold.visibility = View.GONE
        }

        b.btnProfil.setOnClickListener {
            val intent = Intent(thisParent, ProfilActivity::class.java).apply{}
            startActivity(intent)
        }

        b.btnThreshold.setOnClickListener {
            val intent = Intent(thisParent, PengaturanThresholdActivity::class.java).apply{}
            startActivity(intent)
        }

        b.btnLogout.setOnClickListener {
            AlertDialog.Builder(thisParent)
                .setTitle("Konfirmasi Logout")
                .setMessage("Apakah Anda yakin ingin logout?")
                .setPositiveButton("Ya") { dialog, _ ->
                    logout() // gunakan fungsi logout yang telah Anda buat
                }
                .setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        return b.root
    }

    fun logout() {
        val prefs = thisParent.getSharedPreferences("user", Context.MODE_PRIVATE)
        val role = prefs.getString("role", "") ?: ""
        val email = prefs.getString("email", "") ?: ""

        // Unsubscribe FCM topic berdasarkan role
        if (role == "admin") {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("all_devices")
        } else if (role == "petugas") {
            val topic = email.replace(".", ",")
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
        }

        // 🔐 Logout dari Firebase Authentication
        FirebaseAuth.getInstance().signOut()

        // Hapus SharedPreferences
        prefs.edit().clear().apply()

        // Pindah ke LoginActivity dan bersihkan task stack
        val intent = Intent(thisParent, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        thisParent.finish()
    }

}