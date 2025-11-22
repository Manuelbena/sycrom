package com.manuelbena.synkron.presentation.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.DialogAddNewReminderBinding
import com.manuelbena.synkron.presentation.models.ReminderItem
import com.manuelbena.synkron.presentation.models.ReminderMethod

class AddReminderDialog(
    private val onReminderCreated: (ReminderItem) -> Unit
) : DialogFragment() {

    private var _binding: DialogAddNewReminderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddNewReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout((resources.displayMetrics.widthPixels * 0.90).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        setupCustomDropdown()
    }

    private fun setupCustomDropdown() {
        val defaultUnits = arrayOf("minutos", "horas", "días")
        val units = try {
            resources.getStringArray(R.array.reminder_units)
        } catch (e: Exception) { defaultUnits }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        binding.actvCustomUnit.setAdapter(adapter)
        // Seleccionamos el primero por defecto pero SIN filtrar (false)
        binding.actvCustomUnit.setText(units[0], false)
    }

    // --- ESCONDER TECLADO ---
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // Usamos el token de la ventana actual para asegurar que se cierre
        binding.etCustomAmount.clearFocus()
        view?.windowToken?.let { token ->
            imm.hideSoftInputFromWindow(token, 0)
        }
    }

    private fun showDropdownUpwards() {
        val spinner = binding.actvCustomUnit
        val adapter = spinner.adapter ?: return

        // 1. Calculamos la altura total de la lista
        var totalHeight = 0
        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(spinner.width, View.MeasureSpec.AT_MOST)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        for (i in 0 until adapter.count) {
            // CORRECCIÓN AQUÍ: Usamos 'spinner.parent' (TextInputLayout) y lo casteamos a ViewGroup
            val parentView = spinner.parent as ViewGroup
            val itemView = adapter.getView(i, null, parentView)

            itemView.measure(widthMeasureSpec, heightMeasureSpec)
            totalHeight += itemView.measuredHeight
        }

        // Añadimos un pequeño margen de seguridad (padding del popup)
        val verticalPadding = 32 // un poco más de margen

        // 2. Movemos el inicio de la lista hacia arriba
        // (Altura del campo de texto + Altura de la lista + padding)
        spinner.dropDownVerticalOffset = -(spinner.height + totalHeight + verticalPadding)

        // 3. Mostramos la lista
        spinner.showDropDown()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        binding.root.setOnClickListener { hideKeyboard() }

        binding.rgTimeOptions.setOnCheckedChangeListener { _, checkedId ->
            hideKeyboard()
            binding.layoutCustomInput.isVisible = (checkedId == R.id.rbCustom)
        }

        // LISTENER MODIFICADO: Cierra teclado y fuerza lista HACIA ARRIBA
        binding.actvCustomUnit.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                hideKeyboard()
                showDropdownUpwards() // <--- Usamos nuestra nueva función
            }
            // Devolvemos true para indicar que hemos consumido el evento y evitar el comportamiento por defecto (que abre hacia abajo)
            true
        }

        binding.btnCancel.setOnClickListener {
            hideKeyboard()
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            hideKeyboard()
            val minutes = getSelectedMinutes()
            if (minutes != null) {
                val method = when (binding.tabLayoutType.selectedTabPosition) {
                    0 -> ReminderMethod.POPUP
                    1 -> ReminderMethod.EMAIL
                    2 -> ReminderMethod.ALARM
                    else -> ReminderMethod.POPUP
                }
                val newItem = ReminderItem(minutes = minutes, method = method)
                onReminderCreated(newItem)
                dismiss()
            }
        }
    }

    private fun getSelectedMinutes(): Int? {
        return when (binding.rgTimeOptions.checkedRadioButtonId) {

            R.id.rb15Min -> 15
            R.id.rb30Min -> 30
            R.id.rb1Hour -> 60
            R.id.rb1Day -> 1440
            R.id.rbCustom -> {
                val amountStr = binding.etCustomAmount.text.toString()
                if (amountStr.isEmpty()) {
                    binding.etCustomAmount.error = "Requerido"
                    return null
                }
                val amount = amountStr.toIntOrNull() ?: 0
                val unit = binding.actvCustomUnit.text.toString()

                when {
                    unit.contains("hora") -> amount * 60
                    unit.contains("día") -> amount * 1440
                    unit.contains("semana") -> amount * 10080
                    else -> amount
                }
            }
            else -> 10
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddReminderDialog"
    }
}