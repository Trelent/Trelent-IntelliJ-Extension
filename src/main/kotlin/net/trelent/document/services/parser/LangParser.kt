package net.trelent.document.services.parser

import ai.serenade.treesitter.Tree
import net.trelent.document.helpers.Function
import net.trelent.document.helpers.QueryGroup

interface LangParser {

    fun filterTree(tree: Tree): List<QueryGroup>;

    fun parseNodes(groups: List<QueryGroup>): List<Function>;
}