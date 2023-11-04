package com.github.sblundy.changelistprotocol

import org.intellij.markdown.html.urlEncode
import org.junit.Test

class AddTargetTest : ChangelistJBProtocolTargetTestCase("add") {
    @Test
    fun simpleChangelist() = targetTest("name" to testChangeListName) { result ->
        assertNull(result)

        assertNumChangelist(2)
        assertTestChangelistExists()
        assertTestChangelistIsDefault()
        assertTestChangelistComment("")
    }

    @Test
    fun withComment() = targetTest("name" to testChangeListName, "comment" to urlEncode("test comment")) { result ->
        assertNull(result)

        assertNumChangelist(2)
        assertTestChangelistExists()
        assertTestChangelistIsDefault()
        assertTestChangelistComment("test comment")
    }

    @Test
    fun activateFalse() = targetTest("name" to testChangeListName, "activate" to "false") { result ->
        assertNull(result)

        assertNumChangelist(2)
        assertTestChangelistExists()
        assertTestChangelistNotDefault()
    }
}
