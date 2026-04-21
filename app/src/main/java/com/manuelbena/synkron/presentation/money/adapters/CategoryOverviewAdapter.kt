package com.manuelbena.synkron.presentation.money.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.databinding.ItemSendCategoryBinding
import com.manuelbena.synkron.presentation.models.BudgetPresentationModel
import java.util.Locale

class CategoryOverviewAdapter : ListAdapter<BudgetPresentationModel, CategoryOverviewAdapter.CategoryViewHolder>(BudgetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemSendCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(private val binding: ItemSendCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BudgetPresentationModel) {
            binding.apply {
                tvCategoryName.text = item.name

                // Muestra lo que llevas gastado (Ej: 12.00 €)
                tvCategoryAmount.text = String.format(java.util.Locale.getDefault(), "%.2f €", item.spent)

                // CALCULAMOS EL PORCENTAJE RESTANTE
                var percentLeft = 100 - item.usedPercentage
                if (percentLeft < 0) percentLeft = 0 // Para que no salga negativo si te pasas

                tvCategoryPercentage.text = "Queda $percentLeft%"

                // La barra de progreso muestra lo USADO (así visualmente se va llenando de color)
                pbCategoryProgress.progress = item.usedPercentage

                // PINTAMOS EL EMOJI
                tvEmojiIcon.text = item.emoji

                // COLOR MÁGICO
                try {
                    val color = Color.parseColor(item.colorHex)

                    // Fondo transparente para el círculo
                    val background = GradientDrawable()
                    background.shape = GradientDrawable.OVAL
                    background.setColor(adjustAlpha(color, 0.2f))
                    flIconBackground.background = background

                    // Color de la barra
                    pbCategoryProgress.progressTintList = ColorStateList.valueOf(color)

                    // Si te pasas, ponemos el texto rojo
                    if(item.usedPercentage >= 100) {
                        tvCategoryPercentage.setTextColor(Color.parseColor("#EF4444"))
                        tvCategoryPercentage.text = "¡Límite superado!"
                    } else {
                        tvCategoryPercentage.setTextColor(Color.parseColor("#888888"))
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        private fun adjustAlpha(color: Int, factor: Float): Int {
            val alpha = (Color.alpha(color) * factor).toInt()
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        }
    }
    class BudgetDiffCallback : DiffUtil.ItemCallback<BudgetPresentationModel>() {
        override fun areItemsTheSame(oldItem: BudgetPresentationModel, newItem: BudgetPresentationModel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BudgetPresentationModel, newItem: BudgetPresentationModel) = oldItem == newItem
    }
}