package org.bxkr.octodiary

/** Reads the current text from the system clipboard, or null if none/not text. */
expect fun getClipboardText(): String?

/** Writes [text] to the system clipboard. */
expect fun setClipboardText(text: String)