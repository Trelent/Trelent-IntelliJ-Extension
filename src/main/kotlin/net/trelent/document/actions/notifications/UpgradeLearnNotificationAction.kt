package net.trelent.document.actions.notifications

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import net.trelent.document.helpers.getCheckoutURL
import net.trelent.document.helpers.showGenericError
import net.trelent.document.services.MyProjectService

class UpgradeLearnNotificationAction(text: String) : NotificationAction(text) {

    override fun update(e: AnActionEvent) {
        // Using the event, evaluate the context, and enable or disable the action.
    }

    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
        BrowserUtil.browse("https://trelent.net#pricing")

        notification.hideBalloon()
    }
}