package com.github.sblundy.changelistprotocol

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.ide.RestService

class ChangelistRestService : RestService() {
    private val logger = logger<ChangelistRestService>()

    override fun getServiceName(): String = "changelist"

    override fun execute(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? =
            try {
                runBlocking {
                    logger.debug("handling ${request.method()} ${request.uri()}")
                    urlDecoder.pathSegment(2)?.let { project ->
                        logger.debug("project=${project}")
                        return@runBlocking when (request.method()) {
                            HttpMethod.GET -> executeGET(project, urlDecoder, request, context)
                            HttpMethod.POST -> executePOST(project, urlDecoder, request, context)
                            HttpMethod.PUT -> executePUT(project, urlDecoder, request, context)
                            HttpMethod.DELETE -> executeDELETE(project, urlDecoder, context)
                            else -> sendMethodNotAllowed(context)
                        }
                    }?: sendStatus(HttpResponseStatus.NOT_FOUND, false, context.channel());null
                }
            } catch (e: RuntimeException) {
                logger.error("error handling ${request.uri()}", e)
                throw e
            }

    override fun isMethodSupported(method: HttpMethod) = method == HttpMethod.POST || method == HttpMethod.GET || method == HttpMethod.PUT || method == HttpMethod.DELETE

    private suspend fun executeGET(project: String, urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
        val out = BufferExposingByteArrayOutputStream()
        val write = createJsonWriter(out)

        val result = when (val target = urlDecoder.pathSegment(3)) {
            null -> ReadTarget.ListChangelists.execute(Params(urlDecoder.flattenParameterValues().withProject(project)), write)
            else -> ReadTarget.GetChangelist.execute(ChangelistParams(urlDecoder.flattenParameterValues().withProject(project).withName(target)), write)
        }

        write.flush()

        if (logger.isDebugEnabled) {
            logger.debug("out=$out")
        }

        send(out, request, context)

        return result
    }

    private suspend fun executePOST(project: String, urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? =
            when (urlDecoder.pathSegment(3)) {
                null -> WriteTarget.AddTarget.execute(gson.fromJson<AddParams>(createJsonReader(request), AddParams::class.java).apply {
                    this.project = project
                }) ?: sendCreated(context)
                else -> sendMethodNotAllowed(context)
            }

    private suspend fun executePUT(project: String, urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? =
            when (val target = urlDecoder.pathSegment(3)) {
                null -> sendMethodNotAllowed(context)
                else -> WriteTarget.EditTarget.execute(gson.fromJson<EditParams>(createJsonReader(request), EditParams::class.java).apply {
                    this.project = project
                    this.name = target
                }) ?: sendNoContent(context)
            }

    private suspend fun executeDELETE(project: String, urlDecoder: QueryStringDecoder, context: ChannelHandlerContext): String? =
            when (val target = urlDecoder.pathSegment(3)) {
                null -> sendMethodNotAllowed(context)
                else -> {
                    WriteTarget.RemoveTarget.execute(ChangelistParams(project, target)) ?: sendNoContent(context)
                }
            }

    private fun QueryStringDecoder.pathSegment(idx: Int): String? = path().split('/').filter { it.isNotEmpty() }.elementAtOrNull(idx)

    private fun QueryStringDecoder.flattenParameterValues(): Map<String, String?> = parameters().entries.associate { (key, value) -> Pair(key, value.firstOrNull()) }

    private fun sendMethodNotAllowed(context: ChannelHandlerContext): String? {
        sendStatus(HttpResponseStatus.METHOD_NOT_ALLOWED, false, context.channel())
        return null
    }

    private fun sendNoContent(context: ChannelHandlerContext): String? {
        sendStatus(HttpResponseStatus.NO_CONTENT, false, context.channel())
        return null
    }

    private fun sendCreated(context: ChannelHandlerContext): String? {
        sendStatus(HttpResponseStatus.CREATED, false, context.channel())
        return null
    }
}