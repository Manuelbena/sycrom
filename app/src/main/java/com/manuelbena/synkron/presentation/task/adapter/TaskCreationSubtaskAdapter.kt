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
import com.manuelbena.synkron.domain.models.SubTaskDomain
import java.util.Collections

class TaskCreationSubtaskAdapter(
    private val items: MutableList<SubTaskDomain>,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
    private val onRemove: (SubTaskDomain) -> Unit
) : RecyclerView.Adapter<TaskCreationSubtaskAdapter.SubtaskViewHolder>() {

    inner class SubtaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.tvSubtaskText)
        val removeButton: ImageButton = view.findViewById(R.id.ibRemoveSubtask)
        val dragHandle: ImageView = view.findViewById(R.id.ivDragHandle)

        init {
            removeButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRemove(items[position])
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
        val item = items[position]
        holder.textView.text = item.title

        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                onStartDrag(holder)
            }
            false
        }
    }

    override fun getItemCount() = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<SubTaskDomain>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    // Este método es vital para el Callback
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

// --- AQUÍ ESTÁ LA CORRECCIÓN DEL CALLBACK ---
class SubtaskTouchHelperCallback(
    private val adapter: TaskCreationSubtaskAdapter
) : ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled() = false // Usamos el handle
    override fun isItemViewSwipeEnabled() = false

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = 0
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        adapter.onItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // No implementado
    }
}