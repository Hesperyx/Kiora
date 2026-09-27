package cn.hxy.kiora.plugin.agent.tool

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 读取脚本目录里的单个文件。
 *
 * 有了它就不必在每轮请求里预先把整个目录塞进去 —— 模型先 `list_files` 看清有什么，
 * 再决定读哪几个。之前那种「全量塞 + 超上限静默省略」的做法，模型甚至不知道自己缺东西。
 */
object ReadFileTool : AgentTool {

    /** 单次返回的字符上限，超长文件截断并说明，避免一次把上下文吃光。 */
    private const val MAX_READ_CHARS = 20_000

    override val name = "read_file"

    override val description =
        "读取脚本目录里的某个文件。要改现有脚本时先读它再动手，不要凭猜测改。" +
                "一次只读一个文件，路径用相对路径。"

    override val parameters: JSONObject = JSONObject().apply {
        put("type", "object")
        put(
            "properties",
            JSONObject().apply {
                put(
                    "path",
                    JSONObject().apply {
                        put("type", "string")
                        put("description", "相对脚本目录的路径，例如 main.java、core/economy.java")
                    }
                )
            }
        )
        put("required", JSONArray().put("path"))
    }

    override fun execute(arguments: JSONObject, context: AgentToolContext): String {
        val dir = context.scriptDir
            ?: return "当前是新建脚本，还没有文件可读。"

        val raw = arguments.optString("path").trim()
        if (raw.isEmpty()) return "请给出要读取的文件路径。"

        val root = runCatching { dir.canonicalFile }.getOrNull() ?: return "脚本目录无效。"
        val target = runCatching { File(dir, raw).canonicalFile }.getOrNull()
            ?: return "路径无效：$raw"

        // 与落盘侧同一套防护：任何情况下都不越出脚本目录
        if (!target.path.startsWith(root.path + File.separator)) {
            return "只能读取脚本目录内的文件，$raw 超出了范围。"
        }
        if (!target.exists() || !target.isFile) return "文件不存在：$raw"

        val text = runCatching { target.readText() }.getOrNull()
            ?: return "读取失败：$raw（可能是二进制文件，读不了）"

        val rel = target.relativeTo(root).path.replace('\\', '/')

        return if (text.length > MAX_READ_CHARS) {
            "=== $rel（共 ${text.length} 字符，以下为前 $MAX_READ_CHARS） ===\n" +
                    text.take(MAX_READ_CHARS) +
                    "\n…（已截断，需要看后面某一段请告诉我）"
        } else {
            "=== $rel ===\n$text"
        }
    }
}
