package com.fendi.jamuriot

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.fendi.jamuriot.databinding.ActivityMainBinding
import com.fendi.jamuriot.fragment.FragmentKelolaPetugas
import com.fendi.jamuriot.fragment.FragmentKelolaKumbung
import com.fendi.jamuriot.fragment.FragmentMonitoringAdmin
import com.google.android.material.navigation.NavigationBarView


class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener{

    lateinit var b : ActivityMainBinding
    lateinit var fmonitoringadmin : FragmentMonitoringAdmin
    lateinit var fkelolakumbung: FragmentKelolaKumbung
    lateinit var fkelolapetugas: FragmentKelolaPetugas
    lateinit var ft : FragmentTransaction

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cek apakah pengguna sudah login
//        val currentUser = auth.currentUser
//        if (currentUser == null) {
//            // Jika belum login, arahkan ke LoginActivity
//            val intent = Intent(this, LoginActivity::class.java)
//            startActivity(intent)
//            finish() // Tutup MainActivity
//            return
//        }else{
//            // Subscribe to Firebase topic
//            Firebase.messaging.subscribeToTopic(topic).addOnCompleteListener { task ->
//                val message = if (task.isSuccessful) {
//                    "Subscribed to $topic"
//                } else {
//                    "Failed to subscribe to topic"
//                }
//            }
//        }


        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.bottomNavigationView.setOnItemSelectedListener(this)

        fmonitoringadmin = FragmentMonitoringAdmin()
        fkelolakumbung = FragmentKelolaKumbung()
        fkelolapetugas = FragmentKelolaPetugas()

        ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.frameLayout,fmonitoringadmin).commit()
        b.frameLayout.setBackgroundColor(Color.argb(255,255,255,255))
        b.frameLayout.visibility = View.VISIBLE
    }

//    override fun onStart() {
//        super.onStart()
//        db = FirebaseDatabase.getInstance().getReference("device/10000000001")
//        db.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                Log.d("FirebaseDebug", "DataSnapshot exists: ${snapshot.exists()}")  // Check if snapshot exists
//                if (snapshot.exists()) {
//                    val profile = snapshot.child("profile")
//                    val name = profile.child("name").getValue(String::class.java)
//                    Log.d("FirebaseDebug", "Profile name: $name")  // Log the profile name
//                    val status = profile.child("status").getValue(String::class.java)
//                    Log.d("FirebaseDebug", "Profile status: $status")  // Log the profile status
//                    // Update UI here
//                } else {
//                    Log.d("FirebaseDebug", "No data found in the snapshot.")
//                    Toast.makeText(this@MainActivity, "No data available", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.d("FirebaseDebug", "Database error: ${error.message}")  // Log the error message
//                Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
//            }
//        })
//    }

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
            ft = supportFragmentManager.beginTransaction()
            ft.replace(R.id.frameLayout,fkelolapetugas).commit()
            b.frameLayout.setBackgroundColor(Color.argb(255,255,255,255))
            b.frameLayout.visibility = View.VISIBLE
        }
//            R.id.itemHistory -> {
//                ft = supportFragmentManager.beginTransaction()
//                ft.replace(R.id.frameLayout,fhistory).commit()
//                b.frameLayout.setBackgroundColor(Color.argb(255,255,255,255))
//                b.frameLayout.visibility = View.VISIBLE
//            }
//            R.id.itemHistory -> replaceFragment("https://pkl.supala.fun/historyPakan")
//
//            R.id.itemProgres -> {
//                ft = supportFragmentManager.beginTransaction()
//                ft.replace(R.id.frameLayout,fprogres).commit()
//                b.frameLayout.setBackgroundColor(Color.argb(255,255,255,255))
//                b.frameLayout.visibility = View.VISIBLE
//            }
//            R.id.itemSetting -> {
//                ft = supportFragmentManager.beginTransaction()
//                ft.replace(R.id.frameLayout,fsetting).commit()
//                b.frameLayout.setBackgroundColor(Color.argb(255,255,255,255))
//                b.frameLayout.visibility = View.VISIBLE
//            }
        }
        return true
    }


//    fun replaceFragment(url: String) {
//        GlobalVariables.url = url
//        b.frameLayout.visibility = View.GONE
//        ft = supportFragmentManager.beginTransaction()
//        ft.remove(fhistory).commitNow()
//        ft = supportFragmentManager.beginTransaction()
//        ft.replace(R.id.frameLayout, fhistory).commit()
//        b.frameLayout.visibility = View.VISIBLE
//    }
//
//    override fun onResume() {
//        super.onResume()
//
//        // Fetch Firebase token and set it to edit text
////        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
////            if (task.isSuccessful) {
////                Toast.makeText(this, task.result, Toast.LENGTH_LONG).show()
////            }
////        }
//
//        // Try to get extras from the intent
//        try {
//            bundle = intent.extras
//        } catch (e: Exception) {
//            Log.e("BUNDLE", "Bundle is null", e)
//        }
//
//        // Handle bundle data if available
//        bundle?.let {
//            type = it.getInt("type")
//            when (type) {
//                0 -> {
////                    binding.edPromoId.setText(it.getString("promoId"))
////                    binding.edPromo.setText(it.getString("promo"))
////                    binding.edPromoUntil.setText(it.getString("promoUntil"))
//                }
//                1 -> {
////                    binding.edTitle.setText(it.getString("title"))
////                    binding.edBody.setText(it.getString("body"))
//                    Toast.makeText(this, it.getString("body"), Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//    }
}