package com.github.sblundy.changelistprotocol

import org.jetbrains.annotations.TestOnly

@TestOnly
class TestCallbackInvoker: CallbackInvoker {
    val invocations  = mutableListOf<Pair<String?, String>>()
    override fun invoke(source: String?, callback: String) {
        invocations.add(Pair(source, callback))
    }

    fun reset() {
        invocations.clear()
    }
}