package com.github.sblundy.changelistprotocol.system

import com.github.sblundy.changelistprotocol.CallbackInvoker
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.mac.foundation.Foundation.*
import com.intellij.ui.mac.foundation.ID
import com.intellij.ui.mac.foundation.NSWorkspace

class MacosCallbackInvoker : CallbackInvoker {
    private val logger = logger<MacosCallbackInvoker>()

    override fun invoke(source: String?, callback: String) {
        val pool = NSAutoreleasePool()
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
            invoke(getObjcClass("NSURL"), "URLWithString:", nsString(callback))

    private fun openURL(workspace: ID, url: ID) =
            invoke(workspace, "openURL:", url)
}
