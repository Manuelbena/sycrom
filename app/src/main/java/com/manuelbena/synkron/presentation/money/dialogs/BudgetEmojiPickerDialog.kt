package com.manuelbena.synkron.presentation.money.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.DialogEmojiPickerBinding

class BudgetEmojiPickerDialog(
    private val title: String,
    private val emojis: List<String>,
    private val onEmojiSelected: (String) -> Unit
) : DialogFragment() {

    private var _binding: DialogEmojiPickerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogEmojiPickerBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvPickerTitle.text = title

        binding.rvEmojis.apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = EmojiAdapter(emojis) { emoji ->
                onEmojiSelected(emoji)
                dismiss()
            }
        }

        binding.btnClosePicker.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.75).toInt(), // Reducido al 75% del ancho
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class EmojiAdapter(
        private val emojis: List<String>,
        private val onSelected: (String) -> Unit
    ) : RecyclerView.Adapter<EmojiAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvEmoji: TextView = view.findViewById(R.id.tvEmoji)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_emoji_picker, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val emoji = emojis[position]
            holder.tvEmoji.text = emoji
            holder.itemView.setOnClickListener { onSelected(emoji) }
        }

        override fun getItemCount() = emojis.size
    }

    companion object {
        val EXPENSE_EMOJIS = listOf(
            "🍔", "🏠", "🚗", "🛒", "👕", "💊", "🎬", "✈️", "📱", "📚", "🎁", "🔧",
            "🚉", "⛽", "🚲", "🍕", "☕", "🍺", "🏋️", "🐕", "🐈", "💐", "💈", "🧼",
            "🍴", "🍎", "🎮", "🔌", "🏠", "🏢", "🚌", "🚕", "🚃", "🚲", "🧥", "👞",
            "🎭", "🎨", "🎤", "🎷", "🎸", "🎓", "🖊️", "💻", "⌚", "📷", "💡", "🧹"
        )
        val INCOME_EMOJIS = listOf(
            "💰", "🏦", "📈", "💼", "💸", "🤑", "💳", "🏧", "🪙", "💵", "💴", "💶",
            "💎", "⚖️", "🏆", "🎰", "🎁", "🏗️", "🏢", "🏘️", "🚜", "🚚", "⚓", "⚡",
            "📈", "💹", "📊", "🏅", "🥇", "🥈", "🥉", "💰", "💵", "💸", "💳", "🧾",
            "🤝", "💼", "👔", "🏭", "🏪", "🚜", "🚢", "🚛", "⛏️", "🔌", "💻", "💾"
        )
    }
}