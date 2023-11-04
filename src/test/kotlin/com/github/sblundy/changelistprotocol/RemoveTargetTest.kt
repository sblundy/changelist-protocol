package com.github.sblundy.changelistprotocol

import org.junit.Test

class RemoveTargetTest : ChangelistJBProtocolTargetTestCase("remove") {
    @TestChangelist
    @Test
    fun changelistIsRemoved() = targetTest("name" to testChangeListName) { result ->
        assertNull(result)

        assertNumChangelist(1)
        assertTestChangelistNotExists()
    }

    @TestChangelist
    @Test
    fun errorOnNonExistentChangelist() = targetTest("name" to "not-$testChangeListName") { result ->
        assertNotNull(result)

        assertNumChangelist(2)
        assertTestChangelistExists()
    }
}
