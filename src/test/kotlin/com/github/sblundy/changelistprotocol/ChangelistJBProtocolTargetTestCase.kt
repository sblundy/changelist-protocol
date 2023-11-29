package com.github.sblundy.changelistprotocol

import com.intellij.openapi.application.JBProtocolCommand
import kotlinx.coroutines.runBlocking

abstract class ChangelistJBProtocolTargetTestCase(private val target: String) : ChangelistTestCase() {
    @Suppress("UnstableApiUsage")
    protected fun targetTest(vararg parameters:Pair<String, String>, checkAction: (result: String?) -> Unit) {
        runBlocking {
            val query = parameters.asSequence().fold("project=${project.name}") { acc, e -> acc + "&${e.first}=${e.second}" }
            val result = JBProtocolCommand.execute("idea/changelist/${target}?${query}")
            checkAction(result)
        }
    }
}