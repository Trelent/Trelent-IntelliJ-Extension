package net.trelent.document.helpers

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress


class AuthHelper {
    private lateinit var server: HttpServer

    fun start() {
        val server : HttpServer = HttpServer.create(InetSocketAddress (54321), 0)
        server.createContext("/auth", authHandler())
        server.createContext("/loggedout", logoutHandler())
        server.executor = null
        server.start()
        this.server = server
    }

    fun stop() {
        if(server == null) {
            return
        }

        server.stop(1)
    }

    private fun authHandler() : HttpHandler {
        return HttpHandler { exchange ->
            val requestURL = exchange.requestURI.toString()
            var token = requestURL.split("/")[2]
            var response = ""

            if(token.isEmpty()) {
                response = "<h1>Login failed.</h1><h3>Please close this window and try again.</h3>"
                println("Token was null, something went wrong!")
            }
            else {

                // Store the token in a credential provider
                val attributes = CredentialAttributes("trelent-document-auth-token", "trelent-document-auth-token")
                PasswordSafe.instance.set(attributes, Credentials("trelent-document-auth-token", token))

                response = "<h1>Login was successful!</h1><h3>You may close this window.</h3>"

            }

            exchange.sendResponseHeaders(200, response.length.toLong())
            val os = exchange.responseBody
            os.write(response.toByteArray())
            os.close()

            server.stop(0)
        }
    }

    private fun logoutHandler() : HttpHandler {
        return HttpHandler { exchange ->

            println("Logged out of Trelent.")

            val response = "<h1>Successfully logged out of Trelent!</h1><h3>You may close this window.</h3>"
            exchange.sendResponseHeaders(200, response.length.toLong())
            val os = exchange.responseBody
            os.write(response.toByteArray())
            os.close()

            server.stop(0)
        }
    }
}