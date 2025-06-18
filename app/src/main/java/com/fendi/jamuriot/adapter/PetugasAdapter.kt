package com.fendi.jamuriot.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.fendi.jamuriot.R
import com.fendi.jamuriot.model.PetugasData

class PetugasAdapter (
    private val staffList: List<PetugasData>,
    private val onItemClick: (PetugasData) -> Unit,
    private val onDeleteClick: (PetugasData) -> Unit // Add delete callback
) : RecyclerView.Adapter<PetugasAdapter.PetugasViewHolder>() {

    inner class PetugasViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvStaffName)
        val tvRole: TextView = itemView.findViewById(R.id.tvStaffRole)
        val tvEmail: TextView = itemView.findViewById(R.id.tvStaffEmail)
        val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)
        val ivArrow: ImageView = itemView.findViewById(R.id.ivArrow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetugasViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_petugas, parent, false)
        return PetugasViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetugasViewHolder, position: Int) {
        val staff = staffList[position]
        holder.tvName.text = staff.name
        holder.tvRole.text = staff.role
        holder.tvEmail.text = staff.email

        // Item klik: kirim data ke fungsi callback
        holder.itemView.setOnClickListener {
            onItemClick(staff)
        }

        // Arrow click (optional)
        holder.ivArrow.setOnClickListener {
            onItemClick(staff)
        }

        // Delete button click
        holder.ivDelete.setOnClickListener {
            onDeleteClick(staff)
        }
    }

    override fun getItemCount(): Int = staffList.size
}
