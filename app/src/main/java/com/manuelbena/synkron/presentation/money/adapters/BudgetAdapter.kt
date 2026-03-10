package com.manuelbena.synkron.presentation.money.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.databinding.ItemBudgetCategoryBinding
import com.manuelbena.synkron.presentation.models.BudgetPresentationModel
import java.util.Locale

class BudgetAdapter : ListAdapter<BudgetPresentationModel, BudgetAdapter.BudgetViewHolder>(BudgetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val binding = ItemBudgetCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BudgetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BudgetViewHolder(private val binding: ItemBudgetCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(budget: BudgetPresentationModel) {
            binding.apply {
                tvCategoryTitle.text = budget.name
                tvCategoryLimit.text = String.Companion.format(Locale.getDefault(), "Límite: %.2f €", budget.limit)
                tvSpentValue.text = String.Companion.format(Locale.getDefault(), "%.2f €", budget.spent)
                tvAvailableValue.text = String.Companion.format(Locale.getDefault(), "%.2f €", budget.available)
                tvUsedPercent.text = "${budget.usedPercentage}%"

                // Configurar Barra de progreso
                lpiCategory.progress = budget.usedPercentage

                // Configurar Icono y Colores
                ivCategoryIcon.setImageResource(budget.iconRes)

                try {
                    val color = Color.parseColor(budget.colorHex)
                    ivCategoryIcon.imageTintList = ColorStateList.valueOf(color)
                    // Puedes aplicar una versión más clara del color al fondo del icono si lo deseas
                    lpiCategory.setIndicatorColor(color)
                } catch (e: Exception) {
                    // Fallback color in case of parsing error
                }

                // Color dinámico para la columna "Disponible" y "Usado" (Verde si ok, Rojo si se pasa)
                val statusColor = if (budget.usedPercentage >= 100) Color.parseColor("#EF4444") else Color.parseColor("#10B981")
                tvAvailableValue.setTextColor(statusColor)
                tvUsedPercent.setTextColor(statusColor)

                btnEdit.setOnClickListener {
                    // TODO: Abrir dialog/pantalla para editar el presupuesto
                }
            }
        }
    }

    class BudgetDiffCallback : DiffUtil.ItemCallback<BudgetPresentationModel>() {
        override fun areItemsTheSame(oldItem: BudgetPresentationModel, newItem: BudgetPresentationModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BudgetPresentationModel, newItem: BudgetPresentationModel): Boolean {
            return oldItem == newItem
        }
    }
}