package me.teble.xposed.autodaily.utils

import android.content.Context
import cn.hutool.core.io.FileUtil
import cn.hutool.core.io.IoUtil
import me.teble.xposed.autodaily.hook.base.moduleClassLoader
import java.io.BufferedInputStream
import java.io.File

object NativeUtil {

    private val is64Bit: Boolean
        get() {
            val clazz = Class.forName("dalvik.system.VMRuntime")
            return clazz.new().invoke("is64Bit") as Boolean
        }

    private fun getLibFilePath(name: String): String {
        return if (is64Bit) "lib/arm64-v8a/lib${name}.so" else "lib/armeabi-v7a/lib$name.so"
    }

    fun getNativeLibrary(context: Context, libName: String): File {
        val soDir = File(context.filesDir, "xa_lib")
        if (soDir.isFile) {
            soDir.delete()
        }
        if (!soDir.exists()) {
            soDir.mkdirs()
        }
        val soPath = getLibFilePath(libName)
        val soFile = File(soDir, libName)
        val libStream =
            BufferedInputStream(moduleClassLoader.getResourceAsStream(soPath))
        val tmpSoFile = File(soDir, "$libName.tmp")
        try {
            FileUtil.writeFromStream(libStream, tmpSoFile)
            if (!soFile.exists()) {
                LogUtil.i("so文件不存在，正在尝试加载")
                tmpSoFile.renameTo(soFile)
            } else {
                val oldStream = FileUtil.getInputStream(soFile)
                val newStream = FileUtil.getInputStream(tmpSoFile)
                if (!IoUtil.contentEquals(oldStream, newStream)) {
                    LogUtil.i("so文件版本存在更新，正在重新加载")
                    soFile.delete()
                    tmpSoFile.renameTo(soFile)
                }
            }
            LogUtil.i("加载so文件成功 -> ${soFile.path}")
            return soFile
        } catch (e: Exception) {
            LogUtil.e(e)
            throw e
        } finally {
            if (tmpSoFile.exists()) {
                tmpSoFile.delete()
            }
        }
    }
}