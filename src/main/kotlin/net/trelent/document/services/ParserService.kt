package net.trelent.document.services

import ai.serenade.treesitter.Languages
import ai.serenade.treesitter.Parser
import ai.serenade.treesitter.Tree
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.ResourceUtil
import com.jetbrains.rd.util.string.printToString
import java.io.File
import net.trelent.document.helpers.SUPPORTED_LANGUAGES

class ParserService{

   private lateinit var parser: Parser;

    init{


        val prefix = "libjava-tree-sitter"
        val suffix: String = "." +(if (SystemInfo.isMac) "dylib" else "so")
        val input = ResourceUtil.getResourceAsStream(javaClass.classLoader, "/grammars/", "$prefix$suffix");
        val tempFile: File = File.createTempFile(prefix, suffix);
        val bytes: ByteArray = input.readAllBytes()
        tempFile.writeBytes(bytes);
        System.load(tempFile.path);

        try{
            parser = Parser();
        }
        catch(e: Error){
            println("Error creating parser: ${e.stackTrace}")
        }

        println("Parser service init finished")
        println("Path to temp file: ${tempFile.path}")

    }

    fun parseDocument(text: String, lang: String){
        if(!SUPPORTED_LANGUAGES.contains(lang)){
            //TODO: Handle fail
            return;
        }
        if(!this::parser.isInitialized){
            try{
                parser = Parser()
            }
            catch(e: Error){
                println("Error creating parser: ${e.stackTrace}")
                return
            }
        }

        //Can return because supported languages checked above
        val language: Long = when (lang){
            "csharp" -> Languages.cSharp()
            "java" -> Languages.java()
            "javascript" -> Languages.javascript()
            "python" -> Languages.python()
            "typescript" -> Languages.typescript()
            else -> return
        }

        println(lang);

        try{
            println("Pre-parse language = $language")
            parser.setLanguage(language);
            println("Post language change")
            val tree: Tree = parser.parseString(text);
            println("Finished")
            try{
                println(tree.rootNode.nodeString);
            }
            catch(e: Error){
                println("Error parsing tree: ${e.stackTrace}")
            }

        }
        catch(e: Error){
            println("Error parsing tree: ${e.stackTrace}")
            return
        }

    }

}