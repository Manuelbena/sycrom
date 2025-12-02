package com.manuelbena.synkron.presentation.task.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.databinding.ItemSubtaskNewBinding
import com.manuelbena.synkron.domain.models.SubTaskDomain
import java.util.Collections

class TaskCreationSubtaskAdapter(
    private val items: MutableList<SubTaskDomain> = mutableListOf(),
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
    private val onRemove: (SubTaskDomain) -> Unit,
    private val onReorder: (List<SubTaskDomain>) -> Unit
) : RecyclerView.Adapter<TaskCreationSubtaskAdapter.SubtaskViewHolder>() {

    fun updateItems(newItems: List<SubTaskDomain>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    // Este método se llama EN CADA PASO del arrastre.
    // Solo actualizamos la lista visualmente, NO avisamos al ViewModel todavía.
    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(items, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    // NUEVO: Este método se llamará cuando el usuario SUELTE el ítem.
    // Aquí es seguro actualizar el ViewModel sin romper la animación.
    fun onDragFinished() {
        onReorder(items.toList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtaskViewHolder {
        val binding = ItemSubtaskNewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SubtaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubtaskViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SubtaskViewHolder(private val binding: ItemSubtaskNewBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("ClickableViewAccessibility")
        fun bind(item: SubTaskDomain) {
            binding.tvSubtaskText.text = item.title

            binding.ibRemoveSubtask.setOnClickListener {
                onRemove(item)
            }

            binding.ivDragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onStartDrag(this)
                }
                false
            }
        }
    }
}