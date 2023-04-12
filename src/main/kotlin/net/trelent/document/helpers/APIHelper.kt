package net.trelent.document.helpers

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
import java.util.*


val SUPPORTED_LANGUAGES = arrayOf<String>("csharp", "java", "javascript", "python", "typescript")
private const val PARSE_URL = "https://code-parsing-server.fly.dev/parse"
private const val VERSION_CHECK_URL          = "https://code-parsing-server.fly.dev/"

// Prod Api
//private const val WRITE_DOCSTRING_URL        = "https://prod-api.trelent.net/docs/docstring"


// Dev Api
//private const val WRITE_DOCSTRING_URL        = "https://dev-api.trelent.net/docs/docstring"


// Local Api
private const val WRITE_DOCSTRING_URL        = "http://localhost:8000/docs/docstring"

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

data class Function(
    var body: String,
    var definition: String,
    var definition_line: Int,
    var docstring: String?,
    var docstring_offset: Int,
    var docstring_range_offsets: Array<Int>?,
    var name: String,
    var params: Array<String>,
    var offsets: Array<Int>,
    var text: String
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Function

        return offsets[0] == other.offsets[0];
    }

    override fun hashCode(): Int {
        return offsets.contentHashCode()
    }
}

data class VersionReturn(
    var version: String
)

fun getDocstring(format: String, language: String, name: String, params: Array<String>, sender: String, snippet: String, user: String) : DocstringResponse {
    val req = DocstringRequest(format, FunctionRequest(snippet, name, params), language, sender, user)
    val body = Gson().toJson(req)

    var returned: Optional<HttpResponse<String>> = Optional.empty();
    return try {
        returned = Optional.of(sendRequest(body, WRITE_DOCSTRING_URL))
        Gson().fromJson(returned.get().body(), DocstringResponse::class.java)
    } catch (e: Exception) {
        val errorType = if(returned.isPresent) returned.get().statusCode().toString() else "internal_error";
        DocstringResponse(null, e.message.toString(), errorType,false)
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

fun parseFunctions(language: String, source: String): Array<Function> {
    val req = ParsingRequest(language = language, source = source)
    val body = Gson().toJson(req)

    try {
        val returned = sendRequest(body, PARSE_URL).body()
        return Gson().fromJson(returned, Array<Function>::class.java)
    } catch (e: Exception) {
        printlnError(e.message.toString())
    }

    return arrayOf()
}

fun sendRequest(body: String, url: String, token: String = ""): HttpResponse<String> {
    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .setHeader("Authorization", "Bearer $token")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .version(HttpClient.Version.HTTP_1_1)
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response
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