package eu.gillissen.rider.usersecrets

import com.intellij.psi.codeStyle.CommonCodeStyleSettings

internal fun CommonCodeStyleSettings.IndentOptions.createIndent(level: Int): String =
    if (USE_TAB_CHARACTER)
        "\t".repeat(level * (INDENT_SIZE / TAB_SIZE))
    else
        " ".repeat(level * INDENT_SIZE)