package com.github.sblundy.changelistprotocol

import com.intellij.ide.IdeBundle
import com.intellij.openapi.application.JBProtocolCommand

class ChangelistJBProtocol : JBProtocolCommand("changelist") {
    /**
     * The handler parses the following "changelist" commands and with these parameters:
     *
     * add\\?project=(?<project>[\\w]+)
     *   &name=(?<name>[\\w]+)
     *   (&active=(?<active>true|false))?
     *   (&comment=(?<comment>[\\w]+))?
     *
     * activate\\?project=(?<project>[\\w]+)
     *   (&name=(?<name>[\\w]+)?
     *   (&default=(?<default>true|false))?
     *
     * update\\?project=(?<project>[\\w]+)
     *   &name=(?<name>[\\w]+)
     *   (&new-name=(?<new-name>[\\w]+))?
     *   (&active=(?<active>true))?
     *   (&comment=(?<comment>[\\w]+))?
     *
     * remove\\?project=(?<project>[\\w]+)
     *   &name=(?<name>[\\w]+)
     */
    override suspend fun execute(target: String?, parameters: Map<String, String>, fragment: String?): String? = when (target) {
        null -> MyBundle.message("jb.protocol.changelist.target.required")
        "add" -> WriteTarget.AddTarget.execute(AddParams(parameters)).getOrNull()
        "activate" -> WriteTarget.ActivateTarget.execute(ActivateParams(parameters)).getOrNull()
        "update" -> WriteTarget.EditTarget.execute(EditParams(parameters)).getOrNull()
        "remove" -> WriteTarget.RemoveTarget.execute(ChangelistParams(parameters)).getOrNull()
        else -> IdeBundle.message("jb.protocol.unknown.target", target)
    }
}