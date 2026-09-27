package cn.hxy.kiora.plugin.agent

import cn.hxy.kiora.plugin.agent.tool.AgentToolContext
import cn.hxy.kiora.plugin.agent.tool.AgentToolRegistry
import cn.hxy.kiora.plugin.bean.AgentMode
import cn.hxy.kiora.plugin.bean.AgentStage
import cn.hxy.kiora.plugin.net.AgentConfig
import cn.hxy.kiora.plugin.net.AgentMessage
import cn.hxy.kiora.plugin.net.AgentService
import cn.hxy.kiora.plugin.net.AgentStreamChunk
import cn.hxy.kiora.plugin.net.AgentToolCall
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

/** 一次生成请求。 */
data class AgentRequest(
    val mode: AgentMode,
    val userText: String,
    /** 之前几轮的对话，不含本轮。 */
    val history: List<AgentMessage>,
    /** 编辑模式下正在改的脚本；新建时为 null。 */
    val existingScript: String?,
    val config: AgentConfig,
    /** 工具执行环境，主要是脚本目录。 */
    val toolContext: AgentToolContext,
)

/**
 * 生成流水线。
 *
 * 这是「流式提示词模板」和「agent」的分界线：模型可以先要信息再动手 ——
 * 查 API 文档、列脚本目录、读某个文件，结果回灌后继续下一轮，最多 [MAX_TOOL_ROUNDS] 轮。
 *
 * 时序逻辑（阶段推进、工具往返、截断判断）全在这里，ViewModel 只负责把事件折叠成界面状态。
 */
class AgentPipeline(
    private val tools: AgentToolRegistry = AgentToolRegistry.default(),
) {

    fun run(request: AgentRequest): Flow<AgentEvent> = flow {
        try {
            drive(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(AgentEvent.Failed(e.message ?: "生成失败"))
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<AgentEvent>.drive(request: AgentRequest) {
        val systemPrompt = when (request.mode) {
            AgentMode.QA -> AgentPrompts.qaPrompt()
            AgentMode.TASK -> AgentPrompts.taskPrompt()
        }

        val messages = mutableListOf<AgentMessage>()
        messages.addAll(request.history)
        messages.add(AgentMessage("user", request.firstUserMessage()))

        val toolsJson = tools.toRequestJson()
        val accumulated = StringBuilder()
        var finished = false
        var round = 0

        while (!finished && round < MAX_TOOL_ROUNDS) {
            round++
            emit(AgentEvent.Stage(AgentStage.CONNECTING))

            val content = StringBuilder()
            val calls = linkedMapOf<Int, ToolCallAccumulator>()
            var finishReason: String? = null
            var reasoningStarted = false
            var contentStarted = false

            AgentService.streamRaw(messages, systemPrompt, request.config, toolsJson)
                .collect { chunk ->
                    when (chunk) {
                        is AgentStreamChunk.Reasoning -> {
                            if (!reasoningStarted) {
                                reasoningStarted = true
                                emit(AgentEvent.Stage(AgentStage.THINKING))
                            }
                            emit(AgentEvent.Reasoning(chunk.text))
                        }

                        is AgentStreamChunk.Content -> {
                            if (!contentStarted) {
                                contentStarted = true
                                emit(AgentEvent.Stage(AgentStage.WRITING))
                            }
                            content.append(chunk.text)
                            emit(AgentEvent.Content(chunk.text))
                        }

                        is AgentStreamChunk.Usage -> emit(
                            AgentEvent.Usage(
                                promptTokens = chunk.promptTokens,
                                completionTokens = chunk.completionTokens,
                                totalTokens = chunk.totalTokens,
                            )
                        )

                        is AgentStreamChunk.Finish -> finishReason = chunk.reason

                        is AgentStreamChunk.ToolCallDelta ->
                            calls.getOrPut(chunk.index) { ToolCallAccumulator() }.merge(chunk)
                    }
                }

            accumulated.append(content)

            val pending = calls.values.filter { it.isCallable() }.map { it.toCall() }

            if (finishReason != "tool_calls" || pending.isEmpty()) {
                // 模型没要求工具，或要了但参数残缺：这一轮就是最终产物
                emit(AgentEvent.Stage(AgentStage.PARSING))
                emit(AgentEvent.Completed(accumulated.toString(), finishReason))
                finished = true
                continue
            }

            // 把这一轮的 assistant 消息与工具结果接进对话，继续下一轮
            messages.add(
                AgentMessage(
                    role = "assistant",
                    content = content.toString(),
                    toolCalls = pending,
                )
            )

            pending.forEach { call ->
                emit(AgentEvent.ToolCall(call.name, call.arguments))
                val result = execute(call, request.toolContext)
                emit(AgentEvent.ToolResult(call.name, result.take(PREVIEW_CHARS)))
                messages.add(AgentMessage(role = "tool", content = result, toolCallId = call.id))
            }
        }

        if (!finished) {
            // 轮次用尽仍在要工具：把手上的内容交出去，别让用户干等
            emit(AgentEvent.Stage(AgentStage.PARSING))
            emit(AgentEvent.Completed(accumulated.toString(), FINISH_ROUNDS_EXHAUSTED))
        }
    }

    private fun execute(call: AgentToolCall, context: AgentToolContext): String {
        val tool = tools.find(call.name)
            ?: return "没有名为「${call.name}」的工具。可用的工具是：${tools.names.joinToString("、")}。"

        val arguments = runCatching { JSONObject(call.arguments.ifBlank { "{}" }) }.getOrNull()
            ?: return "调用参数不是合法 JSON：${call.arguments.take(200)}"

        return runCatching { tool.execute(arguments, context) }
            .getOrElse { "工具执行失败：${it.message}" }
    }

    /**
     * 首轮的 user 消息。
     *
     * 编辑模式下把现有脚本接在需求前面 —— 用的是与产出协议相同的
     * `=== 文件名 ===` 形状，模型看到的既有代码和它要写的东西格式一致。
     */
    private fun AgentRequest.firstUserMessage(): String {
        if (mode == AgentMode.QA) return userText
        if (existingScript.isNullOrBlank()) return userText

        return "请基于以下现有脚本进行修改。\n\n$existingScript\n\n我的需求是：$userText"
    }

    /** 累积一次工具调用的分片。`arguments` 在协议里是逐片到达的 JSON 字符串。 */
    private class ToolCallAccumulator {
        private var id = ""
        private var name = ""
        private val arguments = StringBuilder()

        fun merge(delta: AgentStreamChunk.ToolCallDelta) {
            delta.id?.let { if (id.isEmpty()) id = it }
            delta.name?.let { if (name.isEmpty()) name = it }
            arguments.append(delta.argumentsChunk)
        }

        /** 名字都没拼出来就没法调用，多半是半截分片。 */
        fun isCallable(): Boolean = name.isNotEmpty()

        fun toCall(): AgentToolCall = AgentToolCall(
            id = id.ifBlank { "call_$name" },
            name = name,
            arguments = arguments.toString().ifBlank { "{}" },
        )
    }

    companion object {
        /** 工具往返轮数上限。够模型「查几次文档 + 读几个文件」，又不至于转不出来。 */
        private const val MAX_TOOL_ROUNDS = 6

        /** 回灌给界面的工具结果预览长度。 */
        private const val PREVIEW_CHARS = 240

        const val FINISH_ROUNDS_EXHAUSTED = "tool_rounds_exhausted"
    }
}
