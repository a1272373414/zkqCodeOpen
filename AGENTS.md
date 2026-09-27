Project Context & Architecture
Type: Android Automation Software.
Stack: Kotlin, Jetpack Compose.
Architecture: The app functions as a host loader. It dynamically builds and loads logic from a specific source directory. This app will be run at Root envrionment. For all UI, try to use the componenets under app\src\main\java\com\coc\zkqcode\utils\components\Components.kt. 

When writing UI, if the UI has a key in Schema.kt, then you should display Schema.GLOBAL_SETTINGS.first { it.key == key }.displayName, instead of displaying hardcoded text.

Critical Directory Rules
Dynamic Source Path: app/src/main/java/com/coc/zkqcode/jar
Behavior: Treat all files in this directory as a standalone module. They are packed into a JAR and loaded dynamically at runtime.

I/O & Networking Constraints
Required I/O Method: All file reading and writing that are not in private path must be routed through the WebSocket server.
Server Address: ws://localhost:6839/zkq

When adding debug information, you should use ShowMessage, which can be imported from com.coc.zkqcode.core.util.basic.ShowMessage.

When modify the code, you should add appropriate comments. Write all comments in Chinese (KDoc / block / inline comments), while other content (e.g. identifiers, debug information, variable names or display information) may be written in Chinese or English as appropriate.

When importing a class/object/function or others, you should import the full package name, and only use the last name inside the code.
For example, instead of using com.coc.zkqcode.jar.code.colorschema.ColorSchema in the code, you should import it as import com.coc.zkqcode.jar.code.colorschema.ColorSchema. Then, you can use ColorSchema in the code as ColorSchema.

When the instructions are unclear, you need to ask the user for clarification. Only proceed after you have understood every detail of the instructions.

Debug Environment Rules
Emulator: Always use emulator-5556 for adb commands (screencap, input tap, install, etc.). Specify -s emulator-5556 when needed.

Modification Rules: Only modify the specified feature. Do not make extra changes. Only provide suggestions without implementing them unless asked.

Temporary File Rules
临时文件目录: 所有临时文件（调试截图、日志抓取、试验脚本、解压产物、扫描输出等）一律放在项目根目录的 temp/ 下，不要散落在 tools/、app/ 或其他目录。
按日期分组: temp/ 下按文件产生日期建立 YYYY-MM-DD/ 子目录归档，例如 temp/2026-09-28/infolog.txt。
及时清理: 确认无用的临时文件（一次性解压产物、空日志、重复副本、__pycache__ 等）直接删除，不要长期堆积。
不入库: temp/ 已在 .gitignore 中忽略，提交代码时不要把临时文件加入版本库。
