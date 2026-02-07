package com.zeynbakers.order_management_system.core.navigation

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.zeynbakers.order_management_system.MainActivity
import com.zeynbakers.order_management_system.R

object AppShortcuts {
    private const val SHORTCUT_NEW_ORDER = "new_order"
    private const val SHORTCUT_TODAY = "today"
    private const val SHORTCUT_UNPAID = "unpaid"

    fun ensure(context: Context) {
        val shortcuts =
            listOf(
                buildShortcut(
                    context = context,
                    id = SHORTCUT_NEW_ORDER,
                    labelRes = R.string.shortcut_new_order,
                    action = AppIntents.ACTION_NEW_ORDER
                ),
                buildShortcut(
                    context = context,
                    id = SHORTCUT_TODAY,
                    labelRes = R.string.shortcut_today,
                    action = AppIntents.ACTION_SHOW_TODAY
                ),
                buildShortcut(
                    context = context,
                    id = SHORTCUT_UNPAID,
                    labelRes = R.string.shortcut_unpaid,
                    action = AppIntents.ACTION_SHOW_UNPAID
                )
            )
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    fun reportShortcutUsed(context: Context, action: String?) {
        val shortcutId =
            when (action) {
                AppIntents.ACTION_NEW_ORDER -> SHORTCUT_NEW_ORDER
                AppIntents.ACTION_SHOW_TODAY -> SHORTCUT_TODAY
                AppIntents.ACTION_SHOW_UNPAID -> SHORTCUT_UNPAID
                else -> null
            } ?: return
        ShortcutManagerCompat.reportShortcutUsed(context, shortcutId)
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
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher_foreground))
            .setIntent(intent)
            .build()
    }
}
