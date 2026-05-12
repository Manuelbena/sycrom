package com.manuelbena.synkron.presentation.money.dialogs

import android.util.Log
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                // Permitimos que se expanda solo hasta su contenido
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClose.setOnClickListener { dismiss() }
        setupColorRecyclerView()
        setupTabListener()

        binding.btnSave.setOnClickListener {
            val emoji = binding.etEmoji.text.toString().trim()
            val title = binding.etTitle.text.toString().trim()
            val amountStr = binding.etAmount.text.toString().trim()
            val selectedTab = binding.tabLayoutType.selectedTabPosition
            val type = if (selectedTab == 0) "EXPENSE" else "INCOME"

            Log.d("DEBUG_BUDGET", "Dialog: Botón Guardar pulsado. Tab seleccionado: $selectedTab -> Tipo: $type")

            if (emoji.isEmpty() || title.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var amount = 0.0
            if (type == "EXPENSE") {
                if (amountStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Introduce un importe para el límite", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val parsedAmount = amountStr.toDoubleOrNull()
                if (parsedAmount == null || parsedAmount <= 0) {
                    Toast.makeText(requireContext(), "Introduce un importe válido", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                amount = parsedAmount
            }

            // AHORA ENVIAMOS TAMBIÉN EL COLOR Y EL TIPO
            onSave(emoji, selectedColorHex, title, amount, type)
            dismiss()
        }
    }

    private fun setupTabListener() {
        binding.tabLayoutType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val isExpense = tab?.position == 0
                binding.tvAmountLabel.visibility = if (isExpense) View.VISIBLE else View.GONE
                binding.tilAmount.visibility = if (isExpense) View.VISIBLE else View.GONE
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
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