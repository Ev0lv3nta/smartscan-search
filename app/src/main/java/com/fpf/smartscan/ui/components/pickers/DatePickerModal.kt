package com.fpf.smartscan.ui.components.pickers

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import java.util.Calendar

@Composable
fun DatePickerModal(
    show: Boolean,
    onDismiss: () -> Unit,
    onDateSelected: (year: Int, month: Int, day: Int) -> Unit,
    initialDateMillis: Long? = null

) {
    if (!show) return

    val calendar = Calendar.getInstance()
    val initialSelectedDateMillis = initialDateMillis ?: calendar.timeInMillis

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis
    )

    var hasUserSelected by remember { mutableStateOf(false) }

    LaunchedEffect(datePickerState.selectedDateMillis) {
        val selectedMillis = datePickerState.selectedDateMillis
        if (hasUserSelected && selectedMillis != null) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = selectedMillis
            }
            onDateSelected(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            onDismiss()
        }
        hasUserSelected = true
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface {
            DatePicker(
                state = datePickerState,
                showModeToggle = true
            )
        }
    }
}