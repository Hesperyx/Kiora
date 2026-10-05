@file:Suppress("unused", "SameParameterValue")

package dev.ujhhgtg.wekit.utils

import bsh.Modifiers
import bsh.Node
import bsh.Primitive
import bsh.snapshot.BshSnapshot
import bsh.snapshot.BshSnapshotHelper
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InvalidClassException
import java.io.ObjectInputStream
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Decompiles [BshSnapshot] serialized AST nodes back into BeanShell source code.
 */
object BshSnapshotDecompiler {

    // DEBUG: set true to annotate output with /* node-id:children */
    private const val DEBUG = false

    private fun dbg(node: Node, result: String): String {
        if (!DEBUG) return result
        val info = "/* ${node.javaClass.simpleName}[id=${node.getId()},ch=${node.jjtGetNumChildren()}] */"
        return "$result $info"
    }

    val SECRET_KEY: SecretKey =
        SecretKeySpec("0123456789abcdef".toByteArray(StandardCharsets.UTF_8), "AES")

    private val MAGIC = byteArrayOf('B'.code.toByte(), 'S'.code.toByte(), 'H'.code.toByte(), 'S'.code.toByte())

    // ── Public entry points ────────────────────────────────────────────────

    fun decompileFile(file: File): String {
        val snapshot = readSnapshot(file)
        return decompile(snapshot)
    }

    fun decompileStream(input: InputStream): String {
        val snapshot = readSnapshot(input)
        return decompile(snapshot)
    }

    fun decompile(snapshot: BshSnapshot): String {
        val nodes = snapshot.nodes ?: return ""
        return nodes.joinToString("\n") { node ->
            val code = decompileNode(node, 0)
            // Add trailing semicolons for IDE friendliness.
            // BeanShell itself does not require them.
            if (code.isEmpty() ||
                code.endsWith(";") ||
                code.endsWith("}") ||
                code.endsWith(":")
            ) code
            else "$code;"
        }
    }

    // ── Snapshot reading ───────────────────────────────────────────────────

    private fun readSnapshot(input: InputStream): BshSnapshot {
        val bis = BufferedInputStream(input)
        bis.mark(MAGIC.size)
        val magic = ByteArray(MAGIC.size)
        val read = bis.read(magic)
        bis.reset()
        if (read < MAGIC.size) throw IOException("Stream too short")
        return if (magic.contentEquals(MAGIC)) {
            BshSnapshotHelper.readEncrypted(bis, SECRET_KEY)
        } else {
            ObjectInputStream(bis).use { ois ->
                val obj = ois.readObject()
                if (obj !is BshSnapshot)
                    throw InvalidClassException("Expected BshSnapshot, got ${obj.javaClass.name}")
                obj
            }
        }
    }

    private fun readSnapshot(file: File): BshSnapshot {
        val raf = RandomAccessFile(file, "r")
        val magic = ByteArray(MAGIC.size)
        val read = raf.read(magic)
        raf.close()
        if (read < MAGIC.size) throw IOException("File too short: ${file.name}")
        return if (magic.contentEquals(MAGIC)) {
            file.inputStream().use { BshSnapshotHelper.readEncrypted(it, SECRET_KEY) }
        } else {
            file.inputStream().use { raw ->
                ObjectInputStream(raw).use { ois ->
                    val obj = ois.readObject()
                    if (obj !is BshSnapshot)
                        throw InvalidClassException("Expected BshSnapshot, got ${obj.javaClass.name}")
                    obj
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Node decompiler (recursive)
    // ═══════════════════════════════════════════════════════════════════════

    private fun decompileNode(node: Node, indent: Int): String {
        if (node.jjtGetNumChildren() == 0 && node.getId() == 0)
            return ""
        return dbg(
            node, when (node.getId()) {
                0 -> ""
                1 -> decompileClass(node, indent)
                2 -> decompileEnumConst(node)
                3 -> decompileBlock(node, indent)
                4 -> decompileMethod(node, indent)
                5 -> decompilePackage(node)
                6 -> decompileImport(node)
                7 -> decompileVarDeclarator(node)
                8 -> decompileArrayInit(node, indent)
                9 -> decompileFormalParams(node, indent)
                10 -> decompileFormalParam(node)
                11 -> decompileType(node)
                12 -> decompileReturnType(node)
                13 -> decompilePrimitiveType(node)
                14 -> strField(node, "text") ?: "?"
                15 -> decompileAssignment(node, indent)
                16 -> decompileTernary(node, indent)
                17 -> decompileBinary(node, indent)
                18 -> decompileUnary(node, indent)
                19 -> decompileCast(node, indent)
                20 -> decompilePrimaryExpr(node, indent)
                21 -> decompileMethodInvocation(node, indent)
                22 -> decompileLambda(node, indent)
                23 -> decompileChildren(node, ", ", indent)
                24 -> decompileChildren(node, ", ", indent)
                25 -> decompilePrimarySuffix(node, indent)
                26 -> decompileLiteral(node)
                27 -> decompileArgs(node, indent)
                28 -> decompileAllocation(node, indent)
                29 -> decompileArrayDims(node, indent)
                30 -> decompileLabeledStmt(node, indent)
                31 -> decompileSwitch(node, indent)
                32 -> decompileSwitchLabel(node, indent)
                33 -> decompileIf(node, indent)
                34 -> decompileWhile(node, indent)
                35 -> decompileFor(node, indent)
                36 -> decompileEnhancedFor(node, indent)
                37 -> decompileTypedVarDecl(node, indent)
                38 -> decompileChildren(node, ", ", indent)
                39 -> decompileReturn(node, indent)
                40 -> "throw ${decompileChild(node, 0, indent)};"
                41 -> decompileTry(node, indent)
                42 -> decompileMultiCatch(node)
                43 -> "try"
                44 -> decompileAutoCloseable(node, indent)
                else -> "/* unknown node ${node.javaClass.name} */"
            }
        )
    }

    private fun decompileBlock(block: Node, indent: Int): String {
        val sb = StringBuilder()
        if (boolField(block, "isSynchronized")) {
            // synchronized(expr) – child 0 is the expression, child 1+ is the body
            val expr = if (block.jjtGetNumChildren() > 0)
                decompileNode(block.jjtGetChild(0), indent) else "?"
            sb.append("synchronized ($expr) ")
            for (i in 1 until block.jjtGetNumChildren()) {
                sb.append(decompileNode(block.jjtGetChild(i), indent))
            }
        } else {
            if (boolField(block, "isStatic")) sb.append("static ")
            sb.append("{\n")
            for (i in 0 until block.jjtGetNumChildren()) {
                val child = decompileNode(block.jjtGetChild(i), indent + 1)
                if (child.isNotEmpty()) {
                    sb.append("  ".repeat(indent + 1))
                    sb.append(child)
                    if (!child.endsWith(";") && !child.endsWith("}") && !child.endsWith("{\n") && !child.endsWith(":"))
                        sb.append(";")
                    sb.append("\n")
                }
            }
            sb.append("  ".repeat(indent))
            sb.append("}")
        }
        return dbg(block, sb.toString())
    }

    private fun decompileClass(cls: Node, indent: Int): String {
        val sb = StringBuilder()
        val clsMods = modifiers(cls); if (clsMods != null && clsMods.modifiers != 0) sb.append(modifiersString(clsMods)).append(" ")
        sb.append("class ${strField(cls, "name") ?: "?"} ")
        var i = 0
        val nc = cls.jjtGetNumChildren()
        if (0 < nc && cls.jjtGetChild(i).getId() == 11) {
            val et = decompileType(cls.jjtGetChild(i))
            if (et.isNotEmpty() && et != "java.lang.Object") {
                sb.append("extends $et ")
            }
            i++
        }
        val impl = mutableListOf<String>()
        while (i < nc && cls.jjtGetChild(i).getId() == 11) {
            impl.add(decompileType(cls.jjtGetChild(i)))
            i++
        }
        if (impl.isNotEmpty()) sb.append("implements ${impl.joinToString(", ")} ")
        if (i < nc) sb.append(decompileNode(cls.jjtGetChild(i), indent)) else sb.append("{}")
        return sb.toString()
    }

    private fun decompileMethod(md: Node, indent: Int): String {
        val sb = StringBuilder()
        val mdMods = modifiers(md); if (mdMods != null && mdMods.modifiers != 0) sb.append(modifiersString(mdMods)).append(" ")
        val nc = md.jjtGetNumChildren()
        var i = 0
        if (0 < nc && md.jjtGetChild(i).getId() == 12) {
            sb.append(decompileReturnType(md.jjtGetChild(i)))
            sb.append(" ")
            i++
        }
        val methodName = if (!strField(md, "name").isNullOrEmpty()) {
            strField(md, "name")
        } else if (i < nc && md.jjtGetChild(i).getId() == 14) {
            strField(md.jjtGetChild(i), "text") ?: "?"
        } else "?"
        if (i < nc && md.jjtGetChild(i).getId() == 14) {
            i++
        }
        sb.append(methodName)
        if (i < nc && md.jjtGetChild(i).getId() == 9) {
            sb.append(decompileFormalParams(md.jjtGetChild(i), indent))
            i++
        } else {
            sb.append("()")
        }
        while (i < nc && md.jjtGetChild(i).getId() == 14) {
            sb.append(" throws ")
            sb.append(strField(md.jjtGetChild(i), "text") ?: "?")
            i++
        }
        if (i < nc) {
            sb.append(" ")
            sb.append(decompileBlock(md.jjtGetChild(i), indent))
        } else {
            sb.append(";")
        }
        return dbg(md, sb.toString())
    }

    private fun decompileEnumConst(ec: Node): String = strField(ec, "name") ?: "?"

    private fun decompilePackage(node: Node): String {
        val name = if (node.jjtGetNumChildren() > 0) decompileNode(node.jjtGetChild(0), 0) else ""
        return "package $name;"
    }

    private fun decompileImport(imp: Node): String {
        val sb = StringBuilder("import")
        if (boolField(imp, "staticImport")) sb.append(" static")
        if (boolField(imp, "superImport")) sb.append(" super")
        sb.append(" ")
        val name = if (imp.jjtGetNumChildren() > 0) decompileNode(imp.jjtGetChild(0), 0) else ""
        sb.append(name)
        if (boolField(imp, "importPackage")) sb.append(".*")
        sb.append(";")
        return sb.toString()
    }

    private fun decompileVarDeclarator(vd: Node): String {
        val sb = StringBuilder(strField(vd, "name") ?: "?")
        val vdDims = intField(vd, "dimensions"); if (vdDims > 0) sb.append("[]".repeat(vdDims))
        if (vd.jjtGetNumChildren() > 0) sb.append(" = ${decompileNode(vd.jjtGetChild(0), 0)}")
        return sb.toString()
    }

    private fun decompileArrayInit(node: Node, indent: Int): String {
        val sb = StringBuilder("{")
        for (i in 0 until node.jjtGetNumChildren()) {
            if (i > 0) sb.append(", ")
            sb.append(decompileNode(node.jjtGetChild(i), indent))
        }
        sb.append("}")
        return sb.toString()
    }

    private fun decompileFormalParams(node: Node, indent: Int): String {
        val sb = StringBuilder("(")
        for (i in 0 until node.jjtGetNumChildren()) {
            if (i > 0) sb.append(", ")
            sb.append(decompileNode(node.jjtGetChild(i), indent))
        }
        sb.append(")")
        return sb.toString()
    }

    private fun decompileFormalParam(fp: Node): String {
        val sb = StringBuilder()
        if (boolField(fp, "isFinal")) sb.append("final ")
        if (fp.jjtGetNumChildren() > 0) {
            sb.append(decompileType(fp.jjtGetChild(0)))
            sb.append(" ")
        }
        if (boolField(fp, "isVarArgs")) sb.append("...")
        sb.append(strField(fp, "name") ?: "?")
        return sb.toString()
    }

    private fun decompileType(type: Node): String = strField(type, "typeText") ?: "?"

    private fun decompileReturnType(rt: Node): String {
        if (boolField(rt, "isVoid")) return "void"
        if (rt.jjtGetNumChildren() > 0) return decompileType(rt.jjtGetChild(0))
        return "void"
    }

    @Suppress("RemoveRedundantQualifierName")
    private fun decompilePrimitiveType(pt: Node): String = when (typeField(pt, "type")) {
        java.lang.Boolean.TYPE -> "boolean"
        java.lang.Character.TYPE -> "char"
        java.lang.Byte.TYPE -> "byte"
        java.lang.Short.TYPE -> "short"
        java.lang.Integer.TYPE -> "int"
        java.lang.Long.TYPE -> "long"
        java.lang.Float.TYPE -> "float"
        java.lang.Double.TYPE -> "double"
        java.lang.Void.TYPE -> "void"
        else -> typeField(pt, "type")?.name ?: "?"
    }

    private fun decompileAssignment(assn: Node, indent: Int): String {
        val nc = assn.jjtGetNumChildren()
        if (nc == 0) return "?"
        val lhs = decompileNode(assn.jjtGetChild(0), indent)
        val opRaw = field(assn, "operator") as? Int
        // null operator + single child = naked expression statement, not a real assignment
        if (opRaw == null || nc == 1) return lhs
        val rhs = decompileNode(assn.jjtGetChild(1), indent)
        return dbg(assn, "$lhs ${operatorImage(opRaw)} $rhs")
    }

    private fun decompileTernary(node: Node, indent: Int): String {
        val nc = node.jjtGetNumChildren()
        if (nc < 3) return decompileChildren(node, ", ", indent)
        val c = decompileNode(node.jjtGetChild(0), indent)
        val t = decompileNode(node.jjtGetChild(1), indent)
        val f = decompileNode(node.jjtGetChild(2), indent)
        return "($c ? $t : $f)"
    }

    private fun decompileBinary(be: Node, indent: Int): String {
        val lhs = if (be.jjtGetNumChildren() > 0) decompileNode(be.jjtGetChild(0), indent) else "?"
        val rhs = if (be.jjtGetNumChildren() > 1) decompileNode(be.jjtGetChild(1), indent) else ""
        return dbg(be, "$lhs ${operatorImage(intField(be, "kind"))} $rhs")
    }

    private fun decompileUnary(ue: Node, indent: Int): String {
        val operand = if (ue.jjtGetNumChildren() > 0) decompileNode(ue.jjtGetChild(0), indent) else "?"
        val op = operatorImage(intField(ue, "kind"))
        return if (boolField(ue, "postfix")) "$operand$op" else "$op$operand"
    }

    private fun decompileCast(node: Node, indent: Int): String {
        if (node.jjtGetNumChildren() < 2) return decompileChildren(node, ", ", indent)
        val type = decompileNode(node.jjtGetChild(0), indent)
        val expr = decompileNode(node.jjtGetChild(1), indent)
        return "(($type) $expr)" // I don't care, AI fails to fix this, so I'll just take the shortcut
    }

    private fun decompilePrimaryExpr(node: Node, indent: Int): String {
        val nc = node.jjtGetNumChildren()
        if (nc == 0) return ""
        val sb = StringBuilder()
        val prefix = node.jjtGetChild(0)
        when (prefix.getId()) {
            21 -> sb.append(decompileMethodInvocation(prefix, indent))
            28 -> sb.append(decompileAllocation(prefix, indent))
            22 -> sb.append(decompileLambda(prefix, indent))
            else -> sb.append(decompileNode(prefix, indent))
        }
        for (i in 1 until nc) {
            sb.append(decompilePrimarySuffix(node.jjtGetChild(i), indent))
        }
        return dbg(node, sb.toString())
    }

    private fun decompilePrimarySuffix(sfx: Node, indent: Int): String {
        return when (intField(sfx, "operation")) {
            1 -> {
                val idx = if (sfx.jjtGetNumChildren() > 0) decompileNode(sfx.jjtGetChild(0), indent) else ""
                if (boolField(sfx, "slice")) {
                    val si = StringBuilder(if (boolField(sfx, "safeNavigate")) "?.[" else "[")
                    if (boolField(sfx, "hasLeftIndex") && sfx.jjtGetNumChildren() > 0)
                        si.append(decompileNode(sfx.jjtGetChild(0), indent))
                    si.append(":")
                    if (boolField(sfx, "hasRightIndex")) {
                        val ri = if (boolField(sfx, "hasLeftIndex")) 1 else 0
                        if (ri < sfx.jjtGetNumChildren())
                            si.append(decompileNode(sfx.jjtGetChild(ri), indent))
                    }
                    si.append("]")
                    si.toString()
                } else {
                    if (boolField(sfx, "safeNavigate")) "?.[$idx]" else "[$idx]"
                }
            }

            2 -> {
                val name = strField(sfx, "field") ?: "?"
                if (sfx.jjtGetNumChildren() > 0)
                    ".$name${decompileArgs(sfx.jjtGetChild(0), indent)}"
                else
                    ".$name"
            }

            3 -> {
                val expr = if (sfx.jjtGetNumChildren() > 0) decompileNode(sfx.jjtGetChild(0), indent) else "?"
                ".($expr)"
            }

            4 -> {
                val alloc = if (sfx.jjtGetNumChildren() > 0) decompileNode(sfx.jjtGetChild(0), indent) else ""
                ".$alloc"
            }

            5 -> "::${strField(sfx, "field") ?: "?"}"
            6 -> ".class"
            else -> "/* unknown suffix op=${intField(sfx, "operation")} */"
        }
    }

    private fun decompileMethodInvocation(node: Node, indent: Int): String {
        val name = if (node.jjtGetNumChildren() > 0) decompileNode(node.jjtGetChild(0), indent) else "?"
        val args = if (node.jjtGetNumChildren() > 1) decompileArgs(node.jjtGetChild(1), indent) else "()"
        return "$name$args"
    }

    private fun decompileLambda(le: Node, indent: Int): String {
        val singleParamName = strField(le, "singleParamName"); return if (singleParamName != null) {
            val body = if (le.jjtGetNumChildren() > 0) decompileNode(le.jjtGetChild(0), indent) else ""
            "$singleParamName -> $body"
        } else {
            val params = if (le.jjtGetNumChildren() > 0) decompileNode(le.jjtGetChild(0), indent) else "()"
            val body = if (le.jjtGetNumChildren() > 1) decompileNode(le.jjtGetChild(1), indent) else ""
            "$params -> $body"
        }
    }

    private fun decompileLiteral(lit: Node): String {
        val result = when (val v = field(lit, "value")) {
            null -> "null"
            is String -> quote(v)
            is Char -> "'${escapeChar(v)}'"
            is Boolean -> v.toString()
            is Number -> v.toString()
            is Primitive -> when (v) {
                Primitive.NULL -> "null"
                Primitive.TRUE -> "true"
                Primitive.FALSE -> "false"
                Primitive.VOID -> "void"
                else -> {
                    when (val actual = v.getValue()) {
                        is Char -> "'${escapeChar(actual)}'"
                        is String -> quote(actual)
                        is Boolean -> actual.toString()
                        is Number -> actual.toString()
                        else -> actual?.toString() ?: "null"
                    }
                }
            }

            else -> v.toString()
        }
        return dbg(lit, result)
    }

    private fun decompileArgs(node: Node, indent: Int): String {
        val sb = StringBuilder("(")
        for (i in 0 until node.jjtGetNumChildren()) {
            if (i > 0) sb.append(", ")
            sb.append(decompileNode(node.jjtGetChild(i), indent))
        }
        sb.append(")")
        return dbg(node, sb.toString())
    }

    private fun decompileAllocation(alloc: Node, indent: Int): String {
        val nc = alloc.jjtGetNumChildren()
        if (nc == 0) return "new ?()"
        var i: Int
        val sb = StringBuilder("new ")
        val first = alloc.jjtGetChild(0)
        if (first.getId() == 29) {
            i = 1
            val tn = if (i < nc && alloc.jjtGetChild(i).getId() == 11)
                decompileNode(alloc.jjtGetChild(i++), indent) else "?"
            sb.append(tn)
            sb.append(decompileArrayDims(first, indent))
        } else {
            sb.append(decompileNode(first, indent))
            i = 1
            if (i < nc && alloc.jjtGetChild(i).getId() == 27) {
                sb.append(decompileArgs(alloc.jjtGetChild(i++), indent))
            } else sb.append("()")
        }
        while (i < nc) {
            val child = alloc.jjtGetChild(i++)
            when (child.getId()) {
                8 -> sb.append(" ${decompileArrayInit(child, indent)}")
                3 -> sb.append(" ${decompileBlock(child, indent)}")
                else -> sb.append(" ${decompileNode(child, indent)}")
            }
        }
        return dbg(alloc, sb.toString())
    }

    private fun decompileArrayDims(ad: Node, indent: Int): String {
        val sb = StringBuilder()
        var ci = 0
        val nc = ad.jjtGetNumChildren()
        for (i in 0 until intField(ad, "numDefinedDims")) {
            sb.append("[")
            if (ci < nc) sb.append(decompileNode(ad.jjtGetChild(ci++), indent))
            sb.append("]")
        }
        return sb.toString()
    }

    private fun decompileLabeledStmt(ls: Node, indent: Int): String {
        val stmt = if (ls.jjtGetNumChildren() > 0) decompileNode(ls.jjtGetChild(0), indent) else ""
        return "${strField(ls, "label") ?: "?"}: $stmt"
    }

    private fun decompileSwitch(node: Node, indent: Int): String {
        if (node.jjtGetNumChildren() == 0) return "switch(?) {}"
        val expr = decompileNode(node.jjtGetChild(0), indent)
        val sb = StringBuilder("switch ($expr) {\n")
        for (i in 1 until node.jjtGetNumChildren()) {
            val cs = decompileNode(node.jjtGetChild(i), indent + 1)
            if (cs.isNotEmpty()) {
                sb.append("  ".repeat(indent + 1))
                sb.append(cs)
                if (!cs.endsWith(";") && !cs.endsWith("}") && !cs.endsWith("{\n") && !cs.endsWith(":"))
                    sb.append(";")
                sb.append("\n")
            }
        }
        sb.append("  ".repeat(indent))
        sb.append("}")
        return sb.toString()
    }

    private fun decompileSwitchLabel(sl: Node, indent: Int): String {
        return if (boolField(sl, "isDefault")) "default:"
        else "case ${if (sl.jjtGetNumChildren() > 0) decompileNode(sl.jjtGetChild(0), indent) else ""}:"
    }

    private fun decompileIf(ifs: Node, indent: Int): String {
        val nc = ifs.jjtGetNumChildren()
        if (nc == 0) return "if (?) {}"
        val cond = decompileNode(ifs.jjtGetChild(0), indent)
        val thenB = if (nc > 1) ifs.jjtGetChild(1) else null
        val sb = StringBuilder("if ($cond) ")
        sb.append(if (thenB != null) decompileNode(thenB, indent) else "{}")
        if (nc > 2) {
            sb.append(" else ")
            sb.append(decompileNode(ifs.jjtGetChild(2), indent))
        }
        return sb.toString()
    }

    private fun decompileWhile(ws: Node, indent: Int): String {
        val nc = ws.jjtGetNumChildren()
        return if (boolField(ws, "isDoStatement")) {
            val body = if (nc > 0) decompileNode(ws.jjtGetChild(0), indent) else "{}"
            val cond = if (nc > 1) decompileNode(ws.jjtGetChild(1), indent) else "?"
            "do $body while ($cond);"
        } else {
            val cond = if (nc > 0) decompileNode(ws.jjtGetChild(0), indent) else "?"
            val body = if (nc > 1) decompileNode(ws.jjtGetChild(1), indent) else "{}"
            "while ($cond) $body"
        }
    }

    private fun decompileFor(fs: Node, indent: Int): String {
        var i = 0
        val nc = fs.jjtGetNumChildren()
        val init = if (boolField(fs, "hasForInit") && 0 < nc) decompileNode(fs.jjtGetChild(i++), indent) else ""
        val cond = if (boolField(fs, "hasExpression") && i < nc) decompileNode(fs.jjtGetChild(i++), indent) else ""
        val upd = if (boolField(fs, "hasForUpdate") && i < nc) decompileNode(fs.jjtGetChild(i++), indent) else ""
        val body = if (i < nc) decompileNode(fs.jjtGetChild(i), indent) else "{}"
        return "for ($init; $cond; $upd) $body"
    }

    private fun decompileEnhancedFor(ef: Node, indent: Int): String {
        val nc = ef.jjtGetNumChildren()
        val typeStr = if (nc > 0) "${decompileNode(ef.jjtGetChild(0), indent)} " else ""
        val iter = if (nc > 1) decompileNode(ef.jjtGetChild(1), indent) else "?"
        val body = if (nc > 2) decompileNode(ef.jjtGetChild(2), indent) else "{}"
        val finalStr = if (boolField(ef, "isFinal")) "final " else ""
        return "for ($finalStr$typeStr${strField(ef, "varName") ?: "?"} : $iter) $body"
    }

    private fun decompileTypedVarDecl(tvd: Node, indent: Int): String {
        val nc = tvd.jjtGetNumChildren()
        if (nc == 0) return ""
        var i = 0
        val typeStr = decompileNode(tvd.jjtGetChild(i++), indent)
        val sb = StringBuilder()
        val tvdMods = modifiers(tvd); if (tvdMods != null && tvdMods.modifiers != 0) sb.append(modifiersString(tvdMods)).append(" ")
        sb.append(typeStr).append(" ")
        val decls = mutableListOf<String>()
        while (i < nc) decls.add(decompileNode(tvd.jjtGetChild(i++), indent))
        sb.append(decls.joinToString(", "))
        return sb.toString()
    }

    private fun decompileReturn(rs: Node, indent: Int): String = when (intField(rs, "kind")) {
        47 -> {
            val value = if (rs.jjtGetNumChildren() > 0) decompileNode(rs.jjtGetChild(0), indent) else ""
            if (value.isNotEmpty()) "return $value;" else "return;"
        }

        13 -> "break;"
        20 -> "continue;"
        else -> {
            val value = if (rs.jjtGetNumChildren() > 0) decompileNode(rs.jjtGetChild(0), indent) else ""
            "return $value;"
        }
    }

    private fun decompileTry(node: Node, indent: Int): String {
        val nc = node.jjtGetNumChildren()
        var i = 0
        val sb = StringBuilder()
        if (0 < nc && node.jjtGetChild(i).getId() == 43) {
            val twr = node.jjtGetChild(i++)
            sb.append("try (")
            val rs = mutableListOf<String>()
            for (j in 0 until twr.jjtGetNumChildren()) rs.add(decompileNode(twr.jjtGetChild(j), indent))
            sb.append(rs.joinToString("; "))
            sb.append(") ")
        } else sb.append("try ")
        if (i < nc) sb.append(decompileNode(node.jjtGetChild(i++), indent))
        while (i < nc && node.jjtGetChild(i).getId() == 42) {
            val cp = node.jjtGetChild(i++)
            val cb = if (i < nc) node.jjtGetChild(i++) else null
            sb.append(" catch (")
            sb.append(decompileMultiCatch(cp))
            sb.append(") ")
            if (cb != null) sb.append(decompileNode(cb, indent))
        }
        if (i < nc && node.jjtGetChild(i).getId() == 3) {
            sb.append(" finally ")
            sb.append(decompileNode(node.jjtGetChild(i), indent))
        }
        return sb.toString()
    }

    private fun decompileMultiCatch(mc: Node): String {
        val sb = StringBuilder()
        if (boolField(mc, "isFinal")) sb.append("final ")
        val nc = mc.jjtGetNumChildren()
        if (nc == 0) sb.append(strField(mc, "name") ?: "?") else {
            sb.append((0 until nc).joinToString(" | ") { decompileType(mc.jjtGetChild(it)) })
            sb.append(" ${strField(mc, "name") ?: "?"}")
        }
        return sb.toString()
    }

    private fun decompileAutoCloseable(ac: Node, indent: Int): String {
        val sb = StringBuilder()
        val acTypeName = strField(ac, "typeName"); if (acTypeName != null) sb.append(acTypeName).append(" ")
        sb.append(strField(ac, "name") ?: "?")
        if (ac.jjtGetNumChildren() > 0) sb.append(" = ${decompileNode(ac.jjtGetChild(0), indent)}")
        return sb.toString()
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private fun field(obj: Any, name: String): Any? {
        var cls: Class<*>? = obj.javaClass
        while (cls != null) {
            try {
                val f = cls.getDeclaredField(name)
                f.isAccessible = true
                return f.get(obj)
            } catch (ignored: NoSuchFieldException) {
                cls = cls.superclass
            }
        }
        return null
    }

    private fun boolField(node: Node, name: String): Boolean = field(node, name) as? Boolean ?: false

    private fun intField(node: Node, name: String): Int = (field(node, name) as? Number)?.toInt() ?: 0

    private fun strField(node: Node, name: String): String? = field(node, name) as? String

    private fun modifiers(node: Node): Modifiers? = field(node, "modifiers") as? Modifiers

    private fun typeField(node: Node, name: String): Class<*>? = field(node, name) as? Class<*>
    private fun decompileChildren(node: Node, sep: String, indent: Int): String =
        (0 until node.jjtGetNumChildren()).joinToString(sep) { decompileNode(node.jjtGetChild(it), indent) }

    private fun decompileChild(node: Node, index: Int, indent: Int): String =
        if (index < node.jjtGetNumChildren()) decompileNode(node.jjtGetChild(index), indent) else "?"

    private fun modifiersString(m: Modifiers): String {
        val mods = m.modifiers
        val list = mutableListOf<String>()
        if (mods and 0x0001 != 0) list.add("public")
        if (mods and 0x0002 != 0) list.add("private")
        if (mods and 0x0004 != 0) list.add("protected")
        if (mods and 0x0008 != 0) list.add("static")
        if (mods and 0x0010 != 0) list.add("final")
        if (mods and 0x0020 != 0) list.add("synchronized")
        if (mods and 0x0040 != 0) list.add("volatile")
        if (mods and 0x0080 != 0) list.add("transient")
        if (mods and 0x0100 != 0) list.add("native")
        if (mods and 0x0400 != 0) list.add("abstract")
        if (mods and 0x0800 != 0) list.add("strictfp")
        if (mods and 0x1000 != 0) list.add("synthetic")
        if (mods and 0x2000 != 0) list.add("annotation")
        if (mods and 0x4000 != 0) list.add("enum")
        if (mods and 0x8000 != 0) list.add("mandated")
        if (mods and 0x10000 != 0) list.add("default")
        return list.joinToString(" ")
    }

    private fun operatorImage(kind: Int): String = when (kind) {
        11 -> "abstract"
        12 -> "boolean"
        13 -> "break"
        14 -> "class"
        15 -> "byte"
        16 -> "case"
        17 -> "catch"
        18 -> "char"
        20 -> "continue"
        21 -> "default"
        22 -> "do"
        23 -> "double"
        24 -> "else"
        25 -> "enum"
        26 -> "extends"
        27 -> "false"
        28 -> "final"
        30 -> "float"
        31 -> "for"
        33 -> "if"
        34 -> "implements"
        35 -> "import"
        36 -> "instanceof"
        37 -> "int"
        38 -> "interface"
        39 -> "long"
        41 -> "new"
        42 -> "null"
        43 -> "package"
        44 -> "private"
        45 -> "protected"
        46 -> "public"
        47 -> "return"
        48 -> "short"
        49 -> "static"
        50 -> "strictfp"
        51 -> "switch"
        52 -> "synchronized"
        54 -> "throw"
        55 -> "throws"
        56 -> "true"
        57 -> "try"
        58 -> "void"
        59 -> "volatile"
        60 -> "when"
        61 -> "while"
        76 -> "("
        77 -> ")"
        82 -> ";"
        84 -> "."
        85 -> "="
        86 -> ">"
        88 -> "<"
        90 -> "!"
        91 -> "~"
        92 -> "=="
        93 -> "<="
        95 -> ">="
        97 -> "!="
        98 -> "||"
        100 -> "&&"
        102 -> "++"
        103 -> "--"
        104 -> "+"
        105 -> "-"
        106 -> "*"
        107 -> "/"
        108 -> "&"
        110 -> "|"
        112 -> "^"
        114 -> "%"
        116 -> "**"
        118 -> "<<"
        120 -> ">>"
        122 -> ">>>"
        124 -> "+="
        125 -> "-="
        126 -> "*="
        127 -> "/="
        128 -> "&="
        130 -> "|="
        132 -> "^="
        134 -> "%="
        136 -> "**="
        138 -> "<<="
        140 -> ">>="
        142 -> ">>>="
        144 -> "->"
        145 -> "<=>"
        146 -> "??="
        147 -> "??"
        148 -> "?:"
        149 -> "?"
        150 -> ":"
        151 -> "::"
        152 -> "..."
        else -> "/* op:$kind */"
    }

    private fun quote(s: String): String {
        val sb = StringBuilder()
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (c.code < 0x20) sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        sb.append('"')
        return sb.toString()
    }

    private fun escapeChar(c: Char): String = when (c) {
        '\'' -> "\\'"
        '\\' -> "\\\\"
        '\n' -> "\\n"
        '\r' -> "\\r"
        '\t' -> "\\t"
        else -> c.toString()
    }
}
