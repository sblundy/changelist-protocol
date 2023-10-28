package com.github.sblundy.changelistprotocol

import org.junit.Before
import org.junit.Test

class ActivateTargetTest : ChangelistJBProtocolTargetTestCase("activate") {
    @Test
    fun activatesChangelist() = targetTest("name" to testChangeListName) { result ->
        assertNull(result)

        assertTestChangelistIsDefault()
    }

    @Test
    fun errorOnNonExistentChangelist() = targetTest("name" to "not-$testChangeListName") { result ->
        assertNotNull(result)

        assertTestChangelistNotDefault()
    }

    @Before
    fun createTestChangelist() {
        clm.addChangeList(getTestName(true), null)
    }
}
