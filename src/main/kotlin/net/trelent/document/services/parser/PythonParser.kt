package net.trelent.document.services.parser

import ai.serenade.treesitter.Node
import ai.serenade.treesitter.Tree
import net.trelent.document.helpers.Function
import net.trelent.document.helpers.QueryGroup

class PythonParser: Parser {

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
    override fun filterTree(tree: Tree): List<QueryGroup> {
        val functions: Set<Node> = traverseNode(tree.rootNode, "function_definition", -1);
        return listOf();
    }

    fun traverseNode(node: Node, name: String, depth: Int): Set<Node>{
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
        TODO("Not yet implemented")
    }
}