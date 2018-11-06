package eu.gillissen.rider.usersecrets

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DefaultProjectFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.sun.javafx.PlatformUtil
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class OpenUserSecretsAction : AnAction() {

    private var chosenProject: Project = DefaultProjectFactory.getInstance().defaultProject
    private var id: String = ""

    override fun actionPerformed(actionEvent: AnActionEvent) {
        val dirs = getSecretsDirectory()
        val path = "$dirs${File.separatorChar}secrets.json"
        val file = File(path)
        if (!file.exists()) {
            File(dirs).mkdirs()
            file.createNewFile()
            file.writeText("{\n" +
                    "//    \"MySecret\": \"ValueOfMySecret\"\n" +
                    "}")
        }
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        if (virtualFile != null) {
            FileEditorManager.getInstance(chosenProject).openFile(virtualFile, true)
        }
    }

    private fun getSecretsDirectory(): String {
        if (PlatformUtil.isWindows())
            return "${System.getenv("APPDATA")}${File.separatorChar}microsoft${File.separatorChar}UserSecrets${File.separatorChar}$id"
        return "~${File.separatorChar}.microsoft${File.separatorChar}usersecrets${File.separatorChar}$id"
    }

    override fun update(actionEvent: AnActionEvent) {
        val project = actionEvent.getData(DataKeys.PROJECT)

        if (project == null || project.isDefault) {
            actionEvent.presentation.isVisible = false
            actionEvent.presentation.isEnabled = false
            return
        }

        val file = actionEvent.getData(DataKeys.VIRTUAL_FILE)
        if (file == null || "csproj" != file.extension) {
            actionEvent.presentation.isVisible = false
            actionEvent.presentation.isEnabled = false
            return
        }

        val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = documentBuilder.parse(file.inputStream)
        document.documentElement.normalize()
        val userSecretsIds = document.getElementsByTagName("UserSecretsId")
        var textContent: String? = null
        if (userSecretsIds.length > 0) {
            textContent = userSecretsIds.item(0).textContent
        }
        if (textContent.isNullOrEmpty()) {
            actionEvent.presentation.isEnabled = false
            return
        }

        chosenProject = project
        id = textContent!!

        actionEvent.presentation.isEnabledAndVisible = true
    }
}