package net.trelent.document.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import net.trelent.document.helpers.getPortalURL
import net.trelent.document.helpers.getToken
import net.trelent.document.helpers.showGenericError
import net.trelent.document.services.MyProjectService

class BillingPortalAction : AnAction() {

    override fun update(e: AnActionEvent) {
        // Using the event, evaluate the context, and enable or disable the action.
        val hasToken = getToken() != null && getToken() != ""
        e.presentation.isEnabled = hasToken
    }

    override fun actionPerformed(e: AnActionEvent) {
        val service = e.project!!.getService(MyProjectService::class.java)
        val portalURL = getPortalURL(service.port)

        if(portalURL != null) {
            BrowserUtil.browse(portalURL)
        }
        else {
            showGenericError("Billing Portal Failed", "Please login, then try again.", e.project!!)
        }
    }
}