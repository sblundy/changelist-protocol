package com.github.sblundy.changelistprotocol

import com.google.gson.JsonParser
import com.intellij.openapi.vcs.changes.LocalChangeList
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.*
import io.netty.handler.codec.string.StringDecoder
import org.junit.Test

import java.nio.charset.StandardCharsets

class ChangelistRestServiceTest: ChangelistTestCase() {
    @TestChangelist
    @Test
    fun list() {
        val (channel, result) = executeTest(HttpMethod.GET, "api/changelist/${project.name}")

        assertNull(result)
        val out = channel.readOutbound<FullHttpResponse>()

        assertEquals(HttpResponseStatus.OK, out.status())
        val payload = JsonParser.parseString(out.content().toString(StandardCharsets.UTF_8))
        val changelists = payload.asJsonObject.get("changelists")
        assertEquals(2, changelists.asJsonArray.size())
        assertEquals(LocalChangeList.getDefaultName(), changelists.asJsonArray[0].asJsonObject.get("name").asString)
        assertEquals(testChangeListName, changelists.asJsonArray[1].asJsonObject.get("name").asString)
    }

    @TestChangelist
    @Test
    fun get() {
        val (channel, result) = executeTest(HttpMethod.GET, "api/changelist/${project.name}/$testChangeListName")

        assertNull(result)
        val out = channel.readOutbound<FullHttpResponse>()

        assertEquals(HttpResponseStatus.OK, out.status())
        val payload = JsonParser.parseString(out.content().toString(StandardCharsets.UTF_8))
        assertEquals(testChangeListName, payload.asJsonObject.get("name").asString)
    }

    @TestChangelist(create = false)
    @Test
    fun add() {
        val (_, result) = executeTest(HttpMethod.POST, "api/changelist/${project.name}","{\"name\":\"$testChangeListName\"}")

        assertNull(result)

        assertNotNull(clm.findChangeList(testChangeListName))
    }

    @TestChangelist
    @Test
    fun activate() {
        val (_, result) = executeTest(HttpMethod.PUT, "api/changelist/${project.name}/$testChangeListName", "{\"activate\":true}")

        assertNull(result)

        assertEquals(true, clm.findChangeList(testChangeListName)?.isDefault)
    }

    @TestChangelist
    @Test
    fun update() {
        val (_, result) = executeTest(HttpMethod.PUT, "api/changelist/${project.name}/$testChangeListName","{\"comment\":\"test\"}")

        assertNull(result)

        assertEquals("test", clm.findChangeList(testChangeListName)?.comment)
    }

    @TestChangelist
    @Test
    fun delete() {
        val (_, result) = executeTest(HttpMethod.DELETE, "api/changelist/${project.name}/$testChangeListName")

        assertNull(result)

        assertNull(clm.findChangeList(testChangeListName))
    }

    private fun executeTest(method: HttpMethod, uri: String): Pair<EmbeddedChannel, String?> {
        val channel = EmbeddedChannel(StringDecoder(StandardCharsets.UTF_8))
        val result = ChangelistRestService().execute(QueryStringDecoder(uri),
                DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri),
                channel.pipeline().firstContext())
        return Pair(channel, result)
    }

    private fun executeTest(method: HttpMethod, uri: String, body: String): Pair<EmbeddedChannel, String?> {
        val channel = EmbeddedChannel(StringDecoder(StandardCharsets.UTF_8))
        val result = ChangelistRestService().execute(QueryStringDecoder(uri),
                DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri, Unpooled.copiedBuffer(body, StandardCharsets.UTF_8)),
                channel.pipeline().firstContext())
        return Pair(channel, result)
    }
}