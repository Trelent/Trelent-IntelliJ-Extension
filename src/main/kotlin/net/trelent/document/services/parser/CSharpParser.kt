package net.trelent.document.services.parser

import ai.serenade.treesitter.Node
import ai.serenade.treesitter.Tree
import net.trelent.document.helpers.Function
import net.trelent.document.helpers.QueryGroup

class CSharpParser: Parser {
    override fun filterTree(tree: Tree): List<QueryGroup> {
        TODO("Not yet implemented")
    }

    override fun parseNodes(groups: List<QueryGroup>): List<Function> {
        TODO("Not yet implemented")
    }
}