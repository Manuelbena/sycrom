package com.manuelbena.synkron.presentation.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.BottomSheetCreateSuperTaskBinding
import com.manuelbena.synkron.domain.models.SubTaskItem
import com.manuelbena.synkron.domain.models.SuperTaskModel
import com.manuelbena.synkron.domain.models.SuperTaskType
import com.manuelbena.synkron.presentation.note.adapter.CreationAdapter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CreateSuperTaskBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCreateSuperTaskBinding? = null
    private val binding get() = _binding!!

    private var selectedType: SuperTaskType = SuperTaskType.GYM
    private var selectedDate: LocalDate = LocalDate.now()

    // Lista temporal
    private val currentSubTasks = mutableListOf<SubTaskItem>()
    private lateinit var adapter: CreationAdapter

    var onSaveListener: ((SuperTaskModel) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetCreateSuperTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    // --- FIX 2: FORZAR ALTURA COMPLETA PARA QUE NO SE VEA VACÍO ---
    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let { sheet ->
            // Esto es vital: Le decimos que ocupe toda la pantalla
            sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

            val behavior = BottomSheetBehavior.from(sheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- FIX 1: EL ORDEN IMPORTA (Evita el Crash) ---
        // Primero inicializamos el RecyclerView (y el adapter)
        setupRecyclerView()

        // Luego las categorías (que usan el adapter)
        setupCategories()

        setupDateSelector()
        setupListeners()
    }

    private fun setupCategories() {
        val types = listOf(
            SuperTaskType.GYM to "Gimnasio",
            SuperTaskType.WORK to "Estudio",
            SuperTaskType.COOKING to "Cocina",
            SuperTaskType.MEDITATION to "Personal"
        )

        binding.lyCategories.removeAllViews() // Limpiar por si acaso

        types.forEach { (type, label) ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_category_selector, binding.lyCategories, false)

            val tvLabel = itemView.findViewById<TextView>(R.id.tvCatName)
            val icon = itemView.findViewById<android.widget.ImageView>(R.id.ivCatIcon)
            val container = itemView.findViewById<View>(R.id.container) // Asegúrate de tener este ID en el XML del item

            tvLabel.text = label

            when(type) {
                SuperTaskType.GYM -> icon.setImageResource(R.drawable.ic_health)
                SuperTaskType.COOKING -> icon.setImageResource(R.drawable.ic_health) // Usa un drawable válido
                else -> icon.setImageResource(R.drawable.ic_health) // Icono por defecto
            }

            itemView.setOnClickListener {
                selectCategory(type)
                updateCategoryVisuals()
            }

            itemView.tag = type
            binding.lyCategories.addView(itemView)
        }

        updateCategoryVisuals()
    }

    private fun updateCategoryVisuals() {
        // Recorrer hijos y cambiar borde/color según selectedType
        for (i in 0 until binding.lyCategories.childCount) {
            val child = binding.lyCategories.getChildAt(i)
            val type = child.tag as? SuperTaskType ?: continue
            val isSelected = type == selectedType

            val bg = child.findViewById<View>(R.id.container)
            if (isSelected) {
                bg.setBackgroundResource(R.drawable.bg_category_selected)
            } else {
                bg.setBackgroundResource(R.drawable.bg_category_unselected)
            }
        }

        // Aquí es donde crasheaba antes si el adapter no estaba listo
        if (::adapter.isInitialized) {
            adapter.setType(selectedType)
        }

        binding.tvItemsLabel.text = if (selectedType == SuperTaskType.GYM) "Ejercicios" else "Elementos"
    }

    private fun selectCategory(type: SuperTaskType) {
        selectedType = type
    }

    private fun setupDateSelector() {
        updateDateText()
        binding.tvDateSelector.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecciona fecha")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                val date = Instant.ofEpochMilli(selection)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                selectedDate = date
                updateDateText()
            }
            picker.show(parentFragmentManager, "DATE_PICKER")
        }
    }

    private fun updateDateText() {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        binding.tvDateSelector.text = selectedDate.format(formatter)
    }

    private fun setupRecyclerView() {
        adapter = CreationAdapter { position ->
            if (position in currentSubTasks.indices) {
                currentSubTasks.removeAt(position)
                adapter.submitList(currentSubTasks.toList())
            }
        }

        binding.rvCreationItems.layoutManager = LinearLayoutManager(context)
        binding.rvCreationItems.adapter = adapter

        // Añadir un item inicial
        if (currentSubTasks.isEmpty()) {
            addItem()
        }
    }

    private fun addItem() {
        currentSubTasks.add(SubTaskItem(title = "", details = "", isCompleted = false))
        adapter.submitList(currentSubTasks.toList())
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener { dismiss() }

        binding.btnAddItem.setOnClickListener { addItem() }

        binding.btnSave.setOnClickListener {
            savePlan()
        }
    }

    private fun savePlan() {
        val title = binding.tietTitle.text.toString()
        if (title.isBlank()) {
            Toast.makeText(context, "Ponle un nombre al plan", Toast.LENGTH_SHORT).show()
            return
        }

        val validItems = adapter.getItems().filter { it.title.isNotBlank() }

        if (validItems.isEmpty()) {
            Toast.makeText(context, "Añade al menos un elemento", Toast.LENGTH_SHORT).show()
            return
        }

        val zoneId = ZoneId.systemDefault()
        val dateMillis = selectedDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val newSuperTask = SuperTaskModel(
            id = 0,
            date = dateMillis,
            title = title,
            type = selectedType,
            subTasks = validItems,
            completedCount = 0,
            totalCount = validItems.size
        )

        onSaveListener?.invoke(newSuperTask)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}