package cn.hxy.kiora.plugin.agent.tool

import org.json.JSONObject

/** 列出脚本目录里的文件。编辑现有脚本前，模型先用它看清结构。 */
object ListFilesTool : AgentTool {

    override val name = "list_files"

    override val description =
        "列出当前脚本目录里的全部文件及其相对路径与大小。" +
                "编辑现有脚本前先用它看清结构，别凭猜测改。"

    override val parameters: JSONObject = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject())
    }

    override fun execute(arguments: JSONObject, context: AgentToolContext): String {
        val dir = context.scriptDir
            ?: return "当前是新建脚本，还没有脚本目录。直接产出文件即可，不需要列目录。"

        if (!dir.exists()) return "脚本目录不存在。"

        val root = dir.canonicalFile
        val entries = dir.walkTopDown()
            .filter { it.isFile }
            .mapNotNull { file ->
                runCatching {
                    val rel = file.canonicalFile.relativeTo(root).path.replace('\\', '/')
                    "$rel  (${file.length()} 字节)"
                }.getOrNull()
            }
            .sorted()
            .toList()

        if (entries.isEmpty()) return "脚本目录是空的。"

        return "共 ${entries.size} 个文件：\n" + entries.joinToString("\n")
    }
}
