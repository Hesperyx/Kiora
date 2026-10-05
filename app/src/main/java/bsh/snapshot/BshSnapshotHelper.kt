package bsh.snapshot

import java.io.IOException
import java.io.InputStream
import java.io.InvalidClassException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Arrays
import java.util.Collection
import java.util.Map
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

public object BshSnapshotHelper {
    private val MAGIC = byteArrayOf('B'.code.toByte(), 'S'.code.toByte(), 'H'.code.toByte(), 'S'.code.toByte())
    private const val HEADER_VERSION = 1
    private const val IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private val RANDOM = SecureRandom()

    private fun readExact(input: InputStream, length: Int): ByteArray {
        val data = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = input.read(data, offset, length - offset)
            if (read < 0) throw IOException("BeanShell snapshot unexpected end")
            offset += read
        }
        return data
    }

    public fun writeEncrypted(snapshot: BshSnapshot, out: OutputStream, key: SecretKey) {
        val iv = ByteArray(IV_LENGTH)
        RANDOM.nextBytes(iv)

        out.write(MAGIC)
        out.write(HEADER_VERSION)
        out.write(iv.size)
        out.write(iv)

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            ObjectOutputStream(CipherOutputStream(out, cipher)).use { objectOut ->
                objectOut.writeObject(snapshot)
            }
        } catch (e: GeneralSecurityException) {
            throw IOException("BeanShell snapshot encrypt failed", e)
        }
    }

    public fun readEncrypted(input: InputStream, key: SecretKey): BshSnapshot {
        val magic = readExact(input, MAGIC.size)
        if (!Arrays.equals(magic, MAGIC)) throw IOException("BeanShell snapshot invalid header")

        val version = input.read()
        if (version != HEADER_VERSION) throw IOException("BeanShell snapshot unsupported version: $version")

        val ivLength = input.read()
        if (ivLength <= 0 || ivLength > 32) throw IOException("BeanShell snapshot invalid IV length")

        val iv = readExact(input, ivLength)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            ObjectInputStream(FilteringObjectInputStream(CipherInputStream(input, cipher))).use { objectIn ->
                val obj = objectIn.readObject()
                if (obj !is BshSnapshot) throw InvalidClassException("BeanShell snapshot unexpected payload")
                if (obj.getFormatVersion() != BshSnapshot.FORMAT_VERSION) {
                    throw IOException("BeanShell snapshot unsupported AST format: ${obj.getFormatVersion()}")
                }
                return obj
            }
        } catch (e: GeneralSecurityException) {
            throw IOException("BeanShell snapshot decrypt failed", e)
        } catch (e: ClassNotFoundException) {
            throw IOException("BeanShell snapshot class not found", e)
        }
    }

    private class FilteringObjectInputStream(input: InputStream) : ObjectInputStream(input) {
        override fun resolveClass(desc: ObjectStreamClass): Class<*> {
            val type = super.resolveClass(desc)
            if (!isAllowed(type)) throw InvalidClassException("BeanShell snapshot rejected class: ${type.name}")
            return type
        }
    }

    private fun isAllowed(type: Class<*>): Boolean {
        if (type.isArray) {
            var component: Class<*>? = type.componentType
            while (component != null && component.isArray) component = component.componentType
            return component != null && (component.isPrimitive || isAllowed(component))
        }
        if (type.isPrimitive ||
            Number::class.java.isAssignableFrom(type) ||
            type == String::class.java ||
            type == Boolean::class.java ||
            type == Character::class.java ||
            Collection::class.java.isAssignableFrom(type) ||
            Map::class.java.isAssignableFrom(type) ||
            type == Enum::class.java ||
            Enum::class.java.isAssignableFrom(type) ||
            type.name.startsWith("java.lang.invoke.") ||
            type.name.startsWith("java.lang.constant.")
        ) return true
        return type.name.startsWith("bsh.")
    }
}