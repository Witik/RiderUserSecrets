package eu.gillissen.rider.usersecrets

import com.intellij.psi.codeStyle.CommonCodeStyleSettings

internal fun CommonCodeStyleSettings.IndentOptions.createIndent(level: Int): String {
    val indentBase = if (USE_TAB_CHARACTER) "\t" else " "

    return indentBase.repeat(level * INDENT_SIZE)
}