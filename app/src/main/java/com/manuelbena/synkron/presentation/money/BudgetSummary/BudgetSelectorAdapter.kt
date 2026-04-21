package com.manuelbena.synkron.presentation.money.BudgetSummary

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.databinding.ItemBudgetSelectorBinding
import com.manuelbena.synkron.presentation.models.BudgetPresentationModel

class BudgetSelectorAdapter(
    private val budgets: List<BudgetPresentationModel>,
    private val onBudgetSelected: (BudgetPresentationModel) -> Unit
) : RecyclerView.Adapter<BudgetSelectorAdapter.ViewHolder>() {

    private var selectedPosition = -1

    inner class ViewHolder(val binding: ItemBudgetSelectorBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemBudgetSelectorBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val budget = budgets[position]

        holder.binding.apply {
            tvEmoji.text = budget.emoji
            tvBudgetName.text = budget.name

            // Crear el fondo circular
            val background = GradientDrawable()
            background.shape = GradientDrawable.OVAL

            if (position == selectedPosition) {
                // Seleccionado: Relleno con transparencia y borde del color del presupuesto
                val color = Color.parseColor(budget.colorHex)
                background.setColor(Color.argb(30, Color.red(color), Color.green(color), Color.blue(color)))
                background.setStroke(6, color)
                tvBudgetName.setTextColor(Color.BLACK)
            } else {
                // No seleccionado: Gris clarito
                background.setColor(Color.parseColor("#F3F4F6"))
                background.setStroke(0, Color.TRANSPARENT)
                tvBudgetName.setTextColor(Color.parseColor("#888888"))
            }

            circleBackground.background = background

            root.setOnClickListener {
                val previousSelected = selectedPosition
                selectedPosition = holder.adapterPosition
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
                onBudgetSelected(budget)
            }
        }
    }

    override fun getItemCount() = budgets.size
}