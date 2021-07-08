package eu.gillissen.rider.usersecrets

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rdclient.util.idea.toIOFile
import com.jetbrains.rider.run.environment.MSBuildEvaluator
import java.io.File
import java.util.concurrent.TimeUnit

object UserSecretsService {
    val supportedFileExtensions = arrayOf("csproj", "vbproj", "fsproj")
    val supportedFileNames = arrayOf("Directory.Build.props", "Directory.Build.targets")

    const val UserSecretsIdMsBuildProperty = "UserSecretsId"

    fun isUserSecretsToolInstalled(): Boolean {
        val output = Runtime.getRuntime().exec("dotnet user-secrets --version")
        output.waitFor()

        return output.errorStream.readAllBytes().isEmpty()
    }

    fun initUserSecrets(projectFile: VirtualFile): Int {
        val projectDirectory = projectFile.toIOFile().absoluteFile.parentFile
        val output = Runtime.getRuntime().exec("dotnet user-secrets init", null, projectDirectory)
        output.waitFor()

        return output.exitValue()
    }

    fun getMsbuildUserSecretsIdValue(project: Project, projectFile: VirtualFile): String? {
        val msBuildEvaluator = MSBuildEvaluator.getInstance(project)
        val msBuildProperties = msBuildEvaluator
            .evaluateProperties(MSBuildEvaluator.PropertyRequest(projectFile.path, null, listOf(UserSecretsService.UserSecretsIdMsBuildProperty)))
            .blockingGet(1, TimeUnit.MINUTES)
            ?: return null

        val secretsId = msBuildProperties[UserSecretsService.UserSecretsIdMsBuildProperty] ?: return null

        return secretsId
    }

    fun getUserSecretsDirectoryRoot(): String {
        return if (SystemInfo.isWindows)
            "${System.getenv("APPDATA")}${File.separatorChar}microsoft${File.separatorChar}UserSecrets${File.separatorChar}"
        else
            "${System.getenv("HOME")}${File.separatorChar}.microsoft${File.separatorChar}usersecrets${File.separatorChar}"
    }
}