package com.manuelbena.synkron.presentation.money.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.databinding.ItemExpenseRowBinding
import com.manuelbena.synkron.databinding.ItemSendCategoryBinding
import com.manuelbena.synkron.presentation.models.BudgetPresentationModel
import java.util.Locale

class CategoryOverviewAdapter : ListAdapter<BudgetPresentationModel, CategoryOverviewAdapter.CategoryViewHolder>(BudgetDiffCallback()) {

    // ✅ AÑADIDO: Memoria para saber qué tarjeta está desplegada
    private val expandedItemIds = mutableSetOf<Int>()

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
            val isExpanded = expandedItemIds.contains(item.id)

            binding.apply {
                // 1. Textos principales
                tvCategoryName.text = item.name
                tvCategoryAmount.text = String.format(Locale.getDefault(), "%.2f €", item.spent)

                // 2. Progreso y Emoji
                pbCategoryProgress.progress = item.usedPercentage
                tvEmojiIcon.text = item.emoji

                // 3. Subtítulo con cantidad de gastos
                val numGastos = item.transactions.size
                tvCategorySubtitle.text = "${item.usedPercentage}% del total • $numGastos gastos"

                // --- ✅ LÓGICA DEL ACORDEÓN ACTIVADA ---
                ivChevron.visibility = View.VISIBLE // ¡Hacemos visible la flecha!
                lyTransactionsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
                ivChevron.rotation = if (isExpanded) 90f else -90f

                lyHeader.setOnClickListener {
                    val currentlyExpanded = expandedItemIds.contains(item.id)
                    if (currentlyExpanded) {
                        expandedItemIds.remove(item.id)
                        lyTransactionsContainer.visibility = View.GONE
                        ivChevron.animate().rotation(-90f).setDuration(200).start()
                    } else {
                        expandedItemIds.add(item.id)
                        lyTransactionsContainer.visibility = View.VISIBLE
                        ivChevron.animate().rotation(90f).setDuration(200).start()
                    }
                }

                // --- ✅ PINTAMOS LOS GASTOS DENTRO ---
                lyTransactionsContainer.removeAllViews()

                for (tx in item.transactions) {
                    val rowBinding = ItemExpenseRowBinding.inflate(
                        LayoutInflater.from(binding.root.context),
                        lyTransactionsContainer,
                        true // Lo incrusta dentro del acordeón
                    )

                    rowBinding.tvExpenseNote.text = tx.title
                    rowBinding.tvExpenseDate.text = tx.subtitle
                    rowBinding.tvExpenseAmount.text = tx.formattedAmount

                    if (tx.isIncome) {
                        rowBinding.tvExpenseAmount.setTextColor(Color.parseColor("#4CAF50")) // Verde
                    } else {
                        rowBinding.tvExpenseAmount.setTextColor(Color.parseColor("#111111")) // Oscuro
                    }
                }

                // APM (Aplicar Pinceladas Mágicas de Color)
                try {
                    val color = Color.parseColor(item.colorHex)

                    // 1. Fondo circular del icono
                    val background = GradientDrawable()
                    background.shape = GradientDrawable.OVAL
                    background.setColor(adjustAlpha(color, 0.2f))
                    flIconBackground.background = background

                    // ✅ 2. NUEVO: Pintar el LinearProgressIndicator de Material
                    // Color de la barra principal
                    pbCategoryProgress.setIndicatorColor(color)
                    // Color de la pista (fondo) con un 20% de opacidad para que haga juego
                    pbCategoryProgress.trackColor = adjustAlpha(color, 0.2f)

                    // 3. Comprobar si nos hemos pasado del presupuesto
                    if(item.usedPercentage >= 100) {
                        tvCategorySubtitle.setTextColor(Color.parseColor("#EF4444"))
                        tvCategorySubtitle.text = "¡Límite superado! • $numGastos gastos"

                        // Opcional: Si se pasa, la barra se pone ROJA para alertar
                        pbCategoryProgress.setIndicatorColor(Color.parseColor("#EF4444"))
                        pbCategoryProgress.trackColor = adjustAlpha(Color.parseColor("#EF4444"), 0.2f)
                    } else {
                        tvCategorySubtitle.setTextColor(Color.parseColor("#888888"))
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
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