package com.manuelbena.synkron.presentation.util.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.manuelbena.synkron.R
import com.manuelbena.synkron.domain.models.SubTaskDomain


class SubtaskAdapter(
    private val onSubtaskCheckedChange: (subtask: SubTaskDomain, isDone: Boolean) -> Unit
) : ListAdapter<SubTaskDomain, SubtaskAdapter.SubtaskViewHolder>(SubtaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtaskViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_subtask, parent, false)
        return SubtaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubtaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SubtaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: MaterialCheckBox = itemView.findViewById(R.id.subtaskCheckbox)

        init {
            checkBox.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    val item = getItem(bindingAdapterPosition)
                    val newIsDone = !item.isDone

                    updateStrikeThrough(newIsDone)

                    onSubtaskCheckedChange(item, newIsDone)
                }
            }
        }

        fun bind(subtask: SubTaskDomain) {
            checkBox.text = subtask.title
            updateStrikeThrough(subtask.isDone)

            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = subtask.isDone
        }

        private fun updateStrikeThrough(isDone: Boolean) {
            if (isDone) {
                checkBox.paintFlags = checkBox.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                checkBox.paintFlags = checkBox.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
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
