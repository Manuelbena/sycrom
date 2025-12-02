package com.manuelbena.synkron.presentation.task.adapter

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView


class SubtaskTouchHelperCallback(private val adapter: TaskCreationSubtaskAdapter) : ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled(): Boolean = false // Lo controlamos con el icono de arrastre
    override fun isItemViewSwipeEnabled(): Boolean = true

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        // Mover visualmente mientras arrastras
        adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Aquí podrías llamar a borrar si quisieras borrar deslizando
    }

    // Se llama cuando sueltas el elemento
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        // Guardar el orden final en el ViewModel
        adapter.onDragFinished()
    }
}