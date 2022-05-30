package net.trelent.document.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import net.trelent.document.helpers.SIGNUP_URL
import net.trelent.document.helpers.getToken
import net.trelent.document.services.MyProjectService

class SignupAction : AnAction() {

    override fun update(e: AnActionEvent) {
        // Using the event, evaluate the context, and enable or disable the action.
        val hasToken = getToken() != null && getToken() != ""
        e.presentation.isEnabled = !hasToken
    }

    override fun actionPerformed(e: AnActionEvent) {
        var service = e.project!!.getService(MyProjectService::class.java)
        BrowserUtil.browse(SIGNUP_URL + service.port)
    }
}