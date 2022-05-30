package net.trelent.document.actions.notifications

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import net.trelent.document.helpers.getCheckoutURL
import net.trelent.document.helpers.showGenericError
import net.trelent.document.services.MyProjectService

class UpgradeNotificationAction(text: String) : NotificationAction(text) {

    override fun update(e: AnActionEvent) {
        // Using the event, evaluate the context, and enable or disable the action.
    }

    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
        var service = e.project!!.getService(MyProjectService::class.java)
        var sessionURL = getCheckoutURL(service.port)

        if(sessionURL != null) {
            BrowserUtil.browse(sessionURL)
        }
        else {
            showGenericError("Account Upgrade Failed", "Please login, then try again.", e.project!!)
        }

        notification.hideBalloon()
    }
}