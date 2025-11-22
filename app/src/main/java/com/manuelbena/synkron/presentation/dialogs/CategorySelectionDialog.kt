package com.manuelbena.synkron.presentation.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import com.manuelbena.synkron.databinding.DialogCategorySelectionBinding

import com.manuelbena.synkron.presentation.models.CategoryType
import com.manuelbena.synkron.presentation.task.adapter.CategoryGridAdapter

class CategorySelectionDialog(
    private val onCategorySelected: (CategoryType) -> Unit
) : DialogFragment() {

    private var _binding: DialogCategorySelectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogCategorySelectionBinding.inflate(inflater, container, false)

        // Hacemos el fondo del diálogo transparente para que se vea el CardView redondeado
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configuramos el Adapter (Reutilizamos el que creamos antes)
        val adapter = CategoryGridAdapter { category ->
            onCategorySelected(category)
            dismiss()
        }

        binding.rvCategoriesDialog.apply {
            layoutManager = GridLayoutManager(context, 3)
            this.adapter = adapter
        }

        adapter.submitList(CategoryType.getAll())

        binding.btnCloseDialog.setOnClickListener {
            dismiss()
        }

        // binding.btnManageCategories.setOnClickListener { ... }
    }

    // Para que el dialogo ocupe un ancho adecuado
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(), // 85% del ancho
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CategorySelectionDialog"
    }
}