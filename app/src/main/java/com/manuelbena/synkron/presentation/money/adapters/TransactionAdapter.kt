package com.manuelbena.synkron.presentation.money.adapters


import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.databinding.ItemTransactionBinding
import com.manuelbena.synkron.presentation.models.TransactionPresentationModel

class TransactionAdapter : ListAdapter<TransactionPresentationModel, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: TransactionPresentationModel) {
            binding.apply {
                tvTransactionEmoji.text = transaction.emoji
                tvTransactionTitle.text = transaction.title
                tvTransactionSubtitle.text = transaction.subtitle
                tvTransactionAmount.text = transaction.formattedAmount

                // Lógica Visual: Verde para ingresos, Oscuro para gastos
                val amountColor = if (transaction.isIncome) Color.parseColor("#10B981") else Color.parseColor("#111827")
                tvTransactionAmount.setTextColor(amountColor)
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionPresentationModel>() {
        override fun areItemsTheSame(oldItem: TransactionPresentationModel, newItem: TransactionPresentationModel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TransactionPresentationModel, newItem: TransactionPresentationModel) = oldItem == newItem
    }
}