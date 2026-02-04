package com.zeynbakers.order_management_system.core.navigation

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.zeynbakers.order_management_system.MainActivity
import com.zeynbakers.order_management_system.R

object AppShortcuts {
    fun ensure(context: Context) {
        val shortcuts =
            listOf(
                buildShortcut(
                    context = context,
                    id = "new_order",
                    labelRes = R.string.shortcut_new_order,
                    action = AppIntents.ACTION_NEW_ORDER
                ),
                buildShortcut(
                    context = context,
                    id = "today",
                    labelRes = R.string.shortcut_today,
                    action = AppIntents.ACTION_SHOW_TODAY
                ),
                buildShortcut(
                    context = context,
                    id = "unpaid",
                    labelRes = R.string.shortcut_unpaid,
                    action = AppIntents.ACTION_SHOW_UNPAID
                )
            )
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    private fun buildShortcut(
        context: Context,
        id: String,
        labelRes: Int,
        action: String
    ): ShortcutInfoCompat {
        val label = context.getString(labelRes)
        val intent =
            Intent(context, MainActivity::class.java).apply {
                this.action = action
            }
        return ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(intent)
            .build()
    }
}
