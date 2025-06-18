package com.fendi.jamuriot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
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

        b.btnForgotPassword.setOnClickListener {
            showResetPasswordDialog()
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
                                    editor.putString("email", email)
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

    private fun showResetPasswordDialog() {
        // Create a custom dialog layout with email input
        val dialogView = layoutInflater.inflate(R.layout.dialog_reset_password, null)
        val etResetEmail = dialogView.findViewById<EditText>(R.id.etResetEmail)

        // Pre-fill with email from login form if available
        val loginEmail = b.etEmail.text.toString().trim()
        if (loginEmail.isNotEmpty()) {
            etResetEmail.setText(loginEmail)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setView(dialogView)
            .setMessage("Masukkan email Anda untuk menerima link reset password.")
            .setPositiveButton("Kirim", null) // Set null to override default behavior
            .setNegativeButton("Batal", null)
            .create()

        dialog.show()

        // Override positive button click to prevent dialog dismissal on error
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val email = etResetEmail.text.toString().trim()

            // Validate email format
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etResetEmail.error = "Email tidak valid"
                return@setOnClickListener
            }

            // Show loading indicator
            val loadingDialog = AlertDialog.Builder(this)
                .setMessage("Memeriksa email...")
                .setCancelable(false)
                .create()
            loadingDialog.show()

            // Check if email exists in Firestore users collection
            db.collection("users").document(email).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Email exists in Firestore, send reset email
                        loadingDialog.setMessage("Mengirim email reset...")

                        auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { resetTask ->
                                loadingDialog.dismiss()
                                if (resetTask.isSuccessful) {
                                    dialog.dismiss()
                                    Toast.makeText(
                                        this,
                                        "Email reset password telah dikirim ke $email",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Gagal mengirim email reset: ${resetTask.exception?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    } else {
                        // Email not found in Firestore
                        loadingDialog.dismiss()
                        Toast.makeText(
                            this,
                            "Email tidak terdaftar di sistem",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener { e ->
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this,
                        "Gagal memeriksa email: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}
