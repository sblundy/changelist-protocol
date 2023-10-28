package com.github.sblundy.changelistprotocol

import com.intellij.openapi.application.JBProtocolCommand
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.intellij.testFramework.LightPlatform4TestCase
import kotlinx.coroutines.runBlocking
import org.junit.Before

abstract class ChangelistJBProtocolTargetTestCase(private val target: String) : LightPlatform4TestCase() {
    protected lateinit var clm: ChangeListManagerImpl

    protected val testChangeListName: String
        get() = getTestName(true)

    override fun setUp() {
        super.setUp()
        clm = ChangeListManagerImpl.getInstanceImpl(project)
    }

    @Before
    fun resetChangelists() {
        clm.addChangeList(LocalChangeList.getDefaultName(), null)
        clm.setDefaultChangeList(LocalChangeList.getDefaultName())
        for (changeListName in clm.changeLists.map { it.name }) {
            if (changeListName != LocalChangeList.getDefaultName()) clm.removeChangeList(changeListName)
        }
        clm.waitUntilRefreshed()
    }

    @Suppress("UnstableApiUsage")
    protected fun targetTest(vararg parameters:Pair<String, String>, checkAction: (result: String?) -> Unit) {
        runBlocking {
            val query = parameters.asSequence().fold("project=${project.name}") { acc, e -> acc + "&${e.first}=${e.second}" }
            val result = JBProtocolCommand.execute("idea/changelist/${target}?${query}")
            checkAction(result)
        }
    }

    protected fun assertTestChangelistExists() {
        val list = clm.findChangeList(testChangeListName)
        assertNotNull(list)
    }

    protected fun assertTestChangelistNotExists() {
        val list = clm.findChangeList(testChangeListName)
        assertNull(list)
    }

    protected fun assertTestChangelistComment(expected: String?) {
        val list = clm.findChangeList(testChangeListName)
        list?.let { assertEquals(expected, it.comment) }
    }

    protected fun assertTestChangelistIsDefault() {
        val list = clm.findChangeList(testChangeListName)
        list?.let { assertTrue(it.isDefault) }
    }

    protected fun assertTestChangelistNotDefault() {
        val list = clm.findChangeList(testChangeListName)
        list?.let { assertFalse(it.isDefault) }
    }

    protected fun assertNumChangelist(expected: Int) {
        val after = clm.changeLists
        assertEquals(expected, after.size)
    }
}