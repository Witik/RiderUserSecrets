package eu.gillissen.rider.usersecrets

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rdclient.util.idea.toIOFile
import com.jetbrains.rider.run.environment.MSBuildEvaluator
import java.io.File
import java.util.concurrent.TimeUnit

object UserSecretsService {
    private val supportedFileExtensions = arrayOf("csproj", "vbproj", "fsproj")
    private val supportedFileNames = arrayOf("Directory.Build.props", "Directory.Build.targets")

    const val UserSecretsIdMsBuildProperty = "UserSecretsId"

    fun isUserSecretsToolInstalled(): Boolean {
        val output = Runtime.getRuntime().exec("dotnet user-secrets --version")
        output.waitFor()

        return output.errorStream.readAllBytes().isEmpty()
    }

    fun initUserSecrets(projectFile: VirtualFile): Int {
        val projectIoFile = projectFile.toIOFile()
        val fullProjectFilePath = projectIoFile.absolutePath
        val projectDirectory = projectIoFile.absoluteFile.parentFile
        val output = Runtime.getRuntime().exec("dotnet user-secrets init --project \"${fullProjectFilePath}\"", null, projectDirectory)
        output.waitFor()

        return output.exitValue()
    }

    fun getMsbuildUserSecretsIdValue(project: Project, projectFile: VirtualFile): String? {
        val msBuildEvaluator = MSBuildEvaluator.getInstance(project)
        val msBuildProperties = msBuildEvaluator
            .evaluateProperties(MSBuildEvaluator.PropertyRequest(projectFile.path, null, listOf(UserSecretsService.UserSecretsIdMsBuildProperty)))
            .blockingGet(1, TimeUnit.MINUTES)
            ?: return null

        return msBuildProperties[UserSecretsService.UserSecretsIdMsBuildProperty]
    }

    fun getUserSecretsDirectoryRoot(): String {
        return if (SystemInfo.isWindows)
            "${System.getenv("APPDATA")}${File.separatorChar}microsoft${File.separatorChar}UserSecrets${File.separatorChar}"
        else
            "${System.getenv("HOME")}${File.separatorChar}.microsoft${File.separatorChar}usersecrets${File.separatorChar}"
    }

    fun isActionSupported(actionEvent: AnActionEvent): Boolean {
        val project = actionEvent.getActionProject()
        if (project == null || project.isDefault) {
            return false
        }

        val projectFile = actionEvent.getActionProjectFile()
        if (projectFile == null || !UserSecretsService.isActionSupportedForFile(projectFile)) {
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