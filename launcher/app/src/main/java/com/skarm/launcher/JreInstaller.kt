package com.skarm.launcher

import android.content.Context
import android.os.Process
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Extracts the bundled JRE 25 (FCL-Team multiarch build) onto internal storage.
 */
object JreInstaller {

    private const val TAG = "JreInstaller"
    private const val DIR_NAME = "jre25"
    private const val STAMP_NAME = ".version"
    private val assetDir: String
        get() = if (Process.is64Bit()) "jre25" else "jre25-arm32"

    fun homeDir(context: Context): File = File(context.filesDir, DIR_NAME)

    /** Path to libjvm.so once installed. */
    fun libjvmPath(context: Context): File =
        File(homeDir(context), "lib/server/libjvm.so")

    fun isInstalled(context: Context): Boolean {
        val home = homeDir(context)
        val stamp = File(home, STAMP_NAME)
        if (!stamp.isFile) return false
        if (!libjvmPath(context).isFile) return false
        val have = stamp.readText().trim()
        return have == stampValue(context)
    }

    private fun stampValue(context: Context): String =
        "${bundledVersion(context)}+$assetDir+${appStamp(context)}"

    /**
     * The installed app's own version, folded into the stamp below.
     *
     * The component version alone is not enough: shipping a rebuilt runtime under an
     * unchanged version string would leave the previous unpack in place, so the game
     * keeps running last release's files. Any app update changes this.
     */
    private fun appStamp(context: Context): String = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        // PackageInfoCompat, not longVersionCode: that getter is API 28 and this module
        // ships to API 26, where the miss is a NoSuchMethodError -- an Error, which the
        // catch below would not stop.
        "${info.versionName}-${PackageInfoCompat.getLongVersionCode(info)}"
    } catch (e: Exception) {
        "unknown"
    }


    private fun bundledVersion(context: Context): String =
        context.assets.open("$assetDir/version").use { it.bufferedReader().readText().trim() }

    internal fun archAssetName(is64Bit: Boolean): String =
        if (is64Bit) "bin-arm64.tar.xz" else "bin-arm.tar.xz"

    /**
     * Installs (or reinstalls) the JRE. Safe to call from a background thread.
     * Progress lines are reported via [onProgress] for the UI to surface.
     */
    fun install(context: Context, onProgress: (String) -> Unit = {}) {
        val home = homeDir(context)
        if (home.exists()) {
            onProgress("Clearing previous runtime…")
            home.deleteRecursively()
        }
        home.mkdirs()

        onProgress("Unpacking JRE base…")
        extractAsset(context, "$assetDir/universal.tar.xz", home)

        val archAsset = archAssetName(Process.is64Bit())
        onProgress("Unpacking JRE native ($archAsset)…")
        extractAsset(context, "$assetDir/$archAsset", home)

        markExecutable(File(home, "bin"))
        markExecutable(File(home, "lib/server/libjvm.so"))
        markExecutable(File(home, "lib/jspawnhelper"))

        if (!Process.is64Bit()) {
            val source = File(context.applicationInfo.nativeLibraryDir, "libawt_xawt.so")
            source.copyTo(File(home, "lib/libawt_xawt.so"), overwrite = true)
        }

        onProgress("Staging libGL.so…")
        stageLibGL(context, home)

        File(home, STAMP_NAME).writeText(stampValue(context))
        onProgress("Runtime ready.")
        Log.i(TAG, "JRE 25 installed at $home; libjvm exists=${libjvmPath(context).exists()}")
    }

    private fun extractAsset(context: Context, assetPath: String, into: File) {
        context.assets.open(assetPath).use { raw ->
            BufferedInputStream(raw).use { buf ->
                XZInputStream(buf).use { xz ->
                    TarArchiveInputStream(xz).use { tar -> extractTar(tar, into) }
                }
            }
        }
    }

    private fun extractTar(tar: TarArchiveInputStream, into: File) {
        val rootPath = into.canonicalPath + File.separator
        while (true) {
            val entry = tar.nextEntry ?: break
            // Strip the leading "./" that some tar producers prepend.
            val name = entry.name.removePrefix("./").removePrefix("/")
            if (name.isEmpty()) continue
            // These two FCL files are AArch64 even in the ARM32 runtime image.
            if (!Process.is64Bit() && name in listOf("lib/libawt_xawt.so", "lib/jspawnhelper")) continue

            val target = File(into, name).canonicalFile
            if (!target.path.startsWith(rootPath)) {
                error("tar entry escapes target dir: ${entry.name}")
            }

            if (entry.isDirectory) {
                target.mkdirs()
                continue
            }
            if (entry.isSymbolicLink) {
                // Java doesn't ship symlink creation in stdlib without nio...
                java.nio.file.Files.createSymbolicLink(
                    target.toPath(),
                    java.nio.file.Paths.get(entry.linkName),
                )
                continue
            }

            target.parentFile?.mkdirs()
            FileOutputStream(target).use { copy(tar, it) }

            // Preserve the executable bit from the archive's unix mode.
            if (entry.mode and 0b001_001_001 != 0) {
                target.setExecutable(true, false)
            }
        }
    }

    private fun stageLibGL(context: Context, home: File) {
        val source = File(context.applicationInfo.nativeLibraryDir, "libgl4es.so")
        if (!source.isFile) {
            error("libgl4es.so not found in app nativeLibraryDir at ${source.path}")
        }
        val target = File(home, "lib/libGL.so")
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
        target.setReadable(true, false)
        target.setExecutable(true, false)
    }

    private fun markExecutable(path: File) {
        if (!path.exists()) return
        if (path.isDirectory) {
            path.listFiles()?.forEach { markExecutable(it) }
        } else {
            path.setExecutable(true, false)
        }
    }

    private fun copy(input: InputStream, output: FileOutputStream) {
        val buf = ByteArray(64 * 1024)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            output.write(buf, 0, n)
        }
    }
}
