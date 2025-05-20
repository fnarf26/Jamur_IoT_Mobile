package com.fendi.jamuriot.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fendi.jamuriot.LoginActivity
import com.fendi.jamuriot.MainActivity
import com.fendi.jamuriot.databinding.FragmentMonitoringadminBinding
import com.google.firebase.auth.FirebaseAuth


class FragmentMonitoringAdmin : Fragment() {
    private lateinit var b: FragmentMonitoringadminBinding
    private lateinit var v: View
    private lateinit var thisParent: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize binding and view
        thisParent = activity as MainActivity
        b = FragmentMonitoringadminBinding.inflate(inflater, container, false)
        v = b.root

        val prefs = thisParent.getSharedPreferences("user", Context.MODE_PRIVATE)
        val nama = prefs.getString("nama", "N/A")
        val role = prefs.getString("role", "N/A")
        val kumbung = prefs.getStringSet("kumbung", emptySet())

        b.tvGreeting.setText("Halo, ${nama}")

        b.btnNotification.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            prefs.edit().clear().apply()
            startActivity(Intent(thisParent, LoginActivity::class.java))
            thisParent.finish()
        }

        return v
    }
}