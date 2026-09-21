@file:Suppress("SameParameterValue")

package com.coc.zkqcode.loadjar

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.coc.zkqcode.interfaces.MainCode
import com.coc.zkqcode.core.data.database.GlobalVars
import com.coc.zkqcode.statehelper.AppMode
import com.coc.zkqcode.statehelper.AppStateManager
import android.os.Build
import dalvik.system.DexClassLoader
import dalvik.system.InMemoryDexClassLoader
import com.coc.zkqcode.core.util.fileactions.LogHelper
import java.nio.ByteBuffer
import timber.log.Timber
import java.io.File

class Loadjar(private val context: Context) {

    private var pluginUI: MainCode? = null

    /**
     * Main composable function that handles all loading logic and displays the UI
     */
    @Composable
    fun LoadAndShowUI(loadStatus: String, onClose: () -> Unit) {
        if (loadStatus == "Plugin loaded successfully") {
            // Load the UI from jar
            pluginUI?.ShowMainUI(context, onClose)
        } else {
            Column(modifier = Modifier.background(Color.White)) {
                Text(text = loadStatus)
                Button(onClick = {
                    AppStateManager.setMode(AppMode.Run)
                    onClose()
                }) {
                    Text(text = "关闭悬浮窗")
                }
            }
        }
    }

    /**
     * Start the plugin loading process
     */
    fun startLoading(assetFileName: String = "code.jar", onStatusChange: (String) -> Unit) {
        onStatusChange("加载中...")
        // Create assets folder and extract assets (skipping existing .jar files)
        extractAllAssets()

        val assetsDir = File(context.filesDir, "assets")
        val assetList = assetsDir.list() ?: emptyArray()

        // Find the unencrypted jar with the highest timestamp number (latest build, dev/debug)
        val timestampJar = assetList
            .filter { it.endsWith(".jar") && it.removeSuffix(".jar").toLongOrNull() != null }
            .maxByOrNull { it.removeSuffix(".jar").toLong() }

        // Find the encrypted jar with the highest timestamp number (encrypted_<timestamp>.jar)
        val encryptedTimestampJar = assetList
            .filter {
                it.startsWith("encrypted_") && it.endsWith(".jar") &&
                        it.removePrefix("encrypted_").removeSuffix(".jar").toLongOrNull() != null
            }
            .maxByOrNull { it.removePrefix("encrypted_").removeSuffix(".jar").toLong() }

        val success = if (timestampJar != null) {
            loadPluginFromAssets(timestampJar)
        } else if (encryptedTimestampJar != null) {
            loadEncryptedPlugin(encryptedTimestampJar)
        } else if (assetList.contains("encrypted_code.jar")) {
            loadEncryptedPlugin("encrypted_code.jar")
        } else {
            // Fallback to whatever was passed or fail
            loadPluginFromAssets(assetFileName)
        }

        val finalStatus = if (success) {
            "Plugin loaded successfully"
        } else {
            "Failed to load plugin"
        }
        onStatusChange(finalStatus)
    }

    /**
     * Extract jar/dex from assets to private directory and load the plugin UI
     */
    private fun loadPluginFromAssets(assetFileName: String): Boolean {

        return try {
            // Get private directory for extracted files
            val privateDir = File(context.filesDir, "assets")
            val dexOutputDir = context.codeCacheDir

            // Load jar from the assets directory in filesDir
            val jarFile = File(privateDir, assetFileName)

            if (!jarFile.exists()) {
                // File not found in private directory
                return false
            }

            // Android 16+ requires DEX files to be non-writable
            jarFile.setReadOnly()

            // Load the jar with a parent that HIDES the plugin classes: the plugin sources are also
            // compiled into the host APK, so a plain parent-first parent would always return the
            // APK's (stale) copy and the JAR hot-update would never take effect.
            // See PluginHidingClassLoader.
            val classLoader = DexClassLoader(
                jarFile.absolutePath, dexOutputDir.absolutePath, null,
                PluginHidingClassLoader(context.classLoader)
            )

            // Load the plugin implementation class
            val pluginClass = classLoader.loadClass("com.coc.zkqcode.jar.ui.EnterMainCode")
            val instance = pluginClass.getDeclaredConstructor().newInstance() as MainCode
            pluginUI = instance
            GlobalVars.pluginUI = instance

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun loadEncryptedPlugin(assetFileName: String): Boolean {
        return try {
            // 1. Read encrypted bytes
            val file = File(context.filesDir, "assets/$assetFileName")
            if (!file.exists()) return false
            val encryptedBytes = file.readBytes()

            // 2. Decrypt
            val decryptedBytes = com.coc.zkqcode.nativehelper.RustTools.decryptJar(encryptedBytes)
            if (decryptedBytes.isEmpty()) return false

            val classLoader: ClassLoader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 3. Load from memory (Android 8.0+)
                // Since the decrypted bytes are a JAR, we need to extract classes.dex first
                var dexBytes: ByteArray? = null
                java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(decryptedBytes)).use { zipStream ->
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        if (entry.name == "classes.dex") {
                            dexBytes = zipStream.readBytes()
                            break
                        }
                        entry = zipStream.nextEntry
                    }
                }

                if (dexBytes == null) {
                    return false
                }

                val buffer = ByteBuffer.wrap(dexBytes)
                InMemoryDexClassLoader(buffer, PluginHidingClassLoader(context.classLoader))
            } else {
                // 4. Fallback for older versions: Use in-memory file descriptor (memfd/ashmem)
                LogHelper.showDebugInfo(
                    "loadEncryptedPlugin: Using fallback for API ${Build.VERSION.SDK_INT}"
                )

                val fd = com.coc.zkqcode.nativehelper.RustTools.createInMemoryDex(decryptedBytes)

                if (fd < 0) {
                    Timber.e(
                        "loadEncryptedPlugin: Failed to create in-memory dex, fd=$fd"
                    )
                    return false
                }
                LogHelper.showDebugInfo(
                    "loadEncryptedPlugin: Created in-memory dex, fd=$fd"
                )

                // Use procfs path to the file descriptor
                val dexPath = "/proc/self/fd/$fd"
                val dexOutputDir = context.codeCacheDir

                LogHelper.showDebugInfo("loadEncryptedPlugin: Loading from $dexPath")

                DexClassLoader(
                    dexPath, dexOutputDir.absolutePath, null,
                    PluginHidingClassLoader(context.classLoader)
                )
            }

            // 5. Instantiate Plugin
            val pluginClass = classLoader.loadClass("com.coc.zkqcode.jar.ui.EnterMainCode")
            val instance = pluginClass.getDeclaredConstructor().newInstance() as MainCode
            pluginUI = instance
            GlobalVars.pluginUI = instance

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Create assets folder and extract assets into it, skipping .jar files that already exist
     */
    private fun extractAllAssets(): File {
        val privateDir = context.filesDir
        val assetsDir = File(privateDir, "assets")

        if (!assetsDir.exists()) {
            assetsDir.mkdirs()
        }

        try {
            val assetList = context.assets.list("") ?: emptyArray()
            for (assetName in assetList) {

                // --- MODIFIED FILTER RULE ---
                // Only proceed if the file ends with .jar or .tflite (case-insensitive)
                if (!assetName.endsWith(".jar", ignoreCase = true) && !assetName.endsWith(".tflite", ignoreCase = true)) {
                    continue
                }
                // ----------------------------

                try {
                    val outputFile = File(assetsDir, assetName)
                    // If it's a jar, and it already exists, don't overwrite it
                    if (assetName.endsWith(".jar", ignoreCase = true) && outputFile.exists()) {
                        // Ensure existing JARs are read-only (required by Android 16+)
                        outputFile.setReadOnly()
                        continue
                    }

                    context.assets.open(assetName).use { input ->
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    // Make newly extracted JARs read-only (required by Android 16+)
                    if (assetName.endsWith(".jar", ignoreCase = true)) {
                        outputFile.setReadOnly()
                    }
                } catch (e: Exception) {
                    // This handles cases where .jar might be a directory name (unlikely but safe)
                    println("Failed to copy asset: $assetName, error: $e")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return assetsDir
    }
}

/** Package prefix of everything that the hot-update JAR ships. */
private const val PLUGIN_PACKAGE = "com.coc.zkqcode.jar."

/**
 * Parent loader for the hot-update JAR that HIDES the plugin classes.
 *
 * The plugin sources are also part of the host APK (the `com/coc/zkqcode/jar` source folder), so the
 * APK dex contains a copy of every plugin class. `DexClassLoader` / `InMemoryDexClassLoader` are
 * parent-first, which means that copy would always win and every `encrypted_*.jar` hot-update would
 * be silently ignored - only reinstalling the APK changed plugin behaviour.
 *
 * By refusing the plugin package here, the JAR's own `findClass` is the only remaining source for
 * those names, so the freshly built JAR always wins. Every other name (MainCode, GlobalVars,
 * TouchActions, RustTools, androidx, kotlin, ...) is still resolved by [delegate], so the class
 * identity of the host APIs the plugin talks to does not change.
 */
private class PluginHidingClassLoader(private val delegate: ClassLoader) : ClassLoader(delegate) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        // Not "return super.loadClass(...)" on purpose: the plugin must come from the JAR, and the
        // JAR's own loader falls back to this parent for anything it does not ship.
        if (name.startsWith(PLUGIN_PACKAGE)) throw ClassNotFoundException(name)
        return delegate.loadClass(name)
    }
}
