package com.manuelbena.synkron.base

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter


abstract class BaseAdapter<T, VM : BaseViewHolder<T>>(diffCallback: DiffUtil.ItemCallback<T>) :
    ListAdapter<T, VM>(diffCallback) {

    override fun onBindViewHolder(holder: VM, position: Int) {
        holder.bind(getItem(position))
    }
}

