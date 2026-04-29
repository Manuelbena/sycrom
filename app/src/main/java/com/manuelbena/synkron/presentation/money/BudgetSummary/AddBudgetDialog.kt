package com.manuelbena.synkron.presentation.money.dialogs

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.DialogAddBudgetBinding
import com.manuelbena.synkron.databinding.ItemColorCircleBinding


class AddBudgetDialog(
    private val onSave: (emoji: String, colorHex: String, title: String, limit: Double, type: String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogAddBudgetBinding? = null
    private val binding get() = _binding!!

    // Lista de colores predefinidos (Estilo Material)
    private val predefinedColors = listOf(
        "#EF4444", // Rojo
        "#F97316", // Naranja
        "#EAB308", // Amarillo
        "#10B981", // Verde
        "#0EA5E9", // Azul
        "#8B5CF6", // Violeta
        "#EC4899"  // Rosa
    )

    // Estado del color seleccionado (Rojo por defecto)
    private var selectedColorHex = predefinedColors[0]

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupColorRecyclerView()

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSave.setOnClickListener {
            val emoji = binding.etEmoji.text.toString().trim()
            val title = binding.etTitle.text.toString().trim()
            val amountStr = binding.etAmount.text.toString().trim()

            if (emoji.isEmpty() || title.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(requireContext(), "Introduce un importe válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // AHORA ENVIAMOS TAMBIÉN EL COLOR Y EL TIPO
            val type = if (binding.toggleGroupType.checkedButtonId == R.id.btnTypeIncome) "INCOME" else "EXPENSE"
            onSave(emoji, selectedColorHex, title, amount, type)
            dismiss()
        }
    }

    private fun setupColorRecyclerView() {
        binding.rvColors.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = ColorAdapter(predefinedColors) { color ->
                selectedColorHex = color // Actualizamos el estado del diálogo
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- ADAPTADOR INTERNO MUY SIMPLE PARA LOS COLORES ---

    inner class ColorAdapter(
        private val colors: List<String>,
        private val onColorSelected: (String) -> Unit
    ) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

        inner class ColorViewHolder(val binding: ItemColorCircleBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
            val binding = ItemColorCircleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ColorViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
            val colorHex = colors[position]
            val color = Color.parseColor(colorHex)

            // Pintamos el círculo con el color
            val gradientDrawable = GradientDrawable()
            gradientDrawable.shape = GradientDrawable.OVAL
            gradientDrawable.setColor(color)
            holder.binding.viewColorCircle.background = gradientDrawable

            // Mostramos un borde si este color es el seleccionado
            val isSelected = colorHex == selectedColorHex
            val strokeColor = if (isSelected) Color.BLACK else Color.TRANSPARENT
            gradientDrawable.setStroke(6, strokeColor)

            holder.itemView.setOnClickListener {
                selectedColorHex = colorHex
                notifyDataSetChanged() // Refrescamos para mover el borde
                onColorSelected(colorHex)
            }
        }

        override fun getItemCount(): Int = colors.size
    }
}