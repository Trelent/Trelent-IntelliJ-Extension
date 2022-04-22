package net.trelent.document.helpers

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.arrayOf
import com.google.gson.Gson;
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.jetbrains.rd.util.printlnError
import org.jetbrains.annotations.Nullable


val SUPPORTED_LANGUAGES = arrayOf<String>("csharp", "java", "javascript", "python")
private const val PARSE_CURRENT_FUNCTION_URL = "https://lambda.trelent.net/api/v4/ParseCurrent/ParseCurrentFunction"
private const val PARSE_FUNCTIONS_URL        = "https://lambda.trelent.net/api/v4/ParseAll/ParseSourceCode"
private const val WRITE_DOCSTRING_URL        = "https://prod-api.trelent.net/docs/docstring"

const val LOGIN_URL = "https://prod-api.trelent.net/auth/login?mode=login"
const val LOGOUT_URL = "https://prod-api.trelent.net/auth/logout"
const val SIGNUP_URL = "https://prod-api.trelent.net/auth/login?mode=signup"

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

data class DocstringResponse(
    var data: Docstring?,
    var error: String,
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

fun getDocstring(format: String, language: String, name: String, params: Array<String>, sender: String, snippet: String, user: String) : DocstringResponse {
    val req = DocstringRequest(format, FunctionRequest(snippet, name, params), language, sender, user)
    val body = Gson().toJson(req)

    // Check if the user is authenticated and use their token if so
    val attributes = CredentialAttributes("trelent-document-auth-token", "trelent-document-auth-token")
    val credentials = PasswordSafe.instance.get(attributes)
    if(credentials != null) {
        val token = credentials.getPasswordAsString()
        if(token != null && token != "") {
            return try {
                val returned = sendRequest(body, WRITE_DOCSTRING_URL, token)
                Gson().fromJson(returned, DocstringResponse::class.java)
            } catch (e: Exception) {
                DocstringResponse(null, e.message.toString(), false)
            }
        }
    }


    return try {
        val returned = sendRequest(body, WRITE_DOCSTRING_URL)
        Gson().fromJson(returned, DocstringResponse::class.java)
    } catch (e: Exception) {
        DocstringResponse(null, e.message.toString(), false)
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
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}