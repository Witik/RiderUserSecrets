package eu.gillissen.rider.usersecrets

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task

class InitUserSecretsAction : AnAction() {
    override fun update(actionEvent: AnActionEvent) {
        actionEvent.presentation.isEnabledAndVisible = false

        if (!UserSecretsService.isActionSupported(actionEvent)) {
            return
        }

        actionEvent.presentation.isVisible = true

        // If Project already contains <UserSecretsId>
        if (!actionEvent.getXmlUserSecretsIdValue().isNullOrEmpty()) {
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

        object : Task.Backgroundable(project, "Adding user secrets...", true, DEAF) {
            override fun run(indicator: ProgressIndicator) {
                val toolInstalled = UserSecretsService.isUserSecretsToolInstalled()
                if (!toolInstalled) {
                    NotificationGroupManager.getInstance().getNotificationGroup("User Secrets Notification Group")
                        .createNotification("User Secrets global tool not found", NotificationType.ERROR)
                        .notify(project);
                    return
                }

                UserSecretsService.initUserSecrets(projectFile)
                projectFile.refresh(true, false)
            }
        }.queue()
    }
}