package eu.gillissen.rider.usersecrets

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.jetbrains.rd.framework.impl.startAndAdviseSuccess
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.model.*
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.projectView.workspace.containingEntity
import com.jetbrains.rider.projectView.workspace.getId
import com.jetbrains.rider.run.environment.MSBuildEvaluator
import com.jetbrains.rider.util.idea.getPsiFile
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

object UserSecretsService {
    private val projectFileExtensions = arrayOf("csproj", "vbproj", "fsproj")
    private val sharedPropertiesFileNames = arrayOf("Directory.Build.props", "Directory.Build.targets")

    internal fun initUserSecrets(file: VirtualFile, project: Project) {
        if (projectFileExtensions.contains(file.extension)) {
            return initUserSecretsUsingMsBuild(file, project)
        }

        // Rider do not currently support editing of Directory.Build.* files through API
        if (sharedPropertiesFileNames.contains(file.name)) {
            return initUserSecretsUsingXml(file, project)
        }
    }

    internal fun getMsbuildUserSecretsIdValue(project: Project, file: VirtualFile): String? {
        val msBuildEvaluator = MSBuildEvaluator.getInstance(project)
        val msBuildProperties = msBuildEvaluator
            .evaluateProperties(MSBuildEvaluator.PropertyRequest(file.path, null, listOf(SharedConstants.UserSecretsIdMsBuildProperty)))
            .blockingGet(1, TimeUnit.MINUTES)
            ?: return null

        return msBuildProperties[SharedConstants.UserSecretsIdMsBuildProperty]
    }

    private fun getUserSecretsDirectoryRoot(): String {
        return if (SystemInfo.isWindows)
            "${System.getenv("APPDATA")}${File.separatorChar}microsoft${File.separatorChar}UserSecrets${File.separatorChar}"
        else
            "${System.getenv("HOME")}${File.separatorChar}.microsoft${File.separatorChar}usersecrets${File.separatorChar}"
    }

    internal fun isActionSupported(actionEvent: AnActionEvent): Boolean {
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

    private fun initUserSecretsUsingMsBuild(file: VirtualFile, project: Project) {
        val projectModelEntity = file.containingEntity(project) ?: return
        val projectId = projectModelEntity.getId(project) ?: return
        val secretsId = UUID.randomUUID().toString()
        val changeUserSecret = RdChangeProjectProperty(SharedConstants.UserSecretsIdMsBuildProperty, secretsId)
        val propertiesToUpdate = listOf(changeUserSecret)
        val command = RdChangeProjectPropertiesCommand(projectId, propertiesToUpdate)
        Lifetime.using { lifetime ->
            project.solution.projectModelTasks.changeProjectProperties.startAndAdviseSuccess(lifetime, command) {
                file.refresh(true, false)
            }
        }

        openUserSecrets(secretsId, project)
    }

    private fun initUserSecretsUsingXml(file: VirtualFile, project: Project) {
        val documentBuilderFactory = DocumentBuilderFactory.newInstance()
        val document = documentBuilderFactory
            .newDocumentBuilder()
            .parse(file.inputStream)

        val indentOptions = getIndentOptions(file, FileDocumentManager.getInstance(), PsiDocumentManager.getInstance(project))
        val propertyGroup = document.getOrCreatePropertyGroup(indentOptions)
        val secretsId = UUID.randomUUID().toString()
        document.insertUserSecrets(propertyGroup, secretsId, indentOptions)
        document.saveToFile(file.path)
        file.refresh(false, false)

        openUserSecrets(secretsId, project)
    }

    fun openUserSecrets(secretsId: String, project: Project) {
        val secretsDirectoryRoot = getUserSecretsDirectoryRoot()
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

    private fun getIndentOptions(file: VirtualFile, fileDocumentManager: FileDocumentManager, psiDocumentManager: PsiDocumentManager): CommonCodeStyleSettings.IndentOptions =
        CommonCodeStyleSettings.IndentOptions
            .retrieveFromAssociatedDocument(
                file.getPsiFile(
                    fileDocumentManager,
                    psiDocumentManager
                )!!
            ) ?: CommonCodeStyleSettings.IndentOptions.DEFAULT_INDENT_OPTIONS

    private fun isActionSupportedForFile(projectFile: VirtualFile?): Boolean {
        if (projectFile == null) return false

        return projectFileExtensions.any { it.equals(projectFile.extension, ignoreCase = true) } ||
                sharedPropertiesFileNames.any { it.equals(projectFile.name, ignoreCase = true) }
    }
}
