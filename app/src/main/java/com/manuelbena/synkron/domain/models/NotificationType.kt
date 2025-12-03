package com.manuelbena.synkron.domain.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class NotificationType : Parcelable {
    ALARM,
   EMAIL,
   NOTIFICATION
}