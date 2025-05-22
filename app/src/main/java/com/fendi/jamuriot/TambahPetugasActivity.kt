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
import com.fendi.jamuriot.databinding.ItemPilihanKumbungBinding
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

        b.btnBack.setOnClickListener {
            finish()
        }

        b.btnBatal.setOnClickListener {
            finish()
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

                        // Gunakan ViewBinding untuk item_kumbung.xml
                        val kumbungBinding = ItemPilihanKumbungBinding.inflate(layoutInflater, b.kumbungContainer, false)

                        kumbungBinding.txtName.text = name
                        kumbungBinding.checkbox.tag = deviceSnapshot.key

                        b.kumbungContainer.addView(kumbungBinding.root)
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
            val itemView = b.kumbungContainer.getChildAt(i)
            val checkBox = itemView.findViewById<CheckBox>(R.id.checkbox)
            if (checkBox.isChecked) {
                val kumbungId = checkBox.tag as String
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
