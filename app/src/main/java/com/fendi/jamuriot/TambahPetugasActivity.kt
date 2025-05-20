package com.fendi.jamuriot

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.fendi.jamuriot.databinding.ActivityTambahPetugasBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore

class TambahPetugasActivity : AppCompatActivity() {

    private lateinit var b: ActivityTambahPetugasBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTambahPetugasBinding.inflate(layoutInflater)
        setContentView(b.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("devices")

        loadKumbungTerdaftar()

        b.btnTambah.setOnClickListener {
            simpanPetugas()
        }
    }

    private fun loadKumbungTerdaftar() {
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                b.kumbungContainer.removeAllViews()

                for (deviceSnapshot in snapshot.children) {
                    val isRegistered = deviceSnapshot.child("register").getValue(Boolean::class.java) ?: false
                    if (isRegistered) {
                        val name = deviceSnapshot.child("name").getValue(String::class.java) ?: "Kumbung"

                        val kumbungLayout = LinearLayout(this@TambahPetugasActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 16, 0, 16)
                            }
                            orientation = LinearLayout.VERTICAL
                            gravity = Gravity.CENTER
                        }

                        val icon = ImageView(this@TambahPetugasActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(80, 80)
                            setImageResource(R.drawable.ic_perangkat)
                            setColorFilter(getColor(R.color.blue_primary))
                        }

                        val label = TextView(this@TambahPetugasActivity).apply {
                            text = name
                            textSize = 14f
                            setTextColor(getColor(android.R.color.black))
                        }

                        val checkbox = CheckBox(this@TambahPetugasActivity).apply {
                            text = ""
                            buttonTintList = getColorStateList(R.color.blue_primary)
                            tag = deviceSnapshot.key
                        }

                        kumbungLayout.addView(icon)
                        kumbungLayout.addView(label)
                        kumbungLayout.addView(checkbox)
                        b.kumbungContainer.addView(kumbungLayout)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TambahPetugasActivity, "Gagal memuat kumbung", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun simpanPetugas() {
        val nama = b.etNama.text.toString().trim()
        val email = b.etEmail.text.toString().trim()
        val password = "123456"

        if (nama.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Nama dan Email harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil ID kumbung yang dicentang
        val kumbungList = mutableListOf<String>()
        for (i in 0 until b.kumbungContainer.childCount) {
            val layout = b.kumbungContainer.getChildAt(i) as LinearLayout
            val checkbox = layout.getChildAt(2) as CheckBox
            if (checkbox.isChecked) {
                val kumbungId = checkbox.tag as String
                kumbungList.add(kumbungId)
            }
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val userMap = hashMapOf(
                    "nama" to nama,
                    "role" to "petugas",
                    "kumbung" to kumbungList
                )

                firestore.collection("users").document(email).set(userMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Petugas berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal membuat akun: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
