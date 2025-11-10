package com.manuelbena.synkron.presentation.task.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.R
import java.util.Collections

/**
 * Adapter para la lista de creación de subtareas en [TaskFragment].
 * Permite añadir, eliminar y reordenar (drag-and-drop) strings.
 *
 * @param items La lista mutable de strings (títulos de subtareas).
 * @param onStartDrag Callback para iniciar el drag-and-drop.
 */
class TaskCreationSubtaskAdapter(
    private val items: MutableList<String>,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<TaskCreationSubtaskAdapter.SubtaskViewHolder>() {

    /**
     * ViewHolder para el layout [R.layout.item_subtask_new].
     */
    inner class SubtaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.tvSubtaskText)
        val removeButton: ImageButton = view.findViewById(R.id.ibRemoveSubtask)
        val dragHandle: ImageView = view.findViewById(R.id.ivDragHandle)

        init {
            removeButton.setOnClickListener {
                val currentPosition = bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    // Eliminamos el item de la lista y notificamos
                    items.removeAt(currentPosition)
                    notifyItemRemoved(currentPosition)
                    notifyItemRangeChanged(currentPosition, items.size) // Actualiza posiciones
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subtask_new, parent, false)
        return SubtaskViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: SubtaskViewHolder, position: Int) {
        holder.textView.text = items[position]

        // Iniciamos el drag cuando el usuario toca el 'handle'
        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                onStartDrag(holder)
            }
            false
        }
    }

    override fun getItemCount() = items.size

    /**
     * Mueve un item en la lista y notifica al adapter.
     */
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
}

/**
 * Callback para [ItemTouchHelper] que gestiona el drag-and-drop.
 *
 * @param adapter El adapter al que notificar los movimientos.
 */
class SubtaskTouchHelperCallback(
    private val adapter: TaskCreationSubtaskAdapter
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN, // Direcciones de drag
    0 // Sin swipe
) {
    override fun isLongPressDragEnabled() = false // Desactivado, usamos el handle
    override fun isItemViewSwipeEnabled() = false

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        // Llama al método del adapter para mover el item
        adapter.onItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // No implementado
    }
}