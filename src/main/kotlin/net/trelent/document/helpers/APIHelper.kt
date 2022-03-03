package net.trelent.document.helpers

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.arrayOf
import com.google.gson.Gson;
import com.jetbrains.rd.util.printlnError


val SUPPORTED_LANGUAGES = arrayOf<String>("csharp", "java", "javascript", "python")
private const val PARSE_CURRENT_FUNCTION_URL = "https://lambda.trelent.net/api/v4/ParseCurrent/ParseCurrentFunction"
private const val PARSE_FUNCTIONS_URL        = "https://lambda.trelent.net/api/v4/ParseAll/ParseSourceCode"
private const val WRITE_DOCSTRING_URL        = "https://trelent.npkn.net/write-docstring"

data class DocstringRequest(
    var language: String,
    var name: String,
    var params: Array<String>,
    var sender: String,
    var snippet: String,
    var user: String
)

data class ParsingRequest(
    var cursor: Array<Int>,
    var language: String,
    var source: String
)

data class Docstring(
    var docstring: String
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

fun getDocstring(language: String, name: String, params: Array<String>, sender: String, snippet: String, user: String) : Docstring? {
    val req = DocstringRequest(language, name, params, sender, snippet, user)
    val body = Gson().toJson(req)

    try {
        val returned = sendRequest(body, WRITE_DOCSTRING_URL)
        val result = Gson().fromJson<Docstring>(returned, Docstring::class.java)
        if(result.docstring != null) {
            return result
        }
    } catch (e: Exception) {
        printlnError(e.message.toString())
    }

    return null
}

fun parseCurrentFunction(cursor: Array<Int>, language: String, source: String): Function? {
    val req = ParsingRequest(cursor = cursor, language = language, source = source)
    val body = Gson().toJson(req)

    try {
        val returned = sendRequest(body, PARSE_CURRENT_FUNCTION_URL)
        val result = Gson().fromJson<FuncReturn>(returned, FuncReturn::class.java)
        if(result.success)
        {
            return result.current_function
        }
    } catch (e: Exception) {
        printlnError(e.message.toString())
    }

    return null
}

fun sendRequest(body: String, url: String): String? {
    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}