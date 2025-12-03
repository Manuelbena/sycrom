package com.manuelbena.synkron.domain.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class RecurrenceType : Parcelable {
    DAILY,
    WEEKLY,
    CUSTOM,
    NONE
}



