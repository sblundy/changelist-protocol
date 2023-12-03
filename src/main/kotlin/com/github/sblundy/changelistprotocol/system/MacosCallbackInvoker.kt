package com.github.sblundy.changelistprotocol.system

import com.github.sblundy.changelistprotocol.CallbackInvoker
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil.execAndGetOutput
import com.intellij.openapi.diagnostic.logger

class MacosCallbackInvoker : CallbackInvoker {
    private val logger = logger<MacosCallbackInvoker>()
    override fun invoke(source: String?, callback: String) {
        val out = try {
            execAndGetOutput(GeneralCommandLine("open", callback), 30 * 1000)
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
}
