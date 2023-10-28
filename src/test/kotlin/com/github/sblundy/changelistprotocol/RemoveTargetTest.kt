package com.github.sblundy.changelistprotocol

import org.junit.Before
import org.junit.Test

class RemoveTargetTest : ChangelistJBProtocolTargetTestCase("remove") {
    @Test
    fun changelistIsRemoved() = targetTest("name" to testChangeListName) { result ->
        assertNull(result)

        assertNumChangelist(1)
        assertTestChangelistNotExists()
    }

    @Test
    fun errorOnNonExistentChangelist() = targetTest("name" to "not-"+testChangeListName) { result ->
        assertNotNull(result)

        assertNumChangelist(2)
        assertTestChangelistExists()
    }

    @Before
    fun createTestChangelist() {
        clm.addChangeList(getTestName(true), null)
    }
}
