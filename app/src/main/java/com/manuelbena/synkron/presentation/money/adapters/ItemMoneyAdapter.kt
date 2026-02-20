package com.manuelbena.synkron.presentation.money.adapters

import android.annotation.SuppressLint
import android.graphics.Color

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.ItemMoneyBinding
import com.manuelbena.synkron.base.BaseAdapter
import com.manuelbena.synkron.base.BaseViewHolder
import com.manuelbena.synkron.presentation.models.ItemMoneyPresentation
import com.manuelbena.synkron.presentation.models.TypeSpent


class ItemMoneyAdapter:
    BaseAdapter<ItemMoneyPresentation, ItemMoneyAdapter.ItemMoneyHolder>(
        diffCallback
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemMoneyHolder =
        ItemMoneyHolder(
            ItemMoneyBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )


    inner class ItemMoneyHolder(private val binding: ItemMoneyBinding) :
        BaseViewHolder<ItemMoneyPresentation>(binding) {
        @SuppressLint("SetTextI18n")
        override fun bind(data: ItemMoneyPresentation) {
            binding.icMoney.setImageResource(data.icon)
            binding.tvConcept.text = data.concept

            if (data.import > 0){
                binding.tvImport.setTextColor(Color.parseColor("#2E9C8B"))
            }else{
                binding.tvImport.setTextColor(Color.parseColor("#FFC62828"))
            }
            binding.tvImport.text = data.import.toString() + " €"
            when(data.type){
                TypeSpent.GENERALES -> {
                    binding.chip.text = "Generales"
                    binding.chip.setChipBackgroundColorResource(R.color.gray)
                    binding.chip.setTextColor(Color.parseColor("#4C5A92"))
                    binding.chip.setChipStrokeColorResource(R.color.navy)
                }
                TypeSpent.FIJOS -> {
                    binding.chip.text = "Fijos"
                    binding.chip.setChipBackgroundColorResource(R.color.amber_ligh)
                    binding.chip.setTextColor(Color.parseColor("#D4AF37"))
                    binding.chip.setChipStrokeColorResource(R.color.amber)
                }
                TypeSpent.OCIO -> {
                    binding.chip.text = "Ocio"
                    binding.chip.setChipBackgroundColorResource(R.color.teal_ligh)
                    binding.chip.setTextColor(Color.parseColor("#692E9C8B"))
                    binding.chip.setChipStrokeColorResource(R.color.teal)

                }
        }
        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<ItemMoneyPresentation>() {
            override fun areItemsTheSame(
                oldItem: ItemMoneyPresentation,
                newItem: ItemMoneyPresentation
            ): Boolean {
                return oldItem.concept == newItem.concept
            }

            override fun areContentsTheSame(
                oldItem: ItemMoneyPresentation,
                newItem: ItemMoneyPresentation
            ): Boolean {
                TODO("Not yet implemented")
            }


        }
    }
}