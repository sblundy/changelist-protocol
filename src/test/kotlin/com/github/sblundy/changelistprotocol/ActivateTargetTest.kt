package com.github.sblundy.changelistprotocol

import org.junit.Test

class ActivateTargetTest : ChangelistJBProtocolTargetTestCase("activate") {
    @TestChangelist
    @Test
    fun activatesChangelist() = targetTest("name" to testChangeListName) { result ->
        assertNull(result)

        assertTestChangelistIsDefault()
    }

    @TestChangelist
    @Test
    fun errorOnNonExistentChangelist() = targetTest("name" to "not-$testChangeListName") { result ->
        assertNotNull(result)

        assertTestChangelistNotDefault()
    }
}
