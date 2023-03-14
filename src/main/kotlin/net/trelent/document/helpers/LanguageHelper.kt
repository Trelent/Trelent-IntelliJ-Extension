package net.trelent.document.helpers

var languageMap = mapOf(
    "cs" to "csharp",
    "cshtml" to "csharp",
    "cake" to "csharp",
    "csx" to "csharp",
    "java" to "java",
    "js" to "javascript",
    "es" to "javascript",
    "es6" to "javascript",
    "gs" to "javascript",
    "ts" to "typescript",
    "py" to "python"
)

fun getExtensionLanguage(ext: String) : String? {
    return languageMap[ext]
}