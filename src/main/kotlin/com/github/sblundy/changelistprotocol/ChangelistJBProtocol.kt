package com.github.sblundy.changelistprotocol

import com.intellij.ide.IdeBundle
import com.intellij.navigation.ProtocolOpenProjectResult
import com.intellij.navigation.openProject
import com.intellij.openapi.application.JBProtocolCommand
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ChangeListManagerEx
import com.intellij.openapi.vcs.changes.LocalChangeList

class ChangelistJBProtocol : JBProtocolCommand("changelist") {
    /**
     * The handler parses the following "changelist" commands and with these parameters:
     *
     * add\\?project=(?<project>[\\w]+)
     *   &name=(?<name>[\\w]+)
     *   (&activate=(?<activate>true|false))?
     *   (&comment=(?<comment>[\\w]+))?
     *
     * activate\\?project=(?<project>[\\w]+)
     *   (&name=(?<name>[\\w]+)?
     *   (&default=(?<default>true|false))?
     *
     * remove\\?project=(?<project>[\\w]+)
     *   &name=(?<name>[\\w]+)
     */
    override suspend fun execute(target: String?, parameters: Map<String, String>, fragment: String?): String? {
        val project = when (val result = openProject(parameters)) {
            is ProtocolOpenProjectResult.Success -> result.project
            is ProtocolOpenProjectResult.Error -> return result.message
        }

        if (!ChangeListManager.getInstance(project).areChangeListsEnabled()) {
            return MyBundle.message("jb.protocol.changelist.not.enabled")
        }

        return when (target) {
            null -> MyBundle.message("jb.protocol.changelist.target.required")
            "add" -> addChangeList(project, parameters)
            "activate" -> activateChangeList(project, parameters)
            "remove" -> removeChangeList(project, parameters)
            else -> IdeBundle.message("jb.protocol.unknown.target", target)
        }
    }

    private fun addChangeList(project: Project, parameters: Map<String, String>): String? {
        return parameters["name"]?.let { name ->
            val clmgr = ChangeListManager.getInstance(project)
            val list = clmgr.addChangeList(name, parameters["comment"])
            if (parameters["activate"]?.toBoolean() != false) {
                clmgr.defaultChangeList = list
            }
            return null
        } ?: IdeBundle.message("jb.protocol.parameter.missing", "name")
    }

    private fun activateChangeList(project: Project, parameters: Map<String, String>): String? {
        return if (parameters["default"]?.toBoolean() == true) {
            LocalChangeList.getDefaultName()
        } else {
            parameters["name"]
        }?.let { name ->
            val clmgr = ChangeListManagerEx.getInstanceEx(project)
            clmgr.findChangeList(name)?.let { list ->
                clmgr.setDefaultChangeList(list, true)
                return null
            } ?: MyBundle.message("jb.protocol.changelist.not.found", name)
        } ?: IdeBundle.message("jb.protocol.parameter.missing", "name")
    }

    private fun removeChangeList(project: Project, parameters: Map<String, String>): String? {
        return parameters["name"]?.let { name ->
            val clmgr = ChangeListManager.getInstance(project)
            clmgr.findChangeList(name)?.let { list ->
                clmgr.removeChangeList(list)
                return null
            } ?: MyBundle.message("jb.protocol.changelist.not.found", name)
        } ?: IdeBundle.message("jb.protocol.parameter.missing", "name")
    }
}