package com.manuelbena.synkron.presentation.note.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.databinding.ItemCreationRowBinding
import com.manuelbena.synkron.domain.models.SubTaskItem
import com.manuelbena.synkron.domain.models.SuperTaskType

class CreationAdapter(
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<CreationAdapter.CreationViewHolder>() {

    // Lista mutable interna
    private val items = mutableListOf<SubTaskItem>()
    private var currentType: SuperTaskType = SuperTaskType.GYM

    fun submitList(newItems: List<SubTaskItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setType(type: SuperTaskType) {
        currentType = type
        notifyDataSetChanged() // Refrescar para mostrar/ocultar campos
    }

    fun getItems(): List<SubTaskItem> = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreationViewHolder {
        val binding = ItemCreationRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CreationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CreationViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class CreationViewHolder(private val binding: ItemCreationRowBinding) : RecyclerView.ViewHolder(binding.root) {

        // Watchers para guardar lo que escribe el usuario en la lista temporal
        private var titleWatcher: TextWatcher? = null
        private var seriesWatcher: TextWatcher? = null
        private var repsWatcher: TextWatcher? = null
        private var weightWatcher: TextWatcher? = null
        private var detailsWatcher: TextWatcher? = null

        fun bind(item: SubTaskItem, position: Int) {
            binding.tvIndex.text = "${position + 1}."

            // VISIBILIDAD DE CAMPOS SEGÚN EL TIPO
            val isGym = currentType == SuperTaskType.GYM
            binding.lyGymFields.isVisible = isGym

            // Si no es GYM, usamos el campo "details" grande para notas/ingredientes
            binding.etDetails.isVisible = true
            binding.etDetails.hint = if(isGym) "Notas (opcional)" else "Detalles (ej: 200g, cap. 4)..."

            // 1. Quitar watchers viejos
            binding.etTitle.removeTextChangedListener(titleWatcher)
            binding.etSeries.removeTextChangedListener(seriesWatcher)
            binding.etReps.removeTextChangedListener(repsWatcher)
            binding.etWeight.removeTextChangedListener(weightWatcher)
            binding.etDetails.removeTextChangedListener(detailsWatcher)

            // 2. Parsear datos existentes (Si vienes de editar)
            // Asumimos formato GYM: "4x12 60kg"
            // Asumimos formato GENÉRICO en details

            binding.etTitle.setText(item.title)

            if (isGym && item.details.contains("x")) {
                // Intento simple de parseo inverso, mejorable
                val parts = item.details.split(" ")
                val sr = parts.getOrElse(0){""}.split("x")
                binding.etSeries.setText(sr.getOrElse(0){""})
                binding.etReps.setText(sr.getOrElse(1){""})
                binding.etWeight.setText(parts.getOrElse(1){""}.replace("kg",""))
            } else {
                binding.etDetails.setText(item.details)
                binding.etSeries.setText("")
                binding.etReps.setText("")
                binding.etWeight.setText("")
            }

            // 3. Crear nuevos watchers
            titleWatcher = object : SimpleTextWatcher() {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    items[position] = items[position].copy(title = s.toString())
                }
            }
            // Lógica para GYM: combinamos los 3 campos en el string 'details'
            val gymUpdater = {
                val s = binding.etSeries.text.toString()
                val r = binding.etReps.text.toString()
                val w = binding.etWeight.text.toString()
                val note = binding.etDetails.text.toString() // Nota opcional
                val detailsString = if (s.isNotEmpty() || r.isNotEmpty()) "${s}x${r} ${w}kg $note".trim() else note
                items[position] = items[position].copy(details = detailsString)
            }

            seriesWatcher = object : SimpleTextWatcher() { override fun afterTextChanged(s: Editable?) { gymUpdater() } }
            repsWatcher = object : SimpleTextWatcher() { override fun afterTextChanged(s: Editable?) { gymUpdater() } }
            weightWatcher = object : SimpleTextWatcher() { override fun afterTextChanged(s: Editable?) { gymUpdater() } }

            detailsWatcher = object : SimpleTextWatcher() {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (!isGym) {
                        items[position] = items[position].copy(details = s.toString())
                    } else {
                        gymUpdater()
                    }
                }
            }

            // 4. Asignar watchers
            binding.etTitle.addTextChangedListener(titleWatcher)
            if (isGym) {
                binding.etSeries.addTextChangedListener(seriesWatcher)
                binding.etReps.addTextChangedListener(repsWatcher)
                binding.etWeight.addTextChangedListener(weightWatcher)
            }
            binding.etDetails.addTextChangedListener(detailsWatcher)

            binding.btnDelete.setOnClickListener { onDeleteClick(position) }
        }
    }

    open class SimpleTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {}
    }
}