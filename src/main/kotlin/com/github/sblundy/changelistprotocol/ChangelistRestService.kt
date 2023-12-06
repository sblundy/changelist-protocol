package com.github.sblundy.changelistprotocol

import com.google.gson.stream.JsonWriter
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.ide.RestService
import kotlin.reflect.KClass

@Suppress("NAME_SHADOWING")
class ChangelistRestService : RestService() {
    private val logger = logger<ChangelistRestService>()

    override fun getServiceName(): String = "changelist"

    override fun execute(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? =
            try {
                runBlocking {
                    val project = urlDecoder.pathSegment(2)
                            ?: return@runBlocking sendEmptyResponse(HttpResponseStatus.NOT_FOUND, context)

                    return@runBlocking when (val changelist = urlDecoder.pathSegment(3)) {
                        null -> {
                            logger.debug("handling ${request.method()} $project")
                            when (request.method()) {
                                HttpMethod.GET -> executeGET(project, request, context)
                                HttpMethod.POST -> executePOST(project, request, context)
                                else -> sendEmptyResponse(HttpResponseStatus.METHOD_NOT_ALLOWED, context)
                            }
                        }
                        else -> {
                            logger.debug("handling ${request.method()} $project/$changelist")
                            when (request.method()) {
                                HttpMethod.GET -> executeGET(project, changelist, request, context)
                                HttpMethod.POST -> executePOST(project, changelist, request, context)
                                HttpMethod.PUT -> executePUT(project, changelist, request, context)
                                HttpMethod.DELETE -> executeDELETE(project, changelist, request, context)
                                else -> sendEmptyResponse(HttpResponseStatus.METHOD_NOT_ALLOWED, context)
                            }
                        }
                    }
                }
            } catch (e: RuntimeException) {
                logger.error("error handling ${request.uri()}", e)
                sendStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR, false, context.channel())
                throw e
            }

    override fun isMethodSupported(method: HttpMethod) = method == HttpMethod.POST || method == HttpMethod.GET || method == HttpMethod.PUT || method == HttpMethod.DELETE

    private suspend fun executeGET(project: String, request: FullHttpRequest, context: ChannelHandlerContext): String? =
            withJSONResponseWriter(request, context) { write -> ReadTarget.ListChangelists.execute(Params(project), write) }

    private suspend fun executeGET(project: String, changelist: String, request: FullHttpRequest, context: ChannelHandlerContext): String? =
            withJSONResponseWriter(request, context) { write -> ReadTarget.GetChangelist.execute(ChangelistParams(project, changelist), write)}

    private suspend fun withJSONResponseWriter(request: FullHttpRequest, context: ChannelHandlerContext, f: suspend (JsonWriter) -> TargetResult): String? {
        val out = BufferExposingByteArrayOutputStream()
        val write = createJsonWriter(out)

        val result = f(write)

        return handleResult(request, result, context) { context ->
            write.flush()
            send(out, request, context)
            null
        }
    }

    private suspend fun executePOST(project: String, request: FullHttpRequest, context: ChannelHandlerContext): String? =
            handleResult(request, WriteTarget.AddTarget.execute(AddParams(project, readPayload(request, AddParams.Payload::class))), context) {
                sendEmptyResponse(HttpResponseStatus.CREATED, it)
            }

    private suspend fun executePOST(project: String, changelist: String, request: FullHttpRequest, context: ChannelHandlerContext): String? =
            handleResult(request, WriteTarget.RenameEditTarget.execute(RenameEditParams(project, changelist, readPayload(request, RenameEditPayload::class))), context) {
                sendEmptyResponse(HttpResponseStatus.NO_CONTENT, it)
            }

    private suspend fun executePUT(project: String, changelist: String, request: FullHttpRequest, context: ChannelHandlerContext): String? =
            handleResult(request, WriteTarget.EditTarget.execute(EditParams(project, changelist, readPayload(request, EditPayload::class))), context) {
                sendEmptyResponse(HttpResponseStatus.NO_CONTENT, it)
            }

    private fun <P: Any> readPayload(request: FullHttpRequest, p: KClass<P>): P =
            gson.fromJson(createJsonReader(request), p.java)

    private suspend fun executeDELETE(project: String, changelist: String, request: FullHttpRequest, context: ChannelHandlerContext): String? =
            handleResult(request, WriteTarget.RemoveTarget.execute(ChangelistParams(project, changelist)), context) { sendEmptyResponse(HttpResponseStatus.NO_CONTENT, it) }

    private fun QueryStringDecoder.pathSegment(idx: Int): String? = path().split('/').filter { it.isNotEmpty() }.elementAtOrNull(idx)

    private fun sendEmptyResponse(status: HttpResponseStatus, context: ChannelHandlerContext): String? {
        sendStatus(status, false, context.channel())
        return null
    }

    private fun handleResult(request: HttpRequest, result: TargetResult, context: ChannelHandlerContext, onSuccess: (context: ChannelHandlerContext) -> String?): String? {
        return when (result) {
            TargetResult.Success -> onSuccess(context)
            is TargetResult.ChangelistNotEnabled -> sendErrorResponse(request, result, HttpResponseStatus.FORBIDDEN, context)
            is TargetResult.ProjectNotFound -> sendErrorResponse(request, result, HttpResponseStatus.NOT_FOUND, context)
            is TargetResult.ChangelistNotFound -> sendErrorResponse(request, result, HttpResponseStatus.NOT_FOUND, context)
            is TargetResult.MissingParameter -> sendErrorResponse(request, result, HttpResponseStatus.BAD_REQUEST, context)
            is TargetResult.DeactivateNotPermitted -> sendErrorResponse(request, result, HttpResponseStatus.BAD_REQUEST, context)
            is TargetResult.DeleteNotPermitted -> sendErrorResponse(request, result, HttpResponseStatus.BAD_REQUEST, context)
            is TargetResult.DuplicateChangelist -> sendErrorResponse(request, result, HttpResponseStatus.BAD_REQUEST, context)
        }
    }

    private fun sendErrorResponse(request: HttpRequest, result: TargetResult.ErrorTargetResult, status: HttpResponseStatus, context: ChannelHandlerContext): String? {
        sendResponse(request, context, DefaultFullHttpResponse(request.protocolVersion(), status,
                Unpooled.wrappedBuffer(result.getOrNull().encodeToByteArray())))
        return null
    }
}