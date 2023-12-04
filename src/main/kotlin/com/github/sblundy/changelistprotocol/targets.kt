package com.github.sblundy.changelistprotocol

import com.google.gson.annotations.SerializedName
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
                    val clmgr = project.getChangelistManagerEx()
                    if (clmgr.findChangeList(name) != null) {
                        return@let TargetResult.DuplicateChangelist
                    }
                    val list = clmgr.addChangeList(name, parameters.payload.comment)
                    if (parameters.payload.active != false) {
                        clmgr.setDefaultChangeList(list, true)
                    }
                    TargetResult.Success
                } ?: TargetResult.MissingParameter("name")
    }

    data object EditTarget : WriteTarget<EditParams>() {
        override fun doExecute(project: Project, parameters: EditParams): TargetResult =
                parameters.withChangelist(project) { name, clmgr, list ->
                    RenameEditTarget.applyUpdate(parameters.payload, clmgr, list, name) ?: TargetResult.Success
                }
    }

    data object RenameEditTarget : WriteTarget<RenameEditParams>() {
        override fun doExecute(project: Project, parameters: RenameEditParams): TargetResult =
                parameters.payload.newName?.let { newName: String ->
                    parameters.withChangelist(project) { name, clmgr, list ->
                        when (val result = applyUpdate(parameters.payload, clmgr, list, name)) {
                            null -> {
                                if (clmgr.findChangeList(newName) != null) {
                                    return@withChangelist TargetResult.DuplicateChangelist
                                }
                                clmgr.editName(name, newName)
                                TargetResult.Success
                            }

                            else -> result
                        }
                    }
                } ?: TargetResult.MissingParameter("new-name")
    }

    internal fun applyUpdate(payload: ChangelistPayload, clmgr: ChangeListManagerEx, list: LocalChangeList, name: String): TargetResult? {
        if (payload.active == false) {
            return TargetResult.DeactivateNotPermitted
        }
        if (payload.active != false) {
            clmgr.setDefaultChangeList(list, true)
        }
        payload.comment?.let {
            clmgr.editComment(name, it)
        }
        return null
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

internal open class Params(val project: String)

internal open class ChangelistParams(project: String, val name: String) : Params(project) {
    fun withChangelist(project: Project, f: (name: String, clmgr: ChangeListManagerEx, list: LocalChangeList) -> TargetResult): TargetResult {
        val clmgr = project.getChangelistManagerEx()
        clmgr.findChangeList(name)?.let { list ->
            return f(name, clmgr, list)
        } ?: return TargetResult.ChangelistNotFound(name)
    }
}

internal sealed interface ChangelistPayload {
    val comment: String?
    val active: Boolean?
}

internal class ChangelistParamsWithPayload<P : ChangelistPayload>(project: String, name: String, val payload: P) : ChangelistParams(project, name)

internal class AddParams(project: String, val payload: Payload) : Params(project) {
    data class Payload(val name: String?, override val comment: String?, override val active: Boolean?) : ChangelistPayload
}

internal data class EditPayload(override val comment: String?, override val active: Boolean?) : ChangelistPayload
internal typealias EditParams = ChangelistParamsWithPayload<EditPayload>

internal typealias RenameEditParams = ChangelistParamsWithPayload<RenameEditPayload>
internal data class RenameEditPayload(@SerializedName("new-name") val newName: String?, override val comment: String?, override val active: Boolean?) : ChangelistPayload

private fun Project.getChangelistManager(): ChangeListManager = ChangeListManager.getInstance(this)
private fun Project.getChangelistManagerEx(): ChangeListManagerEx = ChangeListManagerEx.getInstanceEx(this)

internal sealed interface TargetResult {
    fun getOrNull(): String?

    data object Success : TargetResult {
        override fun getOrNull(): String? = null
    }

    sealed interface ErrorTargetResult : TargetResult {
        override fun getOrNull(): String
    }

    data object ChangelistNotEnabled : ErrorTargetResult {
        override fun getOrNull(): String = MyBundle.message("jb.protocol.changelist.not.enabled")
    }

    data object DeactivateNotPermitted : ErrorTargetResult {
        override fun getOrNull(): String = MyBundle.message("jb.protocol.changelist.disabled.not.permitted")
    }

    data object DeleteNotPermitted : ErrorTargetResult {
        override fun getOrNull(): String = MyBundle.message("jb.protocol.changelist.delete.not.permitted")
    }

    data object DuplicateChangelist : ErrorTargetResult {
        override fun getOrNull(): String = MyBundle.message("jb.protocol.changelist.duplicate.not.permitted")
    }

    data class ProjectNotFound(val name: String, val message: String) : ErrorTargetResult {
        override fun getOrNull(): String = MyBundle.message("jb.protocol.changelist.project.not.found", name, message)
    }

    data class ChangelistNotFound(val name: String) : ErrorTargetResult {
        override fun getOrNull(): String = MyBundle.message("jb.protocol.changelist.not.found", name)
    }

    data class MissingParameter(val param: String) : ErrorTargetResult {
        override fun getOrNull(): String = MyBundle.message("jb.protocol.changelist.parameter.required", param)
    }
}

internal val Map<String, String?>.project: String? get() = this["project"]

internal val Map<String, String?>.name: String? get() = this["name"]

internal val Map<String, String?>.newName: String? get() = this["new-name"]

internal val Map<String, String?>.comment: String? get() = this["comment"]

internal val Map<String, String?>.active: Boolean? get() = this["active"]?.toBoolean()

internal val Map<String, String?>.default: Boolean? get() = this["default"]?.toBoolean()
