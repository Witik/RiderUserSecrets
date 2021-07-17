package eu.gillissen.rider.usersecrets

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.xml.parsers.DocumentBuilderFactory

internal fun AnActionEvent.getActionProjectFile(): VirtualFile? =
    getData(PlatformDataKeys.VIRTUAL_FILE)

internal fun AnActionEvent.getActionProject(): Project? =
    CommonDataKeys.PROJECT.getData(dataContext)

internal fun AnActionEvent.getXmlUserSecretsIdValue(): String? {
    val projectFile = getActionProjectFile()
    val document = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(projectFile!!.inputStream)
    document.documentElement.normalize()

    val nodes = document.getElementsByTagName(SharedConstants.UserSecretsIdMsBuildProperty)

    if (nodes.length == 0) {
        return null
    }

    val node = nodes.item(0)

    return node.textContent
}
