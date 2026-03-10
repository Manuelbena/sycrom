package com.manuelbena.synkron.presentation.money.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.databinding.ItemMetaBinding
import com.manuelbena.synkron.presentation.models.GoalPresentationModel

class GoalsAdapter(
    private val onAddMoney: (GoalPresentationModel, Double) -> Unit, // Callback para ingresos rápidos
    private val onAddCustomMoney: (GoalPresentationModel) -> Unit,   // Callback para abrir dialog manual
    private val onDeleteGoal: (GoalPresentationModel) -> Unit        // Callback para borrar
) : ListAdapter<GoalPresentationModel, GoalsAdapter.GoalViewHolder>(GoalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemMetaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GoalViewHolder(private val binding: ItemMetaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(goal: GoalPresentationModel) {
            binding.apply {
                tvGoalTitle.text = goal.title
                tvGoalPercent.text = goal.formattedPercent
                tvGoalProgress.text = goal.formattedProgress
                tvTimeRemaining.text = goal.timeRemaining

                // Texto de sugerencia dinámico
                tvSuggestionText.text = goal.suggestionText

                lpiGoalItem.progress = goal.progressPercent

                try {
                    val color = Color.parseColor(goal.colorHex)
                    lpiGoalItem.setIndicatorColor(color)
                } catch (e: Exception) {
                    lpiGoalItem.setIndicatorColor(Color.parseColor("#F97316"))
                }

                // --- LISTENERS DE LA NUEVA BOTONERA ---

                btnDeleteGoal.setOnClickListener {
                    onDeleteGoal(goal)
                }

                btnAdd10.setOnClickListener {
                    onAddMoney(goal, 10.0)
                }

                btnAdd50.setOnClickListener {
                    onAddMoney(goal, 50.0)
                }

                btnAddCustom.setOnClickListener {
                    onAddCustomMoney(goal)
                }
            }
        }
    }

    class GoalDiffCallback : DiffUtil.ItemCallback<GoalPresentationModel>() {
        override fun areItemsTheSame(oldItem: GoalPresentationModel, newItem: GoalPresentationModel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: GoalPresentationModel, newItem: GoalPresentationModel) = oldItem == newItem
    }
}