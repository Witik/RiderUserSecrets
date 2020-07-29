package eu.gillissen.rider.usersecrets

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DefaultProjectFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
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
                ?: return
        FileEditorManager.getInstance(chosenProject).openFile(virtualFile, true)
    }

    private fun isWindows(): Boolean {
        return System.getProperty("os.name").toLowerCase().contains("win")
    }

    private fun getSecretsDirectory(): String {
        return if (isWindows())
            "${System.getenv("APPDATA")}${File.separatorChar}microsoft${File.separatorChar}UserSecrets${File.separatorChar}$id"
        else
            "${System.getenv("HOME")}${File.separatorChar}.microsoft${File.separatorChar}usersecrets${File.separatorChar}$id"
    }

    override fun update(actionEvent: AnActionEvent) {
        val project = actionEvent.getData(PlatformDataKeys.PROJECT)

        if (project == null || project.isDefault) {
            actionEvent.presentation.isVisible = false
            actionEvent.presentation.isEnabled = false
            return
        }

        val file = actionEvent.getData(PlatformDataKeys.VIRTUAL_FILE)
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
        id = textContent

        actionEvent.presentation.isEnabledAndVisible = true
    }
}