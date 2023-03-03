package net.trelent.document.services

import com.intellij.openapi.util.SystemInfo
import kotlin.collections.listOf
class ParserService{

    private val supportedLanguages = listOf("python", "csharp", "java", "javascript", "typescript");


    init{
        println("parser service")

        if(SystemInfo.isMac){

        }
        else{
            
        }


    }
}