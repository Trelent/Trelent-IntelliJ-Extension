package net.trelent.document.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import net.trelent.document.helpers.AuthHelper
import net.trelent.document.helpers.LOGIN_URL

class LoginAction : AnAction() {

    override fun update(e: AnActionEvent) {
        // Using the event, evaluate the context, and enable or disable the action.
    }

    override fun actionPerformed(e: AnActionEvent) {
        var auth = AuthHelper()
        auth.start()

        BrowserUtil.browse(LOGIN_URL)
    }
}