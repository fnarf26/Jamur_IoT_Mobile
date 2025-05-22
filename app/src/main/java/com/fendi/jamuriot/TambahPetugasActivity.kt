package com.fendi.jamuriot

import android.os.Bundle
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fendi.jamuriot.databinding.ActivityTambahPetugasBinding
import com.fendi.jamuriot.databinding.ItemPilihanKumbungBinding
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore

class TambahPetugasActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTambahPetugasBinding
    private lateinit var dbRef: DatabaseReference
    private val db = FirebaseFirestore.getInstance()
    private val checkboxMap = mutableMapOf<String, CheckBox>()
    private var kumbungTerpilih: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahPetugasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbRef = FirebaseDatabase.getInstance().getReference("devices")

        val mode = intent.getStringExtra("mode") ?: "tambah"
        val email = intent.getStringExtra("email") ?: ""
        val nama = intent.getStringExtra("nama") ?: ""
        val role = intent.getStringExtra("role") ?: "petugas"

        if (mode == "edit") {
            binding.etEmail.setText(email)
            binding.etNama.setText(nama)
            binding.etEmail.isEnabled = false
            binding.etNama.isEnabled = false
            binding.tvFormPetugas.setText("Edit Petugas")

            // Ambil data kumbung dari Firestore
            db.collection("users").document(email).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        kumbungTerpilih = document.get("kumbung") as? List<String> ?: emptyList()
                    }
                    loadKumbungTerdaftar(kumbungTerpilih)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal memuat data pengguna", Toast.LENGTH_SHORT).show()
                    loadKumbungTerdaftar(emptyList())
                }
        } else {
            loadKumbungTerdaftar(emptyList())
        }

        binding.btnTambah.setOnClickListener {
            val selectedKumbung = ambilCheckboxTercentang()

            if (mode == "edit") {
                db.collection("users").document(email)
                    .update("kumbung", selectedKumbung)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Berhasil update kumbung", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal update", Toast.LENGTH_SHORT).show()
                    }
            } else {
                val newUser = hashMapOf(
                    "nama" to nama,
                    "role" to role,
                    "kumbung" to selectedKumbung
                )
                db.collection("users").document(email)
                    .set(newUser)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Petugas ditambahkan", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal menambahkan", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnBatal.setOnClickListener {
            finish()
        }
    }

    private fun loadKumbungTerdaftar(selected: List<String>) {
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.kumbungContainer.removeAllViews()
                checkboxMap.clear()

                for (deviceSnapshot in snapshot.children) {
                    val deviceId = deviceSnapshot.key ?: continue
                    val isRegistered = deviceSnapshot.child("register").getValue(Boolean::class.java) ?: false
                    if (isRegistered) {
                        val name = deviceSnapshot.child("name").getValue(String::class.java) ?: "Kumbung"

                        val kumbungBinding = ItemPilihanKumbungBinding.inflate(layoutInflater, binding.kumbungContainer, false)
                        kumbungBinding.txtName.text = name
                        kumbungBinding.checkbox.tag = deviceId
                        kumbungBinding.checkbox.isChecked = selected.contains(deviceId)

                        binding.kumbungContainer.addView(kumbungBinding.root)
                        checkboxMap[deviceId] = kumbungBinding.checkbox
                    }
                }

                if (checkboxMap.isEmpty()) {
                    Toast.makeText(this@TambahPetugasActivity, "Tidak ada kumbung terdaftar", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TambahPetugasActivity, "Gagal memuat kumbung", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun ambilCheckboxTercentang(): List<String> {
        return checkboxMap.filter { it.value.isChecked }.map { it.key }
    }
}
