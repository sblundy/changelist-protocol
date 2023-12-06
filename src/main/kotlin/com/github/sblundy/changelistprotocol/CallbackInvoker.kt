package com.github.sblundy.changelistprotocol

import com.intellij.openapi.components.service

interface CallbackInvoker {
    companion object { @JvmStatic
        fun getInstance(): CallbackInvoker = service()
    }
    fun invoke(source: String?, callback: String)
}