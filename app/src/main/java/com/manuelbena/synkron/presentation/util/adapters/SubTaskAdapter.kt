package com.manuelbena.synkron.presentation.util.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.manuelbena.synkron.R
import com.manuelbena.synkron.domain.models.SubTask


class SubtaskAdapter : ListAdapter<SubTask, SubtaskAdapter.SubtaskViewHolder>(SubtaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subtask, parent, false)
        return SubtaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubtaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SubtaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: MaterialCheckBox = itemView.findViewById(R.id.subtaskCheckbox)

        fun bind(subtask: SubTaskPresentation) {
            checkBox.text = subtask.title
            checkBox.isChecked = subtask.isDone
        }
    }
}

class SubtaskDiffCallback : DiffUtil.ItemCallback<SubTask>() { // <-- MODIFICADO
    override fun areItemsTheSame(oldItem: SubTask, newItem: SubTask): Boolean { // <-- MODIFICADO
        return oldItem.title == newItem.title // Assuming title is a unique identifier
    }

    override fun areContentsTheSame(oldItem: SubTask, newItem: SubTask): Boolean { // <-- MODIFICADO
        return oldItem == newItem
    }
}