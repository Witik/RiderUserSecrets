package eu.gillissen.rider.usersecrets

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.jetbrains.rider.run.environment.MSBuildEvaluator
import java.io.File
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.SystemInfo
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

class OpenUserSecretsAction : AnAction() {

    companion object {
        private const val CsProjExtension = "csproj"
        private const val UserSecretsIdMsBuildProperty = "UserSecretsId"
    }

    override fun update(actionEvent: AnActionEvent) {

        val project = CommonDataKeys.PROJECT.getData(actionEvent.dataContext)

        if (project == null || project.isDefault) {
            actionEvent.presentation.isEnabledAndVisible = false
            return
        }

        val projectFile = actionEvent.getData(PlatformDataKeys.VIRTUAL_FILE)
        if (projectFile == null || CsProjExtension != projectFile.extension) {
            actionEvent.presentation.isEnabledAndVisible = false
            return
        }

        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(projectFile.inputStream)

        document.documentElement.normalize()
        val userSecretsIdNodes = document.getElementsByTagName(UserSecretsIdMsBuildProperty)
        if (userSecretsIdNodes.length == 0) {
            actionEvent.presentation.isEnabledAndVisible = false
            return
        }

        if (userSecretsIdNodes.item(0).textContent.isNullOrEmpty()) {
            actionEvent.presentation.isEnabled = false
            actionEvent.presentation.isVisible = true
            return
        }

        actionEvent.presentation.isEnabledAndVisible = true
    }

    override fun actionPerformed(actionEvent: AnActionEvent) {

        val project = CommonDataKeys.PROJECT.getData(actionEvent.dataContext) ?: return
        if (project.isDefault) return

        val projectFile = actionEvent.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return
        if (CsProjExtension != projectFile.extension) return

        object : Task.Backgroundable(project, "Retrieving user secrets...", true, DEAF) {

            override fun run(indicator: ProgressIndicator) {
                val msBuildEvaluator = MSBuildEvaluator.getInstance(project)
                val msBuildProperties = msBuildEvaluator
                    .evaluateProperties(MSBuildEvaluator.PropertyRequest(projectFile.path, null, listOf(UserSecretsIdMsBuildProperty)))
                    .blockingGet(1, TimeUnit.MINUTES)
                    ?: return

                val secretsId = msBuildProperties[UserSecretsIdMsBuildProperty] ?: return

                ApplicationManager.getApplication().invokeLaterOnWriteThread {
                    val secretsDirectoryRoot = getSecretsDirectoryRoot()
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

    private fun getSecretsDirectoryRoot(): String {
        return if (SystemInfo.isWindows)
            "${System.getenv("APPDATA")}${File.separatorChar}microsoft${File.separatorChar}UserSecrets${File.separatorChar}"
        else
            "${System.getenv("HOME")}${File.separatorChar}.microsoft${File.separatorChar}usersecrets${File.separatorChar}"
    }
}