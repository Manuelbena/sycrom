package com.manuelbena.synkron.presentation.task.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.databinding.ItemSubtaskNewBinding
import com.manuelbena.synkron.domain.models.SubTaskDomain
import java.util.Collections

class TaskCreationSubtaskAdapter(
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
    private val onRemove: (SubTaskDomain) -> Unit,
    private val onReorder: (List<SubTaskDomain>) -> Unit
) : ListAdapter<SubTaskDomain, TaskCreationSubtaskAdapter.SubtaskViewHolder>(SubtaskDiffCallback()) {

    fun updateItems(newItems: List<SubTaskDomain>) {
        submitList(newItems)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtaskViewHolder {
        val binding = ItemSubtaskNewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SubtaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubtaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // --- MÉTODOS QUE FALTABAN PARA EL DRAG & DROP ---

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        // 1. Creamos una copia mutable de la lista actual (porque currentList es solo lectura)
        val updatedList = currentList.toMutableList()

        // 2. Intercambiamos los elementos
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(updatedList, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(updatedList, i, i - 1)
            }
        }

        // 3. Actualizamos la lista oficial (esto dispara la animación)
        submitList(updatedList)
    }

    fun onDragFinished() {
        // 4. Notificamos al ViewModel del nuevo orden final
        onReorder(currentList)
    }

    // ------------------------------------------------

    inner class SubtaskViewHolder(private val binding: ItemSubtaskNewBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("ClickableViewAccessibility")
        fun bind(item: SubTaskDomain) {
            binding.tvSubtaskText.text = item.title

            binding.ibRemoveSubtask.setOnClickListener {
                onRemove(item)
            }

            // Si tienes un icono de arrastre (ej: ivDragHandle), descomenta esto:

            binding.ivDragHandle.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    onStartDrag(this)
                }
                false
            }

        }
    }

    class SubtaskDiffCallback : DiffUtil.ItemCallback<SubTaskDomain>() {
        override fun areItemsTheSame(oldItem: SubTaskDomain, newItem: SubTaskDomain): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SubTaskDomain, newItem: SubTaskDomain): Boolean {
            return oldItem == newItem
        }
    }
}