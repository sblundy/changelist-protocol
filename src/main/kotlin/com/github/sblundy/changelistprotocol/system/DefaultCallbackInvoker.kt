package com.github.sblundy.changelistprotocol.system

import com.github.sblundy.changelistprotocol.CallbackInvoker
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.mac.foundation.Foundation
import com.intellij.ui.mac.foundation.ID
import com.intellij.ui.mac.foundation.NSWorkspace

class DefaultCallbackInvoker : CallbackInvoker {
    private val logger = logger<DefaultCallbackInvoker>()

    override fun invoke(source: String?, callback: String) {
        when {
            SystemInfo.isWindows -> invokeOpen(GeneralCommandLine(ExecUtil.windowsShellName, "/c", "start",
                GeneralCommandLine.inescapableQuote(""), callback))
            SystemInfo.isMac -> invokeMac(source, callback)
            SystemInfo.hasXdgOpen() -> invokeOpen(GeneralCommandLine("xdg-open", callback))
            else -> logger.warn("Cannot open URL: $callback")
        }}

    private fun invokeOpen(command: GeneralCommandLine) {
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
    private fun invokeMac(source: String?, callback: String) {
        val pool = Foundation.NSAutoreleasePool()
        try {
            val workspace = NSWorkspace.getInstance()
            val url = callbackURL(callback)
            if (openURL(workspace, url).booleanValue()) {
                logger.info("invoked callback: $callback source=$source")
            } else {
                logger.warn("callback failed: $callback source=$source")
            }
        } finally {
            pool.drain()
        }
    }

    private fun callbackURL(callback: String) =
        Foundation.invoke(Foundation.getObjcClass("NSURL"), "URLWithString:", Foundation.nsString(callback))

    private fun openURL(workspace: ID, url: ID) =
        Foundation.invoke(workspace, "openURL:", url)
}