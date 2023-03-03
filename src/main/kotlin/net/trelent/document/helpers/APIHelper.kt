package net.trelent.document.helpers

import ai.serenade.treesitter.Node
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.arrayOf
import com.google.gson.Gson
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.printlnError
import org.jetbrains.annotations.Nullable
import java.net.URLEncoder


val SUPPORTED_LANGUAGES = arrayOf<String>("csharp", "java", "javascript", "python", "typescript")
private const val PARSE_CURRENT_FUNCTION_URL = "https://lambda.trelent.net/api/v4/ParseCurrent/ParseCurrentFunction"
private const val VERSION_CHECK_URL          = "https://trelent.npkn.net/intellij-version-check"

// Prod Api
const val LOGIN_URL                          = "https://prod-api.trelent.net/auth/login?mode=login&port="
const val LOGOUT_URL                         = "https://prod-api.trelent.net/auth/logout?port="
const val SIGNUP_URL                         = "https://prod-api.trelent.net/auth/login?mode=signup&port="
private const val GET_CHECKOUT_URL           = "https://prod-api.trelent.net/billing/checkout?billing_plan=1"
private const val GET_PORTAL_URL             = "https://prod-api.trelent.net/billing/portal"
private const val WRITE_DOCSTRING_URL        = "https://prod-api.trelent.net/docs/docstring"
private var CHECKOUT_RETURN_URL              = "https://prod-api.trelent.net/redirect?redirect_url="
private var PORTAL_RETURN_URL                = "https://prod-api.trelent.net/redirect?redirect_url="


// Dev Api
//const val LOGIN_URL                          = "https://dev-api.trelent.net/auth/login?mode=login&port="
//const val LOGOUT_URL                         = "https://dev-api.trelent.net/auth/logout?port="
//const val SIGNUP_URL                         = "https://dev-api.trelent.net/auth/login?mode=signup&port="
//private const val GET_CHECKOUT_URL           = "https://dev-api.trelent.net/billing/checkout?billing_plan=1"
//private const val GET_PORTAL_URL             = "https://dev-api.trelent.net/billing/portal"
//private const val WRITE_DOCSTRING_URL        = "https://dev-api.trelent.net/docs/docstring"
//private var CHECKOUT_RETURN_URL              = "https://dev-api.trelent.net/redirect?redirect_url="
//private var PORTAL_RETURN_URL                = "https://dev-api.trelent.net/redirect?redirect_url="


// Local Api
//const val LOGIN_URL                          = "http://localhost:8000/auth/login?mode=login&port="
//const val LOGOUT_URL                         = "http://localhost:8000/auth/logout?port="
//const val SIGNUP_URL                         = "http://localhost:8000/auth/login?mode=signup&port="
//private const val GET_CHECKOUT_URL           = "http://localhost:8000/billing/checkout?billing_plan=1"
//private const val GET_PORTAL_URL             = "http://localhost:8000/billing/portal"
//private const val WRITE_DOCSTRING_URL        = "http://localhost:8000/docs/docstring"
//private var CHECKOUT_RETURN_URL              = "http://localhost:8000/redirect?redirect_url="
//private var PORTAL_RETURN_URL                = "http://localhost:8000/redirect?redirect_url="
data class FunctionRequest(
    val function_code: String,
    val function_name: String,
    @Nullable val function_params: Array<String>
)

data class DocstringRequest(
    var format: String,
    var function: FunctionRequest,
    var language: String,
    var sender: String,
    var user_id: String
)

data class ParsingRequest(
    var cursor: Array<Int>,
    var language: String,
    var source: String
)

data class SessionResponse(
    var success: Boolean,
    var session: String?
)

data class DocstringResponse(
    var data: Docstring?,
    var error: String,
    var error_type: String?,
    var successful: Boolean
)
data class Docstring(
    var col: Int,
    var docstring: String,
    var line: Int
)

data class FuncReturn(
    var success: Boolean,
    var current_function: Function
)

data class Function(
    var body: String,
    var definition: String,
    var docstring_point: Array<Int>,
    var hasDocstring: Boolean,
    var indentation: Int,
    var name: String,
    var params: Array<String>,
    var range: Array<Array<Int>>,
    var text: String
)

data class QueryGroup(
    var defNode: Node,
    var nameNode: Node,
    var paramsNode: Node,
    var bodyNode: Node,
    var docNodes: Array<Node>
)

data class VersionReturn(
    var version: String
)

fun getCheckoutURL(port: Int): String? {
    var url = "http://localhost:$port/checkout"
    var redirectURL = "$CHECKOUT_RETURN_URL${URLEncoder.encode(url, "UTF-8")}"
    var requestURL = "$GET_CHECKOUT_URL&return_url=$redirectURL"

    val token = getToken()
    if(token == null || token == "") {
        return null
    }

    return try {
        val returned = sendAuthenticatedGetRequest(requestURL, token)
        Gson().fromJson(returned, SessionResponse::class.java).session
    } catch(e: Exception) {
        null
    }
}

fun getPortalURL(port: Int): String? {
    var url = "http://localhost:$port/portal"
    var redirectURL = "$PORTAL_RETURN_URL${URLEncoder.encode(url, "UTF-8")}"
    var requestURL = "$GET_PORTAL_URL?return_url=$redirectURL"

    val token = getToken()
    if(token == null || token == "") {
        return null
    }

    return try {
        val returned = sendAuthenticatedGetRequest(requestURL, token)
        Gson().fromJson(returned, SessionResponse::class.java).session
    } catch(e: Exception) {
        null
    }
}

fun getDocstring(format: String, language: String, name: String, params: Array<String>, sender: String, snippet: String, user: String) : DocstringResponse {
    val req = DocstringRequest(format, FunctionRequest(snippet, name, params), language, sender, user)
    val body = Gson().toJson(req)

    // Check if the user is authenticated and use their token if so
    val token = getToken()
    if(token != null && token != "") {
        return try {
            val returned = sendRequest(body, WRITE_DOCSTRING_URL, token)
            Gson().fromJson(returned, DocstringResponse::class.java)
        } catch (e: Exception) {
            DocstringResponse(null, e.message.toString(), "internal_error", false)
        }
    }

    return try {
        val returned = sendRequest(body, WRITE_DOCSTRING_URL)
        Gson().fromJson(returned, DocstringResponse::class.java)
    } catch (e: Exception) {
        DocstringResponse(null, e.message.toString(), "internal_error",false)
    }
}

fun getLatestVersion(): String? {
    return try {
        val returned = sendGetRequest(VERSION_CHECK_URL)
        val result = Gson().fromJson(returned, VersionReturn::class.java)
        result.version
    } catch (e: Exception) {
        printlnError(e.message.toString())
        null
    }
}

fun parseCurrentFunction(cursor: Array<Int>, language: String, source: String): Function? {
    val req = ParsingRequest(cursor = cursor, language = language, source = source)
    val body = Gson().toJson(req)

    try {
        val returned = sendRequest(body, PARSE_CURRENT_FUNCTION_URL)
        val result = Gson().fromJson(returned, FuncReturn::class.java)
        if(result.success)
        {
            return result.current_function
        }
    } catch (e: Exception) {
        printlnError(e.message.toString())
    }

    return null
}

fun sendRequest(body: String, url: String, token: String = ""): String? {
    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .setHeader("Authorization", "Bearer $token")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .version(HttpClient.Version.HTTP_1_1)
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}

fun sendAuthenticatedGetRequest(url: String, token: String = ""): String {
    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .setHeader("Authorization", "Bearer $token")
        .GET()
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}

fun sendGetRequest(url: String): String {
    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .GET()
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}

fun showGenericError(title: String, message: String, project: Project) {
    val errNotification = Notification(
        "Trelent Error Notification Group",
        title,
        message,
        NotificationType.ERROR
    )

    Notifications.Bus.notify(errNotification, project)
}