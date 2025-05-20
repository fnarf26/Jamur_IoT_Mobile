package com.fendi.jamuriot.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fendi.jamuriot.MainActivity
import com.fendi.jamuriot.TambahPetugasActivity
import com.fendi.jamuriot.databinding.FragmentKelolaPetugasBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class FragmentKelolaPetugas : Fragment() {
    private lateinit var b: FragmentKelolaPetugasBinding
    private lateinit var thisParent: MainActivity
    private lateinit var dbRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        thisParent = activity as MainActivity
        b = FragmentKelolaPetugasBinding.inflate(inflater, container, false)

        dbRef = FirebaseDatabase.getInstance().getReference("devices")

        b.cvAddButton.setOnClickListener {
            startActivity(Intent(thisParent, TambahPetugasActivity::class.java))
        }


        return b.root
    }
}