package com.github.sblundy.changelistprotocol

import org.intellij.markdown.html.urlEncode
import org.junit.Test

class UpdateTargetTest : ChangelistJBProtocolTargetTestCase("update") {
    @TestChangelist
    @Test
    fun activatesChangelist() = targetTest("name" to testChangeListName, "active" to true.toString()) { result ->
        assertNull(result)

        assertTestChangelistIsDefault()
    }

    @TestChangelist
    @Test
    fun updateComment() = targetTest("name" to testChangeListName, "comment" to urlEncode("Updated comment")) { result ->
        assertNull(result)

        assertTestChangelistComment("Updated comment")
    }

    @TestChangelist
    @Test
    fun renameChangelist() = targetTest("name" to testChangeListName, "new-name" to "new-$testChangeListName") { result ->
        assertNull(result)

        assertTestChangelistNotExists()
        assertTestChangelistExists("new-$testChangeListName")
    }
}