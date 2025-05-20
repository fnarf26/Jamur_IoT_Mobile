package com.fendi.jamuriot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.fendi.jamuriot.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // ✅ Cek apakah sudah login sebelumnya
        val prefs = getSharedPreferences("user", Context.MODE_PRIVATE)
        val nama = prefs.getString("nama", null)
        val role = prefs.getString("role", null)

        if (nama != null && role != null) {
            // Sudah login → langsung masuk ke MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // 🔐 Proses login
        b.btnLogin.setOnClickListener {
            val email = b.etEmail.text.toString().trim()
            val password = b.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        // Ambil dokumen berdasarkan email sebagai ID
                        val userDocRef = db.collection("users").document(email)

                        userDocRef.get()
                            .addOnSuccessListener { doc ->
                                if (doc.exists()) {
                                    val nama = doc.getString("nama") ?: ""
                                    val role = doc.getString("role") ?: ""

                                    val editor = prefs.edit()
                                    editor.putString("nama", nama)
                                    editor.putString("role", role)

                                    if (role == "petugas") {
                                        val kumbung = doc.get("kumbung") as? List<*>
                                        editor.putStringSet("kumbung", kumbung?.mapNotNull { it?.toString() }?.toSet())
                                    }

                                    editor.apply()

                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(this, "Data pengguna tidak ditemukan di Firestore", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Gagal mengambil data pengguna", Toast.LENGTH_SHORT).show()
                            }

                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Login gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Email dan password harus diisi", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
