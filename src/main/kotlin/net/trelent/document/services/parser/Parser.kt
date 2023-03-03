package net.trelent.document.services.parser

import ai.serenade.treesitter.Node
import ai.serenade.treesitter.Tree
import net.trelent.document.helpers.Function
import net.trelent.document.helpers.QueryGroup

interface Parser {

    fun filterTree(tree: Tree): List<QueryGroup>;

    fun parseNodes(groups: List<QueryGroup>): List<Function>;
}