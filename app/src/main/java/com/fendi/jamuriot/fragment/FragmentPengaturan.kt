package com.fendi.jamuriot.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
                    FirebaseAuth.getInstance().signOut()
                    prefs.edit().clear().apply()
                    startActivity(Intent(thisParent, LoginActivity::class.java))
                    thisParent.finish()
                }
                .setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        return b.root
    }
}