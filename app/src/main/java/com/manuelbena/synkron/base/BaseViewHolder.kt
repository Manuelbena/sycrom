package com.manuelbena.synkron.base



import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * ViewHolder base que trabaja con ViewBinding.
 * @param B El tipo del ViewBinding.
 * @param T El tipo del modelo de datos.
 */
abstract class BaseViewHolder<T>(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
    abstract fun bind(item: T)
}
