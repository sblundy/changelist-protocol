package com.github.sblundy.changelistprotocol

import org.junit.After
import org.junit.Test

class XCallbackTest: ChangelistJBProtocolTargetTestCase("add") {
    @Test
    fun xSuccess() = targetTest("name" to testChangeListName, "x-success" to "$testChangeListName-onsuccess", "x-error" to "$testChangeListName-onerror") { result: String? ->
        assertNull(result)

        val invocation = (CallbackInvoker.getInstance() as TestCallbackInvoker).invocations.lastOrNull()
        assertNotNull(invocation)
        assertNull(invocation?.first)
        assertEquals("$testChangeListName-onsuccess", invocation?.second)
    }

    @TestChangelist
    @Test
    fun xError() = targetTest("name" to testChangeListName, "x-success" to "$testChangeListName-onsuccess", "x-error" to "$testChangeListName-onerror") { result: String? ->
        assertNotNull(result)

        val invocation = (CallbackInvoker.getInstance() as TestCallbackInvoker).invocations.lastOrNull()
        assertNotNull(invocation)
        assertNull(invocation?.first)
        assertEquals("$testChangeListName-onerror", invocation?.second)
    }

    @After
    fun resetTestCallbackInvoker() {
        (CallbackInvoker.getInstance() as TestCallbackInvoker).reset()
    }
}