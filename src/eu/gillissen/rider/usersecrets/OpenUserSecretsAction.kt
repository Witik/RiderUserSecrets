package eu.gillissen.rider.usersecrets

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DefaultProjectFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class OpenUserSecretsAction : AnAction() {

    var chosenProject: Project = DefaultProjectFactory.getInstance().defaultProject
    var id: String = ""

    override fun actionPerformed(actionEvent: AnActionEvent) {
        val dirs = "${System.getenv("APPDATA")}\\microsoft\\UserSecrets\\$id"
        val path = "$dirs\\secrets.json"
        val file = File(path)
        if (!file.exists()) {
            File(dirs).mkdirs()
            file.createNewFile()
        }
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        if (virtualFile != null) {
//            val fileDescriptor = OpenFileDescriptor(chosenProject, virtualFile)
//            fileDescriptor.navigate(true);
            FileEditorManager.getInstance(chosenProject).openFile(virtualFile, true)
        }
    }

    override fun update(actionEvent: AnActionEvent) {
        // In this method, we decide whether our action is shown in the current context or not.
        // We should only be visible when:
        // - A project is loaded
        // - An editor is open
        // - A PsiElement is available
        //      (current PsiElement is a full File instead of a syntax tree we can reason about, but good to check)

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

//        val editor = e.getData(DataKeys.EDITOR)
//        val psiElement = e.getData(DataKeys.PSI_ELEMENT)
//
//        if (editor == null || psiElement == null || editor.document.textLength == 0) {
//            e.presentation.isEnabledAndVisible = false
//            return
//        }
//
//        e.presentation.isEnabledAndVisible = true
    }
}