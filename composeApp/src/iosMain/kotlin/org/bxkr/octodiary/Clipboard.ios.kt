package org.bxkr.octodiary

import platform.UIKit.UIPasteboard

actual fun getClipboardText(): String? = UIPasteboard.generalPasteboard.string

actual fun setClipboardText(text: String) {
    UIPasteboard.generalPasteboard.string = text
}