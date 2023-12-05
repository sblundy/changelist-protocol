package com.github.sblundy.changelistprotocol

import com.intellij.ide.IdeBundle
import com.intellij.openapi.application.JBProtocolCommand
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.intellij.util.applyIf

class ChangelistJBProtocol : JBProtocolCommand("changelist") {
    private val logger = logger<ChangelistJBProtocol>()
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
     *
     * understands x-callback params: x-source, x-success, and x-error
     */
    override suspend fun execute(target: String?, parameters: Map<String, String>, fragment: String?): String? {
        val result = when (target) {
            null -> return MyBundle.message("jb.protocol.changelist.target.required").apply {
                handleCallback(false, parameters.source, parameters.success, parameters.error)
            }
            "add" -> executeAdd(parameters)
            "activate" -> executeActivate(parameters)
            "update" -> executeUpdate(parameters)
            "remove" -> executeRemove(parameters)
            else -> return IdeBundle.message("jb.protocol.unknown.target", target).apply {
                handleCallback(false, parameters.source, parameters.success, parameters.error)
            }
        }

        handleCallback(result == TargetResult.Success, parameters.source, parameters.success, parameters.error)

        return result.getOrNull()
    }

    private suspend fun ChangelistJBProtocol.executeAdd(parameters: Map<String, String>) =
            withProjectChangelist(parameters) { project: String, name: String ->
                WriteTarget.AddTarget.execute(AddParams(project,
                        AddParams.Payload(name, parameters.comment, parameters.active)))
            }

    private suspend fun ChangelistJBProtocol.executeActivate(parameters: Map<String, String>) =
            withProjectChangelist(parameters.applyIf(parameters.default == true) {
                plus("name" to LocalChangeList.getDefaultName())
            }) { project: String, name: String ->
                WriteTarget.EditTarget.execute(EditParams(project, name, EditPayload(null, true)))
            }

    private suspend fun ChangelistJBProtocol.executeUpdate(parameters: Map<String, String>) =
            parameters.newName?.let { it: String ->
                withProjectChangelist(parameters) { project: String, name: String ->
                    WriteTarget.RenameEditTarget.execute(RenameEditParams(project, name,
                            RenameEditPayload(it, parameters.comment, parameters.active)))
                }
            } ?: withProjectChangelist(parameters) { project: String, name: String ->
                WriteTarget.EditTarget.execute(EditParams(project, name,
                        EditPayload(parameters.comment, parameters.active)))
            }

    private suspend fun ChangelistJBProtocol.executeRemove(parameters: Map<String, String>) =
            withProjectChangelist(parameters) { project: String, name: String ->
                WriteTarget.RemoveTarget.execute(ChangelistParams(project, name))
            }

    private fun handleCallback(success: Boolean, source: String?, onSuccess: String?, onError: String?) {
        logger.debug("in handleCallback($success)")
        val callback = if (success) { onSuccess } else { onError }
        callback?.let { it: String ->
            logger.info("handling callback for $source")
            CallbackInvoker.getInstance().invoke(source, it)
        } ?: logger.debug("no callback url")
    }

    private val Map<String, String?>.source: String? get() = this["x-source"]
    private val Map<String, String?>.success: String? get() = this["x-success"]
    private val Map<String, String?>.error: String? get() = this["x-error"]

    private suspend fun withProjectChangelist(parameters: Map<String, String?>, f: suspend (project: String, name: String) -> TargetResult): TargetResult {
        return parameters.project?.let { project ->
            parameters.name?.let { name ->
                f(project, name)
            } ?: TargetResult.MissingParameter("name")
        } ?: TargetResult.MissingParameter("project")
    }
}