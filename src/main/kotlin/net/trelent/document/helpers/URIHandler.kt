package net.trelent.document.helpers

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URL
import java.net.URLDecoder


class URIService {
    private lateinit var server: HttpServer

    fun start(): Int {
        val server = HttpServer.create(InetSocketAddress (0), 0)
        val port = server.address.port

        server.createContext("/auth", loginHandler())
        server.createContext("/loggedout", logoutHandler())
        server.createContext("/checkout", checkoutHandler())
        server.createContext("/portal", portalHandler())
        server.executor = null
        server.start()
        this.server = server
        return port
    }

    private fun getPort(): Int {
        return server.address.port
    }

    fun stop() {
        server.stop(1)
    }

    private fun loginHandler() : HttpHandler {
        return HttpHandler { exchange ->
            val requestURL = exchange.requestURI.toString()
            val token = requestURL.split("/")[2]
            val response: String

            if(token.isEmpty()) {
                response = "<h1>Login failed.</h1><h3>Please close this window and try again.</h3>"
                println("Token was null, something went wrong!")
                showError("Failed to login to Trelent. Please try again, or contact support at contact@trelent.net")
            }
            else {
                // Store the token in a credential provider
                setToken(token)

                response = "<h1>Login was successful!</h1><h3>You may close this window.</h3>"
                showNotification("Logged in to Trelent!", "You have been logged in to Trelent. You may log out at any time by clicking on Tools -> Trelent -> Logout.")

            }

            exchange.sendResponseHeaders(200, response.length.toLong())
            val os = exchange.responseBody
            os.write(response.toByteArray())
            os.close()
        }
    }

    private fun logoutHandler() : HttpHandler {
        return HttpHandler { exchange ->
            // Remove the token from our credential provider
            setToken("")

            val response = "<h1>Successfully logged out of Trelent!</h1><h3>You may close this window.</h3>"

            showNotification("Logged out of Trelent!", "You have been logged out of Trelent. You may log back in at any time by clicking on Tools -> Trelent -> Login/Signup.")

            exchange.sendResponseHeaders(200, response.length.toLong())
            val os = exchange.responseBody
            os.write(response.toByteArray())
            os.close()
        }
    }

    private fun checkoutHandler() : HttpHandler {
        return HttpHandler { exchange ->
            val compiledURL = URL("http", "localhost", getPort(), exchange.requestURI.path)
            val queryParams = splitQuery(compiledURL)
            val event = queryParams["event"]

            val message: String
            when (event) {
                null -> {
                    message = "Thank you for upgrading your account! Enjoy 1,000 docs/month, and more features " +
                            "coming every month! Please allow for up to 5 minutes for your account to be upgraded."
                }
                "upgrade" -> {
                    message = "Thank you for upgrading your account! Enjoy 1,000 docs/month, " +
                            "and more features coming every month!"
                }
                "cancel" -> {
                    message = "Your subscription has been cancelled. You will not be charged again. You will still " +
                            "get 100 free docs/month."
                }
                else -> {
                    message = "Your billing information has been updated."
                }
            }

            val response = "<h1>Successfully Updated your Account!</h1><p>$message</p><p>You may now close this window.</p>"

            showNotification("Trelent Account Updated!", message)

            exchange.sendResponseHeaders(200, response.length.toLong())
            val os = exchange.responseBody
            os.write(response.toByteArray())
            os.close()
        }
    }

    private fun portalHandler() : HttpHandler {
        return HttpHandler { exchange ->
            val response = "<h1>Successfully updated Trelent account!</h1><p>You may now close this window.</p>"

            showNotification("Trelent Account Updated!", "Your Trelent account has been updated successfully.")

            exchange.sendResponseHeaders(200, response.length.toLong())
            val os = exchange.responseBody
            os.write(response.toByteArray())
            os.close()
        }
    }
}

fun setToken(token: String) {
    val attributes = CredentialAttributes("trelent-document-auth-token", "trelent-document-auth-token")
    PasswordSafe.instance.set(attributes, Credentials("trelent-document-auth-token", token))
}

fun getToken() : String? {
    val attributes = CredentialAttributes("trelent-document-auth-token", "trelent-document-auth-token")
    val credentials = PasswordSafe.instance.get(attributes)
    return credentials?.getPasswordAsString()
}

fun showNotification(title: String, message: String)
{
    val errNotification = Notification(
        "Trelent Info Notification Group",
        title,
        message,
        NotificationType.INFORMATION
    )
    Notifications.Bus.notify(errNotification)
}

fun showError(message: String)
{
    val errNotification = Notification(
        "Trelent Error Notification Group",
        "Trelent error",
        message,
        NotificationType.ERROR
    )
    Notifications.Bus.notify(errNotification)
}

fun splitQuery(url: URL): Map<String, String> {
    val queryPairs: MutableMap<String, String> = LinkedHashMap()
    if(url.query == null) {
        return queryPairs
    }

    val query: String = url.query
    val pairs = query.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    for (pair in pairs) {
        val idx = pair.indexOf("=")
        queryPairs[URLDecoder.decode(pair.substring(0, idx), "UTF-8")] =
            URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
    }
    return queryPairs
}