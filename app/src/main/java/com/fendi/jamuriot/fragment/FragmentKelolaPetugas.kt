package com.fendi.jamuriot.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.fendi.jamuriot.MainActivity
import com.fendi.jamuriot.TambahPetugasActivity
import com.fendi.jamuriot.adapter.PetugasAdapter
import com.fendi.jamuriot.databinding.FragmentKelolaPetugasBinding
import com.fendi.jamuriot.model.PetugasData
import com.google.firebase.firestore.FirebaseFirestore

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
        loadPetugas()

        return b.root
    }

    private fun setupRecyclerView() {
        adapter = PetugasAdapter(petugasList) { petugas ->
            val intent = Intent(requireContext(), TambahPetugasActivity::class.java).apply {
                putExtra("mode", "edit") // <--- MODE TAMBAHAN
                putExtra("email", petugas.email)
                putExtra("nama", petugas.name)
                putExtra("role", petugas.role)
                putStringArrayListExtra("kumbung", ArrayList(petugas.kumbung))
            }
            startActivity(intent)
        }
        b.rvStaffList.layoutManager = LinearLayoutManager(requireContext())
        b.rvStaffList.adapter = adapter
    }

    private fun loadPetugas() {
        petugasList.clear()

        firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
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
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal memuat data petugas.", Toast.LENGTH_SHORT).show()
            }
    }
}
