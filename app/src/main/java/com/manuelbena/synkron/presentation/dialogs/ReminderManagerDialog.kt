package com.manuelbena.synkron.presentation.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.DialogAddCustomReminderBinding
import com.manuelbena.synkron.databinding.DialogReminderManagerBinding
import com.manuelbena.synkron.presentation.dialogs.adapter.ReminderManagerAdapter
import com.manuelbena.synkron.presentation.models.ReminderItem
import com.manuelbena.synkron.presentation.models.ReminderMethod

class ReminderManagerDialog(
    private val initialReminders: List<ReminderItem>,
    private val onRemindersConfirmed: (List<ReminderItem>) -> Unit
) : DialogFragment() {
}