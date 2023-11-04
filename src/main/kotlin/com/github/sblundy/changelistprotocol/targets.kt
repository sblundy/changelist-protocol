package com.github.sblundy.changelistprotocol

import com.google.gson.stream.JsonWriter
import com.intellij.ide.IdeBundle
import com.intellij.navigation.ProtocolOpenProjectResult
import com.intellij.navigation.openProject
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ChangeListManagerEx
import com.intellij.openapi.vcs.changes.LocalChangeList

internal sealed class ReadTarget<P : Params> {
    suspend fun execute(parameters: P, write: JsonWriter): String? =
            withProject(parameters) { project -> doExecute(project, parameters, write) }

    abstract fun doExecute(project: Project, parameters: P, write: JsonWriter): String?

    data object ListChangelists : ReadTarget<Params>() {
        override fun doExecute(project: Project, parameters: Params, write: JsonWriter): String? {
            val clmgr = project.getChangelistManager()
            write.beginObject()
            write.name("changelists")
            write.beginArray()
            clmgr.changeLists.forEach {
                it.write(write)
            }
            write.endArray()
            write.endObject()
            return null
        }
    }

    data object GetChangelist : ReadTarget<ChangelistParams>() {
        override fun doExecute(project: Project, parameters: ChangelistParams, write: JsonWriter): String? =
                parameters.withChangelist(project) { _, _, list ->
                    list.write(write)
                    null
                }
    }
}

internal sealed class WriteTarget<P : ChangelistParams> {
    suspend fun execute(parameters: P): String? =
            withProject(parameters) { project -> doExecute(project, parameters) }

    abstract fun doExecute(project: Project, parameters: P): String?

    data object AddTarget : WriteTarget<AddParams>() {
        override fun doExecute(project: Project, parameters: AddParams): String? =
                parameters.withName { name ->
                    val clmgr = project.getChangelistManager()
                    val list = clmgr.addChangeList(name, parameters.comment)
                    if (parameters.activate != false) {
                        clmgr.defaultChangeList = list
                    }
                    null
                }
    }

    data object ActivateTarget : WriteTarget<ActivateParams>() {
        override fun doExecute(project: Project, parameters: ActivateParams): String? {
            return if (parameters.default == true) {
                LocalChangeList.getDefaultName()
            } else {
                parameters.name
            }?.let { name ->
                val clmgr = project.getChangelistManagerEx()
                clmgr.findChangeList(name)?.let { list ->
                    clmgr.setDefaultChangeList(list, true)
                    return null
                } ?: MyBundle.message("jb.protocol.changelist.not.found", name)
            } ?: IdeBundle.message("jb.protocol.parameter.missing", "name")
        }
    }

    data object EditTarget : WriteTarget<EditParams>() {
        override fun doExecute(project: Project, parameters: EditParams): String? =
                parameters.withChangelist(project) { name, clmgr, list ->
                    if (parameters.activate != false) {
                        clmgr.setDefaultChangeList(list, true)
                    }
                    parameters.comment?.let {
                        clmgr.editComment(name, it)
                    }
                    parameters.newName?.let {
                        clmgr.editName(name, it)
                    }
                    null
                }
    }

    data object RemoveTarget : WriteTarget<ChangelistParams>() {
        override fun doExecute(project: Project, parameters: ChangelistParams): String? =
                parameters.withChangelist(project) { _, clmgr, list ->
                    clmgr.removeChangeList(list)
                    return@withChangelist null
                }
    }
}

private suspend fun withProject(parameters: Params, doExecute: (project: Project) -> String?): String? {
    val project = when (val result = openProject(mapOf("project" to parameters.project))) {
        is ProtocolOpenProjectResult.Success -> result.project
        is ProtocolOpenProjectResult.Error -> return result.message
    }

    val clm = project.getChangelistManager()
    if (!clm.areChangeListsEnabled()) {
        return MyBundle.message("jb.protocol.changelist.not.enabled")
    }

    return doExecute(project)
}

private fun LocalChangeList.write(write: JsonWriter) {
    write.beginObject()
    write.name("name").value(name)
    if (isDefault) {
        write.name("active").value(true)
    }
    if (isReadOnly) {
        write.name("readOnly").value(true)
    }
    write.name("comment").value(comment)
    write.endObject()
}

internal open class Params(parameters: Map<String, String?>) {
    var project: String? = parameters["project"]
}

internal open class ChangelistParams(parameters: Map<String, String?>) : Params(parameters) {
    constructor(project: String?, name: String?): this(mapOf("project" to project, "name" to name))

    var name: String? = parameters["name"]

    fun withName(f: (name: String) -> String?): String? =
            name?.let { name -> return f(name) } ?: IdeBundle.message("jb.protocol.parameter.missing", "name")

    fun withChangelist(project: Project, f: (name: String, clmgr: ChangeListManagerEx, list: LocalChangeList) -> String?): String? =
            withName { name ->
                val clmgr = project.getChangelistManagerEx()
                clmgr.findChangeList(name)?.let { list ->
                    return@withName f(name, clmgr, list)
                } ?: MyBundle.message("jb.protocol.changelist.not.found", name)
            }
}

internal open class AddParams(parameters: Map<String, String?>) : ChangelistParams(parameters) {
    var comment: String? = parameters["comment"]
    var activate: Boolean? = parameters["activate"]?.toBoolean()
}

internal class ActivateParams(parameters: Map<String, String?>) : AddParams(parameters) {
    var default: Boolean? = parameters["default"]?.toBoolean()
}

internal class EditParams(parameters: Map<String, String?>) : AddParams(parameters) {
    var newName: String? = parameters["new-name"]
}

internal fun Map<String, String?>.withName(name: String): Map<String, String?> = plus("name" to name)
internal fun Map<String, String?>.withProject(name: String): Map<String, String?> = plus("project" to name)
private fun Project.getChangelistManager(): ChangeListManager = ChangeListManager.getInstance(this)
private fun Project.getChangelistManagerEx(): ChangeListManagerEx = ChangeListManagerEx.getInstanceEx(this)
