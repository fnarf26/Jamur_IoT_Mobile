package com.fendi.jamuriot

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fendi.jamuriot.databinding.ActivityProfilBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfilActivity : AppCompatActivity() {

    private lateinit var b: ActivityProfilBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var userRole: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        b = ActivityProfilBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Initialize Firebase components
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Load saved profile data
        loadProfileData()

        // Back button handler
        b.btnBack.setOnClickListener {
            finish()
        }
        
        // Reset password button
        b.btnResetPassword.setOnClickListener {
            showResetPasswordDialog()
        }
        
        // Save profile updates
        b.btnSave.setOnClickListener {
            saveProfileData()
        }

        // Cancel updates
        b.btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun loadProfileData() {
        // Get current user email
        val userEmail = auth.currentUser?.email
        
        if (userEmail != null) {
            // Load from Firestore using email as document ID
            firestore.collection("users").document(userEmail)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("nama") ?: ""
                        val email = document.getString("email") ?: userEmail
                        userRole = document.getString("role") ?: ""
                        
                        // Set data to fields
                        b.etName.setText(name)
                        b.etEmail.setText(email)
                        b.tvRole.setText(userRole)
                        
                        // Save to SharedPreferences as backup
                        saveToSharedPreferences(name, email, userRole)
                    } else {
                        // If no document exists yet, load from SharedPreferences as fallback
                        loadFromSharedPreferences()
                    }
                }
                .addOnFailureListener { 
                    // On failure, load from SharedPreferences
                    loadFromSharedPreferences()
                }
        } else {
            // If user is not logged in with Firebase, load from SharedPreferences
            loadFromSharedPreferences()
        }
    }
    
    private fun saveToSharedPreferences(name: String, email: String, role: String) {
        val sharedPrefs = getSharedPreferences("user", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putString("nama", name)
        editor.putString("email", email)
        editor.putString("role", role)
        editor.apply()
    }
    
    private fun loadFromSharedPreferences() {
        val sharedPrefs = getSharedPreferences("user", Context.MODE_PRIVATE)
        val savedName = sharedPrefs.getString("nama", "")
        val savedEmail = sharedPrefs.getString("email", "")
        userRole = sharedPrefs.getString("role", "") ?: ""
        
        b.etName.setText(savedName)
        b.etEmail.setText(savedEmail)
    }
    
    private fun saveProfileData() {
        val name = b.etName.text.toString().trim()
        val email = b.etEmail.text.toString().trim()
        
        // Simple validation
        if (name.isEmpty()) {
            Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email tidak valid", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Save to SharedPreferences (keep for backward compatibility)
        saveToSharedPreferences(name, email, userRole)

        firestore.collection("users").document(email)
            .update("nama", name)
            .addOnSuccessListener {
                Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memperbarui: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun showResetPasswordDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Apakah Anda yakin ingin reset password? Link reset akan dikirim ke email Anda.")
            .setPositiveButton("Ya") { _, _ ->
                val email = b.etEmail.text.toString()
                if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    // Show loading indicator
                    val loadingDialog = AlertDialog.Builder(this)
                        .setMessage("Mengirim email reset...")
                        .setCancelable(false)
                        .create()
                    loadingDialog.show()
                    
                    // Use Firebase Auth to reset password
                    auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            loadingDialog.dismiss()
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    this,
                                    "Email reset password telah dikirim ke $email",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Gagal mengirim email reset: ${task.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                } else {
                    Toast.makeText(
                        this,
                        "Email tidak valid untuk reset password",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Tidak", null)
            .show()
    }
}