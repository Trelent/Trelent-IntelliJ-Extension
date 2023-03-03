package net.trelent.document.services

import com.intellij.openapi.util.SystemInfo
import com.intellij.util.ResourceUtil
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.collections.listOf
class ParserService{

    private val supportedLanguages = listOf("python", "csharp", "java", "javascript", "typescript");


    init{


        val prefix = "libjava-tree-sitter"
        val suffix: String = "." +(if (SystemInfo.isMac) "dylib" else "so")
        val input = ResourceUtil.getResourceAsStream(javaClass.classLoader, "/grammars/", "$prefix$suffix");
        val tempFile: File = File.createTempFile(prefix, suffix);
        val bytes: ByteArray = input.readAllBytes()
        tempFile.writeBytes(bytes);
        System.load(tempFile.path);

        println("Parser service init finished")
        println(tempFile.path)

    }
}