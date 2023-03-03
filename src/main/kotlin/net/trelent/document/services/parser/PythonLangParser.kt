package net.trelent.document.services.parser

import ai.serenade.treesitter.Node
import ai.serenade.treesitter.Tree
import com.jetbrains.rd.util.string.printToString
import net.trelent.document.helpers.Function
import net.trelent.document.helpers.QueryGroup

class PythonLangParser: LangParser {

    /*

    const pythonFuncQuery = `
    (function_definition
      name: (identifier) @function.name
      parameters: (parameters) @function.params
      body: (block
        .
        (expression_statement
            (string) @function.docstring
          )?
      ) @function.body
    ) @function.def
    `;
     */

    init{

    }
    override fun filterTree(tree: Tree): List<QueryGroup> {
        val functions: Set<Node> = traverseNode(tree.rootNode, "function_definition", -1);
        val queryGroups: ArrayList<QueryGroup> = arrayListOf();
        for(node: Node in functions){
            try {
                var docNode: Array<Node> = arrayOf();
                var nameNode: Node? = null;
                var paramsNode: Node? = null;
                var bodyNode: Node? = null;
                if (node.childCount == 0) {
                    println("Node has 0 children, skipping")
                    continue
                }

                val cursor = node.walk();
                cursor.gotoFirstChild();
                do{
                    when (cursor.currentFieldName) {
                        "name" -> nameNode = cursor.currentNode
                        "parameters" -> paramsNode = cursor.currentNode
                        "body" -> bodyNode = cursor.currentNode
                        else -> println("Unexpected node ${cursor.currentNode.type}, skipping")
                    }
                } while(cursor.gotoNextSibling())
                cursor.close()
                if(nameNode == null || paramsNode == null || bodyNode == null){
                    println("Some nodes were missing, skipping")
                    continue
                }

                if(bodyNode.childCount > 0 && bodyNode.getChild(0).nodeString.equals("(expression_statement (string))")){
                    docNode = arrayOf(bodyNode.getChild(0).getChild(0));
                }
                queryGroups.add(QueryGroup(node, nameNode, paramsNode, bodyNode, docNode))

            } catch (e: Error) {
                println("Error parsing node with structure ${node.nodeString}");
            }

        }
        return queryGroups;
    }

    private fun traverseNode(node: Node, name: String, depth: Int): Set<Node>{
        val set: HashSet<Node> = HashSet<Node>();

        if(node.childCount == 0){
            return setOf();
        }

        for(i in 0 until node.childCount){
            if(node.getChild(i).type == name){
                set.add(node.getChild(i))
            }
            if(depth != 0){
                val childNodes = traverseNode(node.getChild(i), name, depth - 1);
                set.addAll(childNodes);
            }
        }
        return set;
    }

    override fun parseNodes(groups: List<QueryGroup>): List<Function> {
        println(groups.printToString())
        return listOf()
    }
}