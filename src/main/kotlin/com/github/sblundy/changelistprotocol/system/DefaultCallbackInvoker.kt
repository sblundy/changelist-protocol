package com.github.sblundy.changelistprotocol.system

import com.github.sblundy.changelistprotocol.CallbackInvoker
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.SystemInfo

class DefaultCallbackInvoker : CallbackInvoker {
    private val logger = logger<DefaultCallbackInvoker>()

    override fun invoke(source: String?, callback: String) {
        val command = generalCommandLine(callback)?: run {
            logger.warn("Cannot open URL: $callback")
            return
        }
        val out = try {
            ExecUtil.execAndGetOutput(command, 30 * 1000)
        } catch (e: ExecutionException) {
            logger.error("open exec error", e)
            return
        }

        when (val exitCode = out.exitCode) {
            0 -> logger.info("success")
            else -> {
                logger.error("open failed: $exitCode\nstdout: ${out.stdout}\nstderr: ${out.stderr}")
            }
        }
    }

    private fun generalCommandLine(callback: String) = when {
        SystemInfo.isWindows -> GeneralCommandLine(ExecUtil.windowsShellName, "/c", "start", GeneralCommandLine.inescapableQuote(""), callback)
        SystemInfo.isMac -> GeneralCommandLine(ExecUtil.openCommandPath, callback)
        SystemInfo.hasXdgOpen() -> GeneralCommandLine("xdg-open", callback)
        else -> null
    }
}