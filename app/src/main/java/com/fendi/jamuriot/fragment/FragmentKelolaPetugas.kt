package com.fendi.jamuriot.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.fendi.jamuriot.MainActivity
import com.fendi.jamuriot.TambahPetugasActivity
import com.fendi.jamuriot.adapter.PetugasAdapter
import com.fendi.jamuriot.api.ApiClient
import com.fendi.jamuriot.databinding.FragmentKelolaPetugasBinding
import com.fendi.jamuriot.model.ApiResponse
import com.fendi.jamuriot.model.PetugasData
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FragmentKelolaPetugas : Fragment() {
    private lateinit var b: FragmentKelolaPetugasBinding
    private lateinit var thisParent: MainActivity
    private val firestore = FirebaseFirestore.getInstance()
    private val petugasList = mutableListOf<PetugasData>()
    private lateinit var adapter: PetugasAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        thisParent = activity as MainActivity
        b = FragmentKelolaPetugasBinding.inflate(inflater, container, false)

        b.cvAddButton.setOnClickListener {
            startActivity(Intent(thisParent, TambahPetugasActivity::class.java))
        }

        setupRecyclerView()

        return b.root
    }

    private fun setupRecyclerView() {
        adapter = PetugasAdapter(
            petugasList,
            // Edit click handler
            onItemClick = { petugas ->
                val intent = Intent(requireContext(), TambahPetugasActivity::class.java).apply {
                    putExtra("mode", "edit")
                    putExtra("email", petugas.email)
                    putExtra("nama", petugas.name)
                    putExtra("role", petugas.role)
                    putStringArrayListExtra("kumbung", ArrayList(petugas.kumbung))
                }
                startActivity(intent)
            },
            // Delete click handler - connects to hapusPetugas function
            onDeleteClick = { petugas ->
                hapusPetugas(petugas.email)
            }
        )
        b.rvStaffList.layoutManager = LinearLayoutManager(requireContext())
        b.rvStaffList.adapter = adapter
    }

    private fun hapusPetugas(email: String) {
        // Show confirmation dialog
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Petugas")
            .setMessage("Yakin ingin menghapus petugas ini?")
            .setPositiveButton("Ya") { _, _ ->

                // Delete via API
                ApiClient.apiService.deletePetugas(email).enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        if (response.isSuccessful) {
                            // Delete from local Firestore
                            firestore.collection("users").document(email).delete()
                                .addOnSuccessListener {
                                    Toast.makeText(requireContext(), "Petugas berhasil dihapus", Toast.LENGTH_SHORT).show()
                                    loadPetugas() // Refresh list
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "Gagal menghapus data lokal: ${e.message}", Toast.LENGTH_SHORT).show()
                                    loadPetugas() // Still refresh since API was successful
                                }
                        } else {
                            Toast.makeText(requireContext(), "Gagal menghapus: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        Toast.makeText(requireContext(), "Koneksi gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun loadPetugas() {
        petugasList.clear()

        firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
                // Process data
                for (document in result) {
                    val role = document.getString("role")
                    if (role == "petugas") {
                        val nama = document.getString("nama") ?: ""
                        val kumbung = document.get("kumbung") as? List<String> ?: emptyList()
                        val email = document.id // gunakan document ID (misalnya email)

                        val petugas = PetugasData(nama, role, email, kumbung)
                        petugasList.add(petugas)
                    }
                }

                // Sort list alphabetically by name (optional)
                petugasList.sortBy { it.name }

                // Update UI
                adapter.notifyDataSetChanged()

                // Show empty state if needed
                if (petugasList.isEmpty()) {
                    b.tvEmptyState.visibility = View.VISIBLE
                } else {
                    b.tvEmptyState.visibility = View.GONE
                }
            }
            .addOnFailureListener { error ->
                // Show error message
                Toast.makeText(requireContext(), "Gagal memuat data petugas: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        loadPetugas() // Refresh data whenever fragment becomes visible
    }
}
