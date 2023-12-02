package com.github.sblundy.changelistprotocol

import com.google.gson.stream.JsonWriter
import com.intellij.navigation.ProtocolOpenProjectResult
import com.intellij.navigation.openProject
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ChangeListManagerEx
import com.intellij.openapi.vcs.changes.LocalChangeList

internal sealed class ReadTarget<P : Params> {
    suspend fun execute(parameters: P, write: JsonWriter): TargetResult =
            withProject(parameters) { project -> doExecute(project, parameters, write) }

    abstract fun doExecute(project: Project, parameters: P, write: JsonWriter): TargetResult

    data object ListChangelists : ReadTarget<Params>() {
        override fun doExecute(project: Project, parameters: Params, write: JsonWriter): TargetResult {
            val clmgr = project.getChangelistManager()
            write.beginObject()
            write.name("changelists")
            write.beginArray()
            clmgr.changeLists.forEach {
                it.write(write)
            }
            write.endArray()
            write.endObject()
            return TargetResult.Success
        }
    }

    data object GetChangelist : ReadTarget<ChangelistParams>() {
        override fun doExecute(project: Project, parameters: ChangelistParams, write: JsonWriter): TargetResult =
                parameters.withChangelist(project) { _, _, list ->
                    list.write(write)
                    TargetResult.Success
                }
    }
}

internal sealed class WriteTarget<P : Params> {
    suspend fun execute(parameters: P): TargetResult =
            withProject(parameters) { project -> doExecute(project, parameters) }

    abstract fun doExecute(project: Project, parameters: P): TargetResult

    data object AddTarget : WriteTarget<AddParams>() {
        override fun doExecute(project: Project, parameters: AddParams): TargetResult =
                parameters.payload.name?.let { name ->
                    val clmgr = project.getChangelistManager()
                    val list = clmgr.addChangeList(name, parameters.payload.comment)
                    if (parameters.payload.active != false) {
                        clmgr.defaultChangeList = list
                    }
                    TargetResult.Success
                }?: TargetResult.MissingParameter("name")
    }

    data object ActivateTarget : WriteTarget<ActivateParams>() {
        override fun doExecute(project: Project, parameters: ActivateParams): TargetResult {
            return if (parameters.default == true) {
                LocalChangeList.getDefaultName()
            } else {
                parameters.name
            }?.let { name ->
                val clmgr = project.getChangelistManagerEx()
                clmgr.findChangeList(name)?.let { list ->
                    clmgr.setDefaultChangeList(list, true)
                    return TargetResult.Success
                } ?: TargetResult.ChangelistNotFound(name)
            } ?: TargetResult.MissingParameter("name")
        }
    }

    data object EditTarget : WriteTarget<EditParams>() {
        override fun doExecute(project: Project, parameters: EditParams): TargetResult =
                parameters.withChangelist(project) { name, clmgr, list ->
                    if (parameters.payload.active == false) {
                        return@withChangelist TargetResult.DeactivateNotPermitted
                    }
                    if (parameters.payload.active != false) {
                        clmgr.setDefaultChangeList(list, true)
                    }
                    parameters.payload.comment?.let {
                        clmgr.editComment(name, it)
                    }
                    parameters.payload.newName?.let {
                        clmgr.editName(name, it)
                    }
                    TargetResult.Success
                }
    }

    data object RemoveTarget : WriteTarget<ChangelistParams>() {
        override fun doExecute(project: Project, parameters: ChangelistParams): TargetResult =
                parameters.withChangelist(project) { _, clmgr, list ->
                    if (list.isDefault) {
                        return@withChangelist TargetResult.DeleteNotPermitted
                    }
                    clmgr.removeChangeList(list)
                    return@withChangelist TargetResult.Success
                }
    }
}

private suspend fun withProject(parameters: Params, doExecute: (project: Project) -> TargetResult): TargetResult {
    val project = when (val result = openProject(mapOf("project" to parameters.project))) {
        is ProtocolOpenProjectResult.Success -> result.project
        is ProtocolOpenProjectResult.Error -> return TargetResult.ProjectNotFound(parameters.project, result.message)
    }

    val clm = project.getChangelistManager()
    if (!clm.areChangeListsEnabled()) {
        return TargetResult.ChangelistNotEnabled
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

internal open class Params(val project: String?)

internal open class ChangelistParams(project: String?, val name: String?) : Params(project) {
    constructor(parameters: Map<String, String?>) : this(parameters["project"], parameters["name"])

    private fun withName(f: (name: String) -> TargetResult): TargetResult =
            name?.let { name -> return f(name) } ?: TargetResult.MissingParameter("name")

    fun withChangelist(project: Project, f: (name: String, clmgr: ChangeListManagerEx, list: LocalChangeList) -> TargetResult): TargetResult =
            withName { name ->
                val clmgr = project.getChangelistManagerEx()
                clmgr.findChangeList(name)?.let { list ->
                    return@withName f(name, clmgr, list)
                } ?: TargetResult.ChangelistNotFound(name)
            }
}

internal open class AddParams(project: String?, val payload: Payload) : Params(project) {
    constructor(parameters: Map<String, String?>) : this(parameters["project"],
            Payload(parameters["name"], parameters["comment"], parameters["active"]?.toBoolean()))
    data class Payload(val name: String?, val comment: String?, val active: Boolean?)
}

internal class ActivateParams(parameters: Map<String, String?>) : ChangelistParams(parameters) {
    var default: Boolean? = parameters["default"]?.toBoolean()
}

internal class EditParams(project: String?, name: String?, val payload: Payload) : ChangelistParams(project, name) {
    constructor(parameters: Map<String, String?>) :
            this(parameters["project"], parameters["name"], Payload(parameters["comment"], parameters["active"]?.toBoolean(), parameters["new-name"]))

    data class Payload(val comment: String?, val active: Boolean?, val newName: String?)
}

private fun Project.getChangelistManager(): ChangeListManager = ChangeListManager.getInstance(this)
private fun Project.getChangelistManagerEx(): ChangeListManagerEx = ChangeListManagerEx.getInstanceEx(this)

internal sealed interface TargetResult {
    fun getOrNull(): String?
    data object Success: TargetResult {
        override fun getOrNull(): String? = null
    }

    sealed interface ErrorTargetResult: TargetResult {
        override fun getOrNull(): String
    }

    data object ChangelistNotEnabled: ErrorTargetResult {
        override fun getOrNull(): String = MyBundle.message("jb.protocol.changelist.not.enabled")
    }

    data object DeactivateNotPermitted : ErrorTargetResult {
        override fun getOrNull(): String = MyBundle.message("jb.protocol.changelist.disabled.not.permitted")
    }

    data object DeleteNotPermitted : ErrorTargetResult {
        override fun getOrNull(): String = MyBundle.message("jb.protocol.changelist.delete.not.permitted")
    }

    data class ProjectNotFound(val name: String?, val message: String): ErrorTargetResult {
        override fun getOrNull(): String = MyBundle.message("jb.protocol.changelist.project.not.found", name?:"null", message)
    }
    data class ChangelistNotFound(val name: String): ErrorTargetResult {
        override fun getOrNull(): String = MyBundle.message("jb.protocol.changelist.not.found", name)
    }
    data class MissingParameter(val param: String): ErrorTargetResult {
        override fun getOrNull(): String = MyBundle.message("jb.protocol.changelist.parameter.required", param)
    }
}
