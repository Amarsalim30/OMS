package com.zeynbakers.order_management_system.core.navigation

import android.content.ClipData
import android.content.Intent

fun extractSharedText(intent: Intent): String? {
    if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_SEND_MULTIPLE) return null
    val type = intent.type
    if (type != null && !type.startsWith("text/")) return null

    val parts = linkedSetOf<String>()

    val clipData: ClipData? = intent.clipData
    if (clipData != null) {
        for (index in 0 until clipData.itemCount) {
            val item = clipData.getItemAt(index)
            val text = item.text?.toString()?.trim().orEmpty()
            if (text.isNotBlank()) parts.add(text)
        }
    }

    val singleText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim().orEmpty()
    if (singleText.isNotBlank()) {
        parts.add(singleText)
    }

    val listText = intent.getCharSequenceArrayListExtra(Intent.EXTRA_TEXT)
    if (!listText.isNullOrEmpty()) {
        listText.mapNotNull { it?.toString()?.trim() }
            .filter { it.isNotBlank() }
            .forEach { parts.add(it) }
    }

    return parts.joinToString(separator = "\n\n").trim().ifBlank { null }
}
