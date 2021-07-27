@file:Suppress("UnstableApiUsage")

package eu.gillissen.rider.usersecrets

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.jetbrains.rd.platform.util.application

class OpenUserSecretsAction : AnAction() {

    override fun update(actionEvent: AnActionEvent) {
        actionEvent.presentation.isEnabledAndVisible = false

        if (!UserSecretsService.isActionSupported(actionEvent)) {
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
        if (!UserSecretsService.isActionSupported(actionEvent)) {
            return
        }

        val project = actionEvent.getActionProject()!!
        val projectFile = actionEvent.getActionProjectFile()!!

        object : Task.Backgroundable(project, "Retrieving user secrets...", true, DEAF) {

            override fun run(indicator: ProgressIndicator) {
                val secretsId = UserSecretsService.getMsbuildUserSecretsIdValue(project, projectFile) ?: return

                application.invokeLaterOnWriteThread {
                    UserSecretsService.openUserSecrets(secretsId, project)
                }
            }
        }.queue()
    }
}
