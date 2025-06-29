package com.fendi.jamuriot.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fendi.jamuriot.DetailKumbungActivity
import com.fendi.jamuriot.MainActivity
import com.fendi.jamuriot.adapter.KumbungAdapter
import com.fendi.jamuriot.databinding.FragmentKelolaKumbungBinding
import com.fendi.jamuriot.model.KumbungData
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore

class FragmentKelolaKumbung : Fragment() {
    private lateinit var b: FragmentKelolaKumbungBinding
    private lateinit var thisParent: MainActivity
    private lateinit var dbRef: DatabaseReference
    private lateinit var dbSettingsRef: DatabaseReference
    private lateinit var adapter: KumbungAdapter // Adapter sekarang menjadi properti kelas
    private val kumbungList = ArrayList<KumbungData>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        thisParent = activity as MainActivity
        b = FragmentKelolaKumbungBinding.inflate(inflater, container, false)

        dbRef = FirebaseDatabase.getInstance().getReference("devices")
        dbSettingsRef = FirebaseDatabase.getInstance().getReference("settings")

        // Setup RecyclerView dan Adapter SATU KALI SAJA
        setupRecyclerView()

        // Panggil fungsi untuk mengambil data threshold
        fetchThresholds()

        // Panggil fungsi untuk memuat data dari Firebase
        loadRegisteredDevices()

        // Atur visibilitas dan listener berdasarkan role
        setupRoleSpecificUI()

        return b.root
    }

    private fun setupRecyclerView() {
        val isAdmin = (thisParent.getSharedPreferences("user", Context.MODE_PRIVATE)
            .getString("role", "petugas") == "admin")

        adapter = KumbungAdapter(
            kumbungList,
            { deletedData ->
                // Handler untuk menghapus item
                dbRef.orderByChild("name").equalTo(deletedData.name)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (item in snapshot.children) item.ref.removeValue()
                            Toast.makeText(thisParent, "${deletedData.name} dihapus", Toast.LENGTH_SHORT).show()
                        }
                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(thisParent, "Gagal menghapus: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            },
            { deviceId ->
                // Handler untuk klik item
                navigateToDetailKumbung(deviceId)
            },
            isAdmin,
            thisParent
        )

        b.rvKumbung.layoutManager = LinearLayoutManager(thisParent)
        b.rvKumbung.adapter = adapter

        if (isAdmin) {
            setupSwipeToDelete(adapter)
        }
    }

    private fun setupRoleSpecificUI() {
        if (adapter.canDelete()) { // Menggunakan flag isAdmin dari adapter
            b.inputSection.visibility = View.VISIBLE
            b.btnCheck.setOnClickListener { handleCheckImei() }
        } else {
            b.inputSection.visibility = View.GONE
            val params = b.tvDataHeader.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = 0
            b.tvDataHeader.layoutParams = params
            val constraintSet = ConstraintSet()
            constraintSet.clone(b.root as ConstraintLayout)
            constraintSet.connect(b.tvDataHeader.id, ConstraintSet.TOP, b.divider.id, ConstraintSet.BOTTOM, 0)
            constraintSet.applyTo(b.root as ConstraintLayout)
        }
    }

    private fun fetchThresholds() {
        dbSettingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val tempThreshold = snapshot.child("batasSuhu").value.toString().toFloatOrNull() ?: 100f
                    val humidityThreshold = snapshot.child("batasKelembapan").value.toString().toFloatOrNull() ?: 100f

                    if (::adapter.isInitialized) {
                        adapter.setThresholds(tempThreshold, humidityThreshold)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("KelolaKumbung", "Gagal mengambil threshold: ${error.message}")
            }
        })
    }

    private fun loadRegisteredDevices() {
        val prefs = thisParent.getSharedPreferences("user", Context.MODE_PRIVATE)
        val userRole = prefs.getString("role", "petugas") ?: "petugas"
        val email = prefs.getString("email", "") ?: ""

        if (userRole == "admin") {
            loadAllDevices()
        } else {
            loadPetugasDevices(email)
        }
    }

    private fun loadAllDevices() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newKumbungList = ArrayList<KumbungData>()
                for (deviceSnapshot in snapshot.children) {
                    val register = deviceSnapshot.child("register").getValue(Boolean::class.java) ?: false
                    if (register) {
                        val deviceId = deviceSnapshot.key ?: continue
                        val name = deviceSnapshot.child("name").getValue(String::class.java) ?: "-"
                        val lastUpdate = deviceSnapshot.child("lastUpdate").getValue(String::class.java) ?: "-"
                        val tempAvg = deviceSnapshot.child("temperatureAverage").getValue(Double::class.java) ?: 0.0
                        val humAvg = deviceSnapshot.child("humidityAverage").getValue(Double::class.java) ?: 0.0
                        newKumbungList.add(KumbungData(deviceId, name, lastUpdate, tempAvg, humAvg))
                        adapter.setDeviceId(name, deviceId)
                    }
                }
                updateAdapterData(newKumbungList)
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(thisParent, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadPetugasDevices(email: String) {
        FirebaseFirestore.getInstance().collection("users").document(email).get()
            .addOnSuccessListener { document ->
                val assignedKumbungs = if (document != null && document.exists()) {
                    document.get("kumbung") as? List<String> ?: emptyList()
                } else {
                    emptyList()
                }

                if (assignedKumbungs.isEmpty()) {
                    updateAdapterData(emptyList()) // Tampilkan daftar kosong jika tidak ada tugas
                    return@addOnSuccessListener
                }
                loadFilteredDevices(assignedKumbungs)
            }
            .addOnFailureListener { e ->
                Toast.makeText(thisParent, "Gagal memuat data petugas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadFilteredDevices(assignedKumbungs: List<String>) {
        val assignedKumbungSet = assignedKumbungs.map { it.trim() }.toSet()
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newKumbungList = ArrayList<KumbungData>()
                for (deviceSnapshot in snapshot.children) {
                    val deviceId = deviceSnapshot.key ?: continue
                    if (deviceId in assignedKumbungSet) {
                        val register = deviceSnapshot.child("register").getValue(Boolean::class.java) ?: false
                        if (register) {
                            val name = deviceSnapshot.child("name").getValue(String::class.java) ?: "-"
                            val lastUpdate = deviceSnapshot.child("lastUpdate").getValue(String::class.java) ?: "-"
                            val tempAvg = deviceSnapshot.child("temperatureAverage").getValue(Double::class.java) ?: 0.0
                            val humAvg = deviceSnapshot.child("humidityAverage").getValue(Double::class.java) ?: 0.0
                            newKumbungList.add(KumbungData(deviceId, name, lastUpdate, tempAvg, humAvg))
                            adapter.setDeviceId(name, deviceId)
                        }
                    }
                }
                updateAdapterData(newKumbungList)
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(thisParent, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateAdapterData(newKumbungList: List<KumbungData>) {
        kumbungList.clear()
        kumbungList.addAll(newKumbungList)
        adapter.notifyDataSetChanged() // Hanya perbarui data, jangan buat adapter baru
    }

    // --- SISA FUNGSI LAINNYA (TIDAK PERLU DIUBAH) ---
    private fun navigateToDetailKumbung(deviceId: String) {
        val intent = Intent(thisParent, DetailKumbungActivity::class.java)
        intent.putExtra("DEVICE_ID", deviceId)
        startActivity(intent)
    }

    private fun handleCheckImei() {
        val imei = b.etImei.text.toString().trim()
        if (imei.isNotEmpty()) {
            dbRef.child(imei).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val isRegistered = snapshot.child("register").getValue(Boolean::class.java) ?: false
                        if (isRegistered) {
                            Toast.makeText(thisParent, "Perangkat sudah terdaftar", Toast.LENGTH_SHORT).show()
                        } else {
                            showRegisterDialog(imei)
                        }
                    } else {
                        Toast.makeText(thisParent, "Perangkat tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(thisParent, "Terjadi kesalahan: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(thisParent, "IMEI tidak boleh kosong", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRegisterDialog(imei: String) {
        val input = EditText(thisParent)
        input.hint = "Nama perangkat"
        AlertDialog.Builder(thisParent)
            .setTitle("Daftarkan Perangkat")
            .setMessage("Masukkan nama untuk perangkat dengan IMEI: $imei")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val updates = mapOf("register" to true, "name" to name)
                    dbRef.child(imei).updateChildren(updates).addOnSuccessListener {
                        Toast.makeText(thisParent, "Berhasil didaftarkan", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(thisParent, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupSwipeToDelete(adapter: KumbungAdapter) {
        val itemTouchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.getItemAt(position)
                if (item != null) {
                    AlertDialog.Builder(thisParent).apply {
                        setTitle("Konfirmasi Hapus")
                        setMessage("Apakah Anda yakin ingin menghapus kumbung \"${item.name}\"?")
                        setPositiveButton("Hapus") { _, _ -> adapter.deleteItem(position) }
                        setNegativeButton("Batal") { dialog, _ ->
                            dialog.dismiss()
                            adapter.notifyItemChanged(position)
                        }
                        setCancelable(false)
                        show()
                    }
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(b.rvKumbung)
    }
}