package com.example.callradar.datamodels

import android.net.Uri

data class TelegramContact(
    val id: Long,
    val name: String,
    val phoneNumber: String?,
    val username: String?,
    val photoUri: Uri?,
    val isVerified: Boolean = false
)