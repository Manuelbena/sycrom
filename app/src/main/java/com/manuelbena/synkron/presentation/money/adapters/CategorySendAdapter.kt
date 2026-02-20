package com.manuelbena.synkron.presentation.money.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.databinding.ItemSendCategoryBinding // Asegúrate de que coincida con tu XML item_category.xml
import com.manuelbena.synkron.presentation.models.CategorySendModel

class CategoryAdapter(
    private val categories: List<CategorySendModel>
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

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

    inner class CategoryViewHolder(private val binding: ItemSendCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategorySendModel) {
            binding.tvCategoryName.text = item.name
            binding.tvCategoryAmount.text = item.amount
            binding.tvCategoryPercentage.text = item.percentage
            binding.pbCategoryProgress.progress = item.progress
            binding.ivCategoryIcon.setImageResource(item.iconRes)

            // APM (Aplicar Pinceladas Mágicas de Color)
            try {
                val color = Color.parseColor(item.colorHex)

                // 1. Tintar el icono
                binding.ivCategoryIcon.setColorFilter(color)

                // 2. Tintar el fondo del icono (Círculo) con un tono más suave o el mismo con alpha
                // Nota: Si usaste un drawable shape blanco, backgroundTint lo colorea
                // Para que se vea bonito como la imagen (fondo clarito), usaremos alpha
                binding.ivCategoryIcon.backgroundTintList = ColorStateList.valueOf(adjustAlpha(color, 0.2f))

                // 3. Tintar la barra de progreso
                binding.pbCategoryProgress.progressTintList = ColorStateList.valueOf(color)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Función auxiliar para hacer el color transparente (para el fondo del icono)
        private fun adjustAlpha(color: Int, factor: Float): Int {
            val alpha = (Color.alpha(color) * factor).toInt()
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        }
    }
}