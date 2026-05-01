package com.manuelbena.synkron.presentation.superTask // O .presentation.adapter según tu estructura

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.ItemSuperTaskDetailBinding
import com.manuelbena.synkron.domain.models.SubTaskItem

class SuperTaskDetailAdapter(
    private val items: MutableList<SubTaskItem>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<SuperTaskDetailAdapter.DetailViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        val binding = ItemSuperTaskDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        android.util.Log.d("AdapterCheck", "Pintando fila posición: $position - ${items[position].title}")
        holder.bind(items[position], position)
    }

    override fun getItemCount() = items.size

    fun getUpdatedItems(): List<SubTaskItem> = items

    inner class DetailViewHolder(private val binding: ItemSuperTaskDetailBinding) : RecyclerView.ViewHolder(binding.root) {

        private var seriesWatcher: TextWatcher? = null
        private var repsWatcher: TextWatcher? = null
        private var weightWatcher: TextWatcher? = null

        fun bind(item: SubTaskItem, position: Int) {
            binding.tvTitle.text = "${position + 1}. ${item.title}"

            val parts = item.details.split(" ")
            val seriesVal = parts.getOrElse(0) { "" }.split("x").getOrElse(0) { "" }
            val repsVal = parts.getOrElse(0) { "" }.split("x").getOrElse(1) { "" }
            val weightVal = parts.getOrElse(1) { "" }.replace("kg", "")

            binding.etSeries.removeTextChangedListener(seriesWatcher)
            binding.etReps.removeTextChangedListener(repsWatcher)
            binding.etWeight.removeTextChangedListener(weightWatcher)

            binding.etSeries.setText(seriesVal)
            binding.etReps.setText(repsVal)
            binding.etWeight.setText(weightVal)

            updateCheckVisuals(item.isCompleted)

            seriesWatcher = object : SimpleTextWatcher() {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    updateItemDetails(position, s.toString(), binding.etReps.text.toString(), binding.etWeight.text.toString())
                }
            }
            repsWatcher = object : SimpleTextWatcher() {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    updateItemDetails(position, binding.etSeries.text.toString(), s.toString(), binding.etWeight.text.toString())
                }
            }
            weightWatcher = object : SimpleTextWatcher() {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    updateItemDetails(position, binding.etSeries.text.toString(), binding.etReps.text.toString(), s.toString())
                }
            }

            binding.etSeries.addTextChangedListener(seriesWatcher)
            binding.etReps.addTextChangedListener(repsWatcher)
            binding.etWeight.addTextChangedListener(weightWatcher)

            binding.ivCheck.setOnClickListener {
                val newItem = items[position].copy(isCompleted = !items[position].isCompleted)
                items[position] = newItem
                updateCheckVisuals(newItem.isCompleted)
                onItemClick(position)
            }
        }

        private fun updateCheckVisuals(isCompleted: Boolean) {
            if (isCompleted) {
                binding.ivCheck.setImageResource(R.drawable.ic_check_circle)
                binding.ivCheck.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.md_theme_primary))
                binding.root.alpha = 0.7f
            } else {
                binding.ivCheck.setImageResource(R.drawable.ic_check_circle) // Asegúrate de tener este drawable
                binding.ivCheck.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.secondary_text))
                binding.root.alpha = 1.0f
            }
        }

        private fun updateItemDetails(pos: Int, s: String, r: String, w: String) {
            val newDetails = "${s}x${r} ${w}kg"
            val currentItem = items[pos]
            items[pos] = currentItem.copy(details = newDetails)
        }
    }

    open class SimpleTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {}
    }
}