package cn.hxy.kiora.plugin.agent

/**
 * Agent 提示词的装载与检索。
 *
 * 提示词和 API 文档都放在 resources 下与本包同名的目录里，不再编进 Kotlin 字符串：
 * 改一句措辞不用重编译，也绕开了 raw string 里 `$` 会触发插值、`"""` 根本不能出现的限制。
 *
 * 文档**不再整份注入**。system prompt 里只给方法清单，签名和用法由模型用
 * `search_api` 按需取 —— 这样文档可以继续长大，而每次请求的固定成本不变。
 * 之前 631 行全量塞进去，每加一节文档都是在所有请求上收税。
 */
object AgentPrompts {

    /** src/main/resources 下与本包同名的目录，改提示词不用在 assets 里翻。 */
    private const val RES_DIR = "cn/hxy/kiora/plugin/agent"
    private const val FILE_API = "api.md"
    private const val FILE_TASK = "prompt_task.md"
    private const val FILE_QA = "prompt_qa.md"

    private const val PLACEHOLDER_INDEX = "{{API_INDEX}}"

    /** 单次检索最多返回的块数与字符数，防止一次把整份文档拉回来。 */
    private const val MAX_SEARCH_BLOCKS = 6
    private const val MAX_SEARCH_CHARS = 4000

    /** 命中章节标题时，顺带带上同章节的前几个内容块。 */
    private const val MAX_SECTION_SIBLINGS = 6

    /** 标题边界：每个标题前切开，一块 = 一个标题 + 它下面的正文。 */
    private val HEADING_SPLIT = Regex("""(?m)^(?=#{1,6}\s)""")

    /**
     * 读取失败时的兜底。
     *
     * 提示词是 Agent 的命脉，但 assets 读不到不该让整个功能废掉 ——
     * 给一份最短可用的说明，模型至少还能靠常识写点东西。
     */
    private const val FALLBACK_PROMPT = """你是 Kiora 插件脚本开发 Agent。
写代码前先确认方法确实存在，不确定就不要用；方法名臆造是最常见的失败。"""

    /**
     * 真正的 API 章节：`### 一、消息相关方法` 这种。
     *
     * 文档里 `###` 还用于 Lambda 语法、数据结构说明等，那些不该进能力清单 ——
     * 只认带中文序号的分类标题，清单才不会被说明性内容稀释。
     */
    private val API_SECTION = Regex("""^###\s*[一二三四五六七八九十]+、\s*(.+?)\s*$""")

    /** 任意二三级标题。碰到它就说明上一个 API 章节结束了。 */
    private val ANY_SECTION = Regex("""^#{2,3}(?!#)\s*(.+?)\s*$""")

    /** 行内反引号内容。 */
    private val BACKTICK = Regex("""`([^`]+)`""")

    /** 从签名里取方法名：`void onMsg(Object m)` → `onMsg`。 */
    private val CALL_SIGNATURE = Regex("""([A-Za-z_$][\w$]*)\s*\(""")

    private val apiDocument: String by lazy { readAsset(FILE_API).orEmpty() }
    private val apiIndex: String by lazy { buildIndex(apiDocument) }

    /** 任务模式的 system prompt。 */
    fun taskPrompt(): String = render(readAsset(FILE_TASK))

    /** 问答模式的 system prompt。 */
    fun qaPrompt(): String = render(readAsset(FILE_QA))

    /**
     * 文档里没有、但模型和用户很可能用的说法。
     *
     * 提示词告诉模型「可传方法名、功能词或中文描述」，而文档的章节名是固定的
     * —— 数据存储那节叫「五、数据存储方法」，模型说「持久化」一次都命中不了，
     * 接着就容易当成「这个能力不存在」。
     */
    private val KEYWORD_ALIASES: List<Pair<String, String>> = listOf(
        "持久化" to "数据存储",
        "存档" to "数据存储",
        "配置" to "数据存储",
        "变量" to "数据存储",
        "监听" to "回调方法",
        "事件" to "回调方法",
        "菜单项" to "菜单功能",
        "悬浮窗" to "菜单功能",
        "撤回" to "recallMsg",
        "禁言" to "shutUp",
        "踢人" to "kickGroup",
        "登录态" to "Cookie",
        "密钥" to "Cookie",
        "图片" to "sendPic",
        "语音" to "sendPtt",
        "发消息" to "消息相关",
    )

    /**
     * 检索 API 文档，返回命中的整段。
     *
     * 按标题切块、整块返回，是为了让模型拿到完整签名 —— 只回传命中那一行的话，
     * 参数顺序常常被切断，反而更容易写错。
     */
    fun searchApi(keyword: String): String {
        val doc = apiDocument
        if (doc.isBlank()) {
            return "API 文档读取失败。请只使用你确定存在的接口，把不确定的地方明确标出来。"
        }

        val key = keyword.trim()
        if (key.isEmpty()) return "请给出要查的方法名或功能关键字。"

        var hits = chunks.rankedBy(key)
        var usedKey = key

        // 原文一次都没命中时试别名：模型用的词和文档的措辞常常对不上
        if (hits.isEmpty()) {
            KEYWORD_ALIASES.firstOrNull { (alias, _) ->
                key.contains(alias, ignoreCase = true) || alias.contains(key, ignoreCase = true)
            }?.let { (_, target) ->
                hits = chunks.rankedBy(target)
                usedKey = target
            }
        }

        if (hits.isEmpty()) {
            return "没有找到与「$key」相关的 API。换个关键字再试，" +
                    "或者确认这个方法确实存在 —— 文档里没有的接口不要用。"
        }

        // 命中的若只是章节标题，把同章节的内容块一并带上。
        // 否则模型只拿到一行「五、数据存储方法」，等于什么都没查到 ——
        // 章节标题本身不含方法签名，真正的内容在它下面的四级标题里。
        val picked = LinkedHashSet<Chunk>()
        hits.forEach { hit ->
            picked.add(hit)
            if (hit.level <= 3 && hit.section.isNotEmpty()) {
                chunks.asSequence()
                    .filter { it.section == hit.section }
                    .take(MAX_SECTION_SIBLINGS)
                    .forEach { picked.add(it) }
            }
        }

        val body = picked.take(MAX_SEARCH_BLOCKS)
            .joinToString("\n\n") { it.text }
            .take(MAX_SEARCH_CHARS)

        return buildString {
            append(body)
            if (usedKey != key) append("\n\n（按「").append(usedKey).append("」检索）")
            if (picked.size > MAX_SEARCH_BLOCKS) {
                append("\n\n（还有 ")
                    .append(picked.size - MAX_SEARCH_BLOCKS)
                    .append(" 段未显示，请用更具体的关键字再查。）")
            }
        }
    }

    /** 文档切块：一块 = 一个标题 + 它下面的正文。 */
    private class Chunk(
        val text: String,
        /** 所属的二/三级章节标题；章节标题块自身为空串。 */
        val section: String,
        val level: Int,
    )

    private val chunks: List<Chunk> by lazy { chunkDocument(apiDocument) }

    /**
     * 按标题边界切成块。
     *
     * 比按空行切合适得多 —— 按空行会把「章节标题」和「它下面的方法」切成互不相干的
     * 两块，检索命中标题时就只返回一行标题，内容全在别处。
     */
    private fun chunkDocument(doc: String): List<Chunk> {
        var section = ""

        return HEADING_SPLIT.split(doc).mapNotNull { part ->
            val text = part.trim()
            if (text.isEmpty()) return@mapNotNull null

            val heading = text.lineSequence().first()
            val level = heading.takeWhile { it == '#' }.length

            // 二、三级标题开启新章节；四级标题归属当前章节
            if (level in 2..3) section = text

            Chunk(text = text, section = section, level = level)
        }
    }

    /**
     * 按相关性排序。
     *
     * 只做「是否包含」的过滤是不够的：宽泛词能捞回一大片 —— 「群」会命中 21 块，
     * 差不多是原文的三分之一，等于把文档又发了一遍；而真正精确的方法名命中
     * 往往只有一块。排序才能让那一块浮上来。
     */
    private fun List<Chunk>.rankedBy(key: String): List<Chunk> {
        val callPattern = Regex("""`[^`]*\b${Regex.escape(key)}\s*\(""", RegexOption.IGNORE_CASE)

        return mapNotNull { chunk ->
            if (!chunk.text.contains(key, ignoreCase = true)) return@mapNotNull null

            val score = when {
                // 命中的正是这个方法签名，最该先看到
                callPattern.containsMatchIn(chunk.text) -> 3
                // 标题里带着，多半是文档为它单开的一节
                chunk.text.lineSequence().take(1).any { it.contains(key, ignoreCase = true) } -> 2
                else -> 1
            }
            chunk to score
        }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private fun render(template: String?): String {
        if (template.isNullOrBlank()) return FALLBACK_PROMPT
        val index = apiIndex.ifBlank { "（清单生成失败，请用 search_api 逐个查询。）" }
        return template.replace(PLACEHOLDER_INDEX, index)
    }

    /**
     * 读模块自己的资源。
     *
     * 必须走模块的 ClassLoader —— `HostInfo.hostContext` 是宿主 Context，
     * 它拿到的是 QQ 的资源，读不到模块打进 APK 的那一份。
     */
    private fun readAsset(name: String): String? = runCatching {
        AgentPrompts::class.java.classLoader
            ?.getResourceAsStream("$RES_DIR/$name")
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
    }.getOrNull()?.takeIf { it.isNotBlank() }

    /** 从 API 文档里抽出「分类 → 方法名」，作为 system prompt 里的能力总览。 */
    private fun buildIndex(api: String): String {
        if (api.isBlank()) return ""

        val methods = linkedMapOf<String, MutableList<String>>()
        var collecting = false

        api.lines().forEach { line ->
            val section = API_SECTION.find(line)
            if (section != null) {
                collecting = true
                methods.getOrPut(section.groupValues[1].trim()) { mutableListOf() }
                return@forEach
            }

            // 碰到别的标题就离开 API 分类区，后面的说明性内容不进清单
            if (ANY_SECTION.find(line) != null) {
                collecting = false
                return@forEach
            }
            if (!collecting) return@forEach

            BACKTICK.findAll(line).forEach { token ->
                val name = CALL_SIGNATURE.find(token.groupValues[1])
                    ?.groupValues?.get(1)
                    ?: return@forEach
                val current = methods.values.lastOrNull() ?: return@forEach
                if (name !in current) current.add(name)
            }
        }

        return methods.entries
            .filter { it.value.isNotEmpty() }
            .joinToString("\n\n") { (title, names) ->
                "### $title\n${names.joinToString(" / ")}"
            }
    }
}
