@file:Suppress("UnstableApiUsage")

package eu.gillissen.rider.usersecrets

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.platform.util.application
import java.io.File

class OpenUserSecretsAction : AnAction() {

    override fun update(actionEvent: AnActionEvent) {
        actionEvent.presentation.isEnabledAndVisible = false

        if (!isActionSupported(actionEvent)) {
            return
        }

        actionEvent.presentation.isVisible = true

        // If Project doesn't contain <UserSecretsId>
        if (actionEvent.getXmlUserSecretsIdValue().isNullOrEmpty()) {
            return
        }

        actionEvent.presentation.isEnabledAndVisible = true
    }

    override fun actionPerformed(actionEvent: AnActionEvent) {
        if (!isActionSupported(actionEvent)) {
            return
        }

        val project = actionEvent.getActionProject()!!
        val projectFile = actionEvent.getActionProjectFile()!!

        object : Task.Backgroundable(project, "Retrieving user secrets...", true, DEAF) {

            override fun run(indicator: ProgressIndicator) {
                val secretsId = UserSecretsService.getMsbuildUserSecretsIdValue(project, projectFile) ?: return

                application.invokeLaterOnWriteThread {
                    val secretsDirectoryRoot = UserSecretsService.getUserSecretsDirectoryRoot()
                    val secretsDirectory = "$secretsDirectoryRoot${File.separatorChar}$secretsId"
                    val secretsFile = File("$secretsDirectory${File.separatorChar}secrets.json")
                    if (!secretsFile.exists()) {
                        File(secretsDirectory).mkdirs()
                        secretsFile.createNewFile()
                        secretsFile.writeText(
                            "{\n" +
                                    "//    \"MySecret\": \"ValueOfMySecret\"\n" +
                                    "}"
                        )
                    }

                    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(secretsFile)?.let {
                        FileEditorManager.getInstance(project).openFile(it, true)
                    }
                }
            }
        }.queue()
    }

    private fun isActionSupported(actionEvent: AnActionEvent): Boolean {
        val project = actionEvent.getActionProject()
        if (project == null || project.isDefault) {
            return false
        }

        val projectFile = actionEvent.getActionProjectFile()
        if (projectFile == null || !isActionSupportedForFile(projectFile)) {
            return false
        }

        return true
    }

    private fun isActionSupportedForFile(projectFile: VirtualFile?): Boolean {
        if (projectFile == null) return false

        return UserSecretsService.supportedFileExtensions.any { it.equals(projectFile.extension, ignoreCase = true) } ||
                UserSecretsService.supportedFileNames.any { it.equals(projectFile.name, ignoreCase = true) }
    }
}