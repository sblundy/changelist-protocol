package com.github.sblundy.changelistprotocol

import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.vcs.changes.ChangeListManagerEx
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.intellij.testFramework.LightPlatform4TestCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.MethodRule
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement

abstract class ChangelistTestCase : LightPlatform4TestCase() {
    @get:Rule
    val changelistRule = TestChangelistRule()

    protected val testChangeListName: String
        get() = "tc-"+getTestName(true)

    protected val clm: ChangeListManagerImpl
        get() = ChangeListManagerImpl.getInstanceImpl(project)

    @After
    fun resetChangelists() {
        clm.addChangeList(LocalChangeList.getDefaultName(), null)
        clm.setDefaultChangeList(LocalChangeList.getDefaultName())
        for (changeListName in clm.changeLists.map { it.name }) {
            if (changeListName != LocalChangeList.getDefaultName()) clm.removeChangeList(changeListName)
        }
        clm.waitUntilRefreshed()
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

    override fun getData(dataId: String): Any? {
        return when {
            TestChangelistName.`is`(dataId) -> { testChangeListName }
            ChangelistMgr.`is`(dataId) -> { clm }
            else -> super.getData(dataId)
        }
    }
}

val TestChangelistName = DataKey.create<String>("testChangelistName")

val ChangelistMgr = DataKey.create<ChangeListManagerEx>("ChangelistMgr")

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class TestChangelist(val create: Boolean = true, val active: Boolean = false)

class TestChangelistRule: MethodRule {
    override fun apply(base: Statement, method: FrameworkMethod, target: Any): Statement {
        val annotation = method.getAnnotation(TestChangelist::class.java)
        return if (annotation?.create == true) {
            object : Statement() {
                override fun evaluate() {
                    val clmgr = ChangelistMgr.getData(target as DataProvider)!!
                    val name = TestChangelistName.getData(target)
                    clmgr.addChangeList(name!!, null)
                    if (annotation.active) {
                        clmgr.setDefaultChangeList(name)
                    }
                    base.evaluate()
                }
            }
        } else {
            base
        }
    }
}