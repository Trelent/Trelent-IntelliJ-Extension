package net.trelent.document.actions

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import net.trelent.document.helpers.AuthHelper
import net.trelent.document.helpers.SIGNUP_URL

class SignupNotificationAction(text: String) : NotificationAction(text) {

    override fun update(e: AnActionEvent) {
        // Using the event, evaluate the context, and enable or disable the action.
    }

    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
        var auth = AuthHelper()
        auth.start()

        BrowserUtil.browse(SIGNUP_URL)

        notification.hideBalloon()
    }
}