package org.bxkr.octodiary

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

actual fun getClipboardText(): String? {
    val context = MainApplication.instance
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = cm.primaryClip ?: return null
    return if (clip.itemCount > 0) clip.getItemAt(0).coerceToText(context).toString() else null
}

actual fun setClipboardText(text: String) {
    val context = MainApplication.instance
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("OctoDiary", text))
}