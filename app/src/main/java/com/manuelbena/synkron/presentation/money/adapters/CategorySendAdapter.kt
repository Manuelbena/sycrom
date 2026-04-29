package com.manuelbena.synkron.presentation.money.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.databinding.ItemExpenseRowBinding
import com.manuelbena.synkron.databinding.ItemSendCategoryBinding
import com.manuelbena.synkron.presentation.models.BudgetPresentationModel
import java.util.Locale

class CategoryAdapter(
    private var categories: List<BudgetPresentationModel> = emptyList()
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    // Guarda los IDs de las categorías que están desplegadas
    private val expandedItemIds = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemSendCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount() = categories.size

    fun submitList(newCategories: List<BudgetPresentationModel>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    inner class CategoryViewHolder(private val binding: ItemSendCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BudgetPresentationModel) {
            val isExpanded = expandedItemIds.contains(item.id)

            binding.apply {
                tvCategoryName.text = item.name
                tvCategoryAmount.text = String.format(Locale.getDefault(), "%.2f €", item.spent)
                tvEmojiIcon.text = item.emoji
                pbCategoryProgress.progress = item.usedPercentage

                // Subtítulo con % del total y nº de gastos (Si tienes la lista de transacciones en el modelo)
                // val numGastos = item.transactions.size
                val numGastos = 3 // MOCK (cámbialo por item.transactions.size cuando lo tengas)
                tvCategorySubtitle.text = "${item.usedPercentage}% del total • $numGastos gastos"

                // --- ESTADO INICIAL DEL DESPLIEGUE ---
                lyTransactionsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
                // Si ic_arrow_back apunta a la izq: -90 apunta ABAJO, 90 apunta ARRIBA
                ivChevron.rotation = if (isExpanded) 90f else -90f

                // --- CLIC PARA DESPLEGAR ---
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

                // --- PINTAR LOS GASTOS DENTRO DEL CONTENEDOR ---
                lyTransactionsContainer.removeAllViews()

                for (tx in item.transactions) {
                    val rowBinding = com.manuelbena.synkron.databinding.ItemExpenseRowBinding.inflate(
                        android.view.LayoutInflater.from(binding.root.context),
                        lyTransactionsContainer,
                        true // Lo incrusta dentro del acordeón
                    )

                    // Aquí usamos los campos de TU modelo personalizado:
                    rowBinding.tvExpenseNote.text = tx.title
                    rowBinding.tvExpenseDate.text = tx.subtitle

                    // ¡Usamos tu formateador de dinero automático!
                    rowBinding.tvExpenseAmount.text = tx.formattedAmount

                    // Si quisieras cambiar el color del texto a rojo o verde según si es ingreso/gasto:
                    if (tx.isIncome) {
                        rowBinding.tvExpenseAmount.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Verde
                    } else {
                        rowBinding.tvExpenseAmount.setTextColor(android.graphics.Color.parseColor("#111111")) // Oscuro
                    }
                }

                // APM (Color)
                try {
                    val color = Color.parseColor(item.colorHex)
                    val background = GradientDrawable()
                    background.shape = GradientDrawable.OVAL
                    background.setColor(adjustAlpha(color, 0.2f))
                    flIconBackground.background = background
                    pbCategoryProgress.progressTintList = ColorStateList.valueOf(color)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        private fun adjustAlpha(color: Int, factor: Float): Int {
            val alpha = (Color.alpha(color) * factor).toInt()
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        }
    }
}