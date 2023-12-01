package com.github.sblundy.changelistprotocol

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.ide.IdeBundle
import com.intellij.openapi.application.JBProtocolCommand
import com.intellij.openapi.diagnostic.logger

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
                handleInvalidCallback(parameters["x-source"], parameters["x-error"])
            }
            "add" -> WriteTarget.AddTarget.execute(AddParams(parameters))
            "activate" -> WriteTarget.ActivateTarget.execute(ActivateParams(parameters))
            "update" -> if (parameters.containsKey("new-name")) {
                WriteTarget.RenameEditTarget.execute(RenameEditParams(parameters))
            } else {
                WriteTarget.EditTarget.execute(EditParams(parameters))
            }
            "remove" -> WriteTarget.RemoveTarget.execute(ChangelistParams(parameters))
            else -> return IdeBundle.message("jb.protocol.unknown.target", target).apply {
                handleInvalidCallback(parameters["x-source"], parameters["x-error"])
            }
        }

        handleCallback(result, parameters["x-source"], parameters["x-success"], parameters["x-error"])

        return result.getOrNull()
    }

    private fun handleInvalidCallback(source: String?, onError: String?) {
        onError?.let { onError ->
            logger.info("handling invalid call from $source")
            fireCallback(onError)
        }
    }

    private fun handleCallback(result: TargetResult, source: String?, onSuccess: String?, onError: String?) {
        logger.debug("in handleCallback(result: $result)")
        val callback = if (result == TargetResult.Success) { onSuccess } else { onError }
        callback?.let {
            logger.info("handling callback for $source")
            fireCallback(it)
        }?:logger.debug("no callback url")
    }

    private fun fireCallback(callback: String) {
        val out = try {
            val h = CapturingProcessHandler(GeneralCommandLine("open", callback))
            h.runProcess(30 * 1000, true)
        } catch (e: ExecutionException) {
            logger.error("open exec error", e)
            return
        } catch (e: RuntimeException) {
            logger.error("runtime exception", e)
            throw e
        }

        when (val exitCode = out.exitCode) {
            0 -> logger.info("success")
            else -> {
                logger.error("open failed: $exitCode\nstdout: ${out.stdout}\nstderr: ${out.stderr}")
            }
        }
    }
}