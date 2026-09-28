# 开发任务清单（Task Tracking）

> 本文件是 **开发任务的唯一基准（Single Source of Truth）**。
> 功能对比与缺口分析见 `docs/源项目功能对比与移植计划.md`，本文件只管「做什么、做到哪了」。

---

## 一、管理规则（必读）

1. **唯一基准**：所有开发任务的新增、拆分、状态变更、完成判定，**一律以本文件为准**。禁止只在对话里口头确定任务而不回填本文件。
2. **新增任务**：在对应阶段表格**追加一行**（取下一个 `Txx` 编号），并在「更新记录」追加一条（日期 + 新增内容 + 依据）。
3. **更新任务**：修改对应行的「状态」列，并在「更新记录」追加一条（日期 + 任务ID + 状态变化 + 说明）。**不要删除历史记录行**，已完成的任务保留并标记 ✅。
4. **状态图例**（状态列只能取以下之一）：
   - ⬜ 待办（未开始）
   - 🔄 进行中
   - ✅ 已完成
   - ⏸ 暂停（阻塞/等待依赖）
   - ❌ 已取消 / 不抄（须在备注写清原因）
5. **编号规则**：任务 ID 固定为 `T01`、`T02`…… 顺序分配，不回收、不重排；即使中间有任务取消，ID 也不复用。
6. **完成标准**：代码落地 + 必要的真机/模拟器验证通过（`tools/live_*_test` 或 `emulator-5556` 实机标定）+ 无新增 lint 错误，方可在状态列置 ✅。
7. **与计划文档关系**：`docs/源项目功能对比与移植计划.md` 的「§5 开发任务计划」是任务来源；两者的任务 ID/状态以**本文件为准**，计划文档仅供对照参考。
8. **进度同步**：每次更新后，维护底部「进度汇总」的计数（总数 / ✅ / 🔄 / 其他）。

---

## 二、任务列表

### P0 — 基础增强（低风险，非业务缺口）

| ID | 任务 | 目标文件(当前项目) | 工作量 | 优先级 | 源依据 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|
| T01 | 坐标归一化集成 pass | `app/.../zkqcode/jar` CoordNormalizer 集成 | 1天 | 低 | 源 r()/b() | ⬜ | 把散落硬编码字面量替换为归一化调用 |
| T02 | NCC 接入 OCR pipeline | `NccMatcher.kt` 接线 | 1天 | 低 | 源 ncc.c | 🔄 | 已接线为**模板匹配**：`TemplateMatcher`(assets/templates + 区域 NCC，阈值 0.75) + `SceneState` 模板兜底钩子 `TEMPLATE_RULES`，配套 `tools/make_template.py`；OCR pipeline 兜底仍未接；离线自检 0 误命中，待真机验证 |
| T03 | 特征带方向搜索 | `rust_logic/src/color/mod.rs` | 1天 | 低 | 源 函数24a/26a | ✅ | 评估结论：方向参数已端到端接通(Kotlin `ColorSchema.direction`→`findMultiColors`→Rust `findMultiColorsRaw`→`find_multi_colors_internal`)，但原 Rust 仅实现 0/1 且 `dir=1` 误作「右下→左上」。已修正 `find_multi_colors_internal`：`0=左上→右下 / 1=左下→右上 / 2=右上→左下 / 3=右下→左上 / 4=中心向四周 / 5=四周向中心`，对齐源 `函数24a`(默认 dir=1=BL→TR)。`cargo check` 通过。**待办接线**：当前 `MyColors` 所有 `ColorSchema.parse` 第7参 `dir` 写死 0，需在颜色生成器透传源方向值(避免无设备时全量翻方向，建议随 T30 真机标定逐色接入)；运行时须 `cargo build --release` 重新产出 .so 方生效 |
| T19 | 特征标定工具链（自动生成 + 模板裁剪） | `tools/make_feature.py` / `tools/make_template.py` | 1天 | 中 | 实机痛点：文字按钮需逐点量色 | 🔄 | `make_feature.py`：给截图+矩形自动生成 `ColorSchema`(主色众数 + 跨样本稳定偏移点) + 正/负样本自检；`make_template.py`：裁剪模板到 `assets/templates` 并做同口径 NCC 自检（可限区域/阈值）。已离线验证（编辑模式：15 张负样本 0 误命中），待实机使用验证 |

### P1 — 明确缺失，直接抄（高优先）

| ID | 任务 | 目标文件(当前项目) | 工作量 | 优先级 | 源依据 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|
| T04 | 换阵(战争基地阵型切换) | `jar/code/universal/smalltools/ChangeWarBase.kt` | 1天 | 高 | 源 换阵(48) L55700-55783 | ✅ | 新建 `ChangeWarBase.changeWarBaseLayout(account)`：读 `CHANGE_BASE` 开关→取本账号目标阵型号(存 `StorageKeys.WAR_BASE_LAYOUT`，1~6，0/空=不换)→进战争基地编辑→开阵型列表→按阵型号点槽位(>3 先上滑)→解锁则点「装备」。按钮优先文字 `locate()` 定位、坐标作兜底；坐标按 90° 从竖屏换算横屏。挂在 `MainScript` 主循环 `enterMainScreen` 之后。`buildJar` 通过、0 lint |
| T05 | 协议弹窗双确认强化 | `jar/code/universal/smalltools/AgreementPopups.kt` | 0.5天 | 高 | 源 函数10a/301a | ✅ | 新增 `AgreementPopups.handleAgreementPopups()`：用 T35 的 `ChineseTextReader.locate()` 按文字定位「全部接受/同意/QQ登录/登录/检查更新」，腾讯走「双确认」(点同意→下滑协议→再点同意)；挂在 `MainScript` 主循环 `enterMainScreen` 前；`locate()` 已支持全屏(不传区域)。`buildJar` 通过、0 lint |
| T06 | 都城部署精炼 + 实机验证 | `jar/code/clancapital/attack/CapitalAttack.kt` | 2天 | 高 | 源 突袭打野 函数31a | 🔄 | **代码精炼已完成**（2026-09-28）：①修正部署顺序为源 函数31a 的「兵种先铺满所有下兵点 → 兵尽收尾放法术」（原实现误为法术先）；②9 地图目标建筑坐标前置为首个放兵点（兵集中攻击该建筑）。第二轮扫描+网格兜底+9地图补点(由 T22 落地)、总都城币按领币次数统计(精确数字待 OCR)。**真机部署验证仍待 emulator-5556**（与 T30/T34 同阻塞），下兵/法术颜色(CapitalDeployColors)待真机复标 |
| T22 | 都城突袭补齐：造兵按源两套打法+下兵第二轮扫描+9地图定位+总都城币统计+测试日志截图 | `jar/code/clancapital/*`、`colorschema/colorpackage/clancapital/*` | 2天 | 高 | 源 函数28a/29a/30a/31a + 可打地图 | ✅ | 用户 2026-09-26 要求补齐缺口且造兵方案按源；测试期加日志与截图存档 |
| T23 | 聊天界面都城友谊战识别与进入（含下兵测试） | `jar/code/clancapital/attack/CapitalFriendlyChat.kt`、`colorschema/colorpackage/clancapital/CapitalFriendlyChatColors.kt`、`jar/code/clancapital/attack/CapitalAttack.kt` | 1天 | 高 | 实机：用户手动发起都城友谊战(不消耗突袭次数)用于都城下兵测试；红框标注于聊天卡片 | 🔄 | ①识别聊天卡片「绿侦察+红进攻+蓝详细信息」三按钮组合(`CapitalFriendlyChatColors`)，`enterCapitalFriendlyFromChat()` 点击红「进攻」进入，实机截图离线自检 0 假阳性；②新增 `playCapitalFriendlyChallenge()` 完整流程(进卡片→等放弃按钮→`capitalDeployArmy` 部署)，挂到 `runTestCode` 调试入口；`capitalDeployArmy` 新增 `zoomOut` 参数(友谊战首跑跳过战斗内缩放，待专项标定)；③下兵/法术颜色(CapitalDeployColors)与逻辑待友谊战实机验证标定 |
| T07 | 部落竞赛 ClanGames | `jar/code/mainbase/clangames/ClanGames.kt` + Settings + colorpackage | 2天 | 高 | 源 竞赛(135) | 🔄 | 已拆分为 **T25–T30**（见「部落竞赛（T07 拆分）」章节）；T25–T29 代码落地完成，**T30 真机标定验证通过后才置 ✅** |
| T08 | 每周精选/商人购买 | `jar/code/universal/smalltools/Trader.kt` | 1.5天 | 高 | 源 商人(18)+精选(20) L42550-44297 | ✅ | 新建 `Trader.purchaseWeeklySelection(account)`：读 `TRADER_ENABLED` 开关→进商人(文字 locate)→进「每周精选」标签→按商品开关(`Trader_<商品名>` 记忆键，默认关)逐个文字定位购买+点「确认」→领免费物品。源 2023 像素坐标/预算校验(bi)待真机复标(T30)，改用文字定位抗皮肤。挂在 `MainScript` 主循环 `enterMainScreen` 后。`buildJar` 通过、0 lint |
| T09 | 部落战进攻 ClanWar | `jar/code/mainbase/clan/ClanWar.kt` | 2天 | 高 | 源 部落战(209) L21372-22139 + 发起进攻 L23105-23145 | ✅ | 新建 `ClanWar.clanWarAttack(account)`：读 `CLAN_WAR_ENABLED` 开关→进部落(locate/兜底坐标)→判战斗日+剩余机会(locate「进攻」)→选敌开战(locate「进攻」「开始战斗」,源 1FBC6D 绿待 T30 复标)→**下兵复用主世界 `mainBaseDeployTroops()`**→等放弃按钮(EndBattle/GiveUpButton 同 mainBaseAttack)。支持「留1刀」(`CLAN_WAR_KEEP_ONE`)。挂在 `MainScript` 主循环。`buildJar` 通过、0 lint |

### 部落竞赛（T07 拆分，对齐源 函数230a/231a/232a/233a + L42649 调度）

> 用户 2026-09-28 要求：参考源脚本实现竞赛功能，**先完善任务文档、按计划开发，真机调试留到最后**。
> 源脚本坐标是竖屏 720x1280，本项目横屏 1280x720，全部按项目通用 90° 映射换算
> （区域 `(x1,y1,x2,y2) -> (y1, 719-x2, y2, 719-x1)`；偏移 `(dx,dy) -> (dy,-dx)`），
> 颜色串沿用源脚本 BGR 写法。换算由 `temp/gen_cg_colors.py` / `temp/emit_cg_kt.py` 批量生成，避免手抄出错。

| ID | 任务 | 目标文件(当前项目) | 工作量 | 优先级 | 源依据 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|
| T25 | 竞赛颜色特征库移植（面板/按钮/状态 21 个 + 任务图标 60 组 + 奖励图标 16 组） | `colorpackage/mainbase/ClanGamesColors.kt`、`ClanGamesTaskIcons.kt`、`ClanGamesRewardIcons.kt`、`MyColors.kt` | 0.5天 | 高 | 源 函数231a L33301~33512、函数232a L33559~33605、函数233a L33513~34068 | ✅ | 脚本自动生成；任务图标按源 v 参数分 `SIMPLE_TEMPLATES`(56) / `HARD_TEMPLATES`(4)；**均为 2023 版配色，待真机复标** |
| T26 | 竞赛屋定位 + 进面板 + 状态判定（已接/蓝徽章/积分满/冷却/失败/倒计时/未入部落） | `jar/code/mainbase/clangames/ClanGames.kt` | 0.5天 | 高 | 源 函数233a L33513~33656 | ✅ | 进面板后 15 次轮询状态；`isOnClanGamesPanel` 用「竞赛界面」+右上角切换按钮双佐证 |
| T27 | 接夜世界任务（任务图标匹配 → 绿色接受按钮 → 复核）+ 无任务时放弃第一张卡刷新 + 已接任务联动切号局数 | 同上 | 0.5天 | 高 | 源 函数230a L33267、函数231a、L33964~34041、L42672 | ✅ | 两轮：简单 → 简单+困难，中间上下滑屏；源用 OCR 判「建筑大师」，本项目无中文字库，改为只靠图标模板挑夜世界任务；已接任务落账号记忆 `CLAN_GAMES_ACCEPTED`，夜世界对战改用「接取竞赛后…切号」局数（源 L42672 已接夜竞赛→夜打鱼），积分做满/放弃/失败即恢复 |
| T28 | 领竞赛奖励（逐行绿勾 → 行内奖励图标 → 绿色确认） | 同上 | 0.5天 | 高 | 源 函数232a + L33634/33640 | ✅ | 行区域按 `ColorSchema.rescope` 收窄；图标全不命中时随机点橙色奖励卡 |
| T29 | 账号记忆节流（3h / 10min）+ 挂入主循环 | `StorageKeys.kt`、`MainScript.kt` | 0.5天 | 高 | 源 L42649~L42683 | ✅ | 竞赛为附加功能，内部出错自行跳过，**不参与 stepBlock 中断判定** |
| T30 | 真机标定与验证（emulator-5556） | 竞赛颜色特征 + `ClanGames.kt` | 1天 | 高 | — | ⬜ | **真机调试留到最后**：优先标定 竞赛屋 / 已接任务蓝标 / 绿色接受按钮 三个关键特征（`tools/make_feature.py`），再验证接任务与领奖闭环；不消耗资源、失败仅跳过 |

### P2 — 识别/标签增强（中优先）

| ID | 任务 | 目标文件(当前项目) | 工作量 | 优先级 | 源依据 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|
| T10 | 联赛 CWL 进攻 | `jar/code/mainbase/league/League.kt` | 1天 | 中 | 源 联赛(67) 函数183a·打联赛战 L21543-21593 + 函数331a·联赛选敌 L22899-22916 | ✅ | 新建 `League.leagueAttack(account)`：读 `LEAGUE_WAR_ENABLED` 开关→进部落→切「联赛」标签(locate)→判进攻机会(locate「进攻」)→选敌开战→**下兵复用主世界 `mainBaseDeployTroops()`**→等放弃按钮。对位选敌(源 OCR 名次对齐)简化为首个可进攻敌营，待 T30 复标。挂在 `MainScript`。`buildJar` 通过、0 lint |
| T11 | 中文玩家名 OCR | `jar/code/.../recognizer/PixelFontChinese.kt` + `tools/harvest_chinese_font.py` | 2天 | 中 | 源 函数21a | 🔄 | 已拆分为 **T31–T34**（见「中文识别（T11 拆分）」章节）；**源脚本并不存在 font_chinese**（全库 grep `chinese/中文/hanzi/汉字` 均 0 命中，函数21a 实际是数字库），故改为"ML Kit 中文为主 + 像素字库兜底" |

### 中文识别（T11 拆分）

> 2026-09-28 立项目前提（已核实）：**源脚本没有中文字库**——`doc/decrypt/` 全部 lua 中
> `font_chinese / chinese / 中文 / hanzi / 汉字` 均为 0 命中；`函数21a`（L2477）里的"文字识别库"
> 实际是 0-9 数字字模，与中文无关。所以不能"从源脚本抄字库"，必须自己采集。
>
> 技术路线（与项目现有能力对齐）：
>  - **主路径**：项目已接入 ML Kit `ChineseTextRecognizerOptions`（`TextRecognizer.kt`），
>    中文识别能力本体已有，缺的是"面向业务的封装"。
>  - **兜底**：`PixelFontChinese` 像素字库（格式与 `PixelFontDigits` 一致：
>    `字|高,宽|64进制点阵`），用于 ML Kit 不可用 / 小字号场景。字模由
>    `tools/harvest_chinese_font.py`（截图 + 标注）或 App 端 ML Kit 自动标注采集。
>  - **注意**：中文常用字 3500+，像素字库不可能全覆盖，**必须"识别不出就返回空，交给 ML Kit"**，
>    绝不能硬猜（中文笔画密集、有描边阴影，像素匹配误判率远高于数字）。

| ID | 任务 | 目标文件(当前项目) | 工作量 | 优先级 | 依据 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|
| T31 | 中文像素字库框架（解析 + 匹配 + 采集）+ 采集工具 | `jar/code/universal/recognizer/PixelFontChinese.kt`、`tools/harvest_chinese_font.py` | 1天 | 中 | 复用 `PixelFontOcr` 管线 | ✅ | 字库格式/64进制与 `PixelFontDigits` 完全一致；**新增"字格合并"逻辑**：把相邻笔画/偏旁（如"明=日+月"、"性=忄+生"）合并成字格后再匹配，避免拆字；App 端 `harvestFromScreen()` 用 ML Kit 当"老师"自动标注采集；PC 侧 `tools/validate_chinese_ocr.py` 用 `cocfz-apk-test` 里的截图做离线验证；`gradlew :app:buildJar` 通过 |
| T32 | 中文文本读取封装（部落名/村庄名/玩家名/任务描述） | `jar/code/universal/recognizer/ChineseTextReader.kt` | 0.5天 | 中 | 业务需要 | ✅ | 封装 `TextRecognizer`：区域读取、结果清洗、ML Kit 无结果回落 `PixelFontChinese`；`containsAny()` 做"建筑大师"等关键字包含匹配；编译通过 |
| T33 | 竞赛接入中文 OCR：已接任务区分「主世界 / 夜世界(建筑大师)」 | `jar/code/mainbase/clangames/ClanGames.kt` | 0.5天 | 中 | 源 L33948 OCR 判「建筑大师」 | ✅ | 补 T27 缺口：`detectAcceptedTaskType()` 点开已接任务 → OCR(661,167,799,242) → 含"建筑大师"记 night 否则 main；`ClanGamesState` 增 `isNightTask`，记忆值改 `night/main/0`，`BuilderBaseAttack` 仅 `== "night"` 时切用"接取竞赛后"局数；编译通过 |
| T34 | 真机采集中文字模 + 中文识别验证 | 采集工具 + 字库常量 | 0.5天 | 中 | — | ⬜ | 需 emulator-5556（当前不在线）；先采集部落名/村庄名高频字并验证 `detectAcceptedTaskType` 命中率 |
| T35 | 文字按钮定位 `ChineseTextReader.locate()`（按文案定位 + 坐标反算） | `jar/code/universal/recognizer/ChineseTextReader.kt` | 0.5天 | 中 | T11 收尾/补充 | ✅ | ML Kit 文字识别结果带包围盒，新增 `locate(keyword,...)`：识别→命中含关键字行→包围盒 `/scale`+裁剪偏移反算回屏幕坐标返回 `Rect`；复用 `minConfidence`/`requireCJK` 双闸；调用方自行降级（回退颜色匹配/跳过）。`buildJar` 通过、0 lint |
| T36 | 训练部队/法术（主世界） | `jar/code/mainbase/troop/TrainTroops.kt` | 1.5天 | 中 | 源 造兵(241a/239a/240a) Q段 L34253-41171 | ✅ | 用户 2026-09-28 追加（原编号表无此 ID）。新建 `TrainTroops.trainTroops(account)`：读 `TRAIN_ENABLED` 开关→进军队/造兵栏(文字 locate)→按账号记忆键 `Train_<兵种/法术>` 数量逐个文字定位+连点「训练」。源「左中右栏切换+单位占用扣减+活动兵/超级兵强化」待 T30 复标。`buildJar` 通过、0 lint、挂 MainScript |
| T37 | 升级建筑（主世界） | `jar/code/mainbase/upgrade/Upgrade.kt` | 2天 | 中 | 源 升级(202a/205a/206a) P段 L24387-34252 | ✅ | 用户 2026-09-28 追加（原编号表无此 ID）。新建 `Upgrade.upgradeBuildings(account)`：读 `UPGRADE_ENABLED` 开关→开商店/建筑列表(文字 locate)→按账号记忆键 `Upgrade_<建筑>` 逐个定位+点「升级」+「确定」。源「建筑列表 OCR 扫描+空闲工人判断+宝石秒」待 T30 复标（OCR 字库 jianzhu0.txt）。`buildJar` 通过、0 lint、挂 MainScript |

### P3 — 可选/低优

| ID | 任务 | 目标文件(当前项目) | 工作量 | 优先级 | 源依据 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|
| T12 | 大字号 OCR | `jar/code/universal/recognizer/PixelFontLarge.kt` + `PixelFontOcr.kt`/`PixelFontDigits.kt` | 0.5天 | 低 | 源 函数315a(19×18) | ✅ | 新建 `PixelFontLarge`（复用既有 `PixelFontOcr` 通用点阵引擎 + `PIXEL_FONT_DIGITS` 字库）：`recognize()` 用大字调优参数（maxGlyphSize=60/sizeTolerance=4/minGlyphHeight=10、亮色低饱和 ink），`recognizeRegion()` 截屏区域识别，`digitsOnly()` 抠数字。源 函数315a 二值化 0-251、精确字库/亮饱和区间待 T30 真机复标。`buildJar` 通过、0 lint |
| T13 | 地图缩放检测移植 | `jar/code/universal/map/MapLocator.kt` + `CameraState` | 1天 | 低 | 源 coc-assist MapLocator | ✅ | 新建 `MapLocator`：`detectZoomLevel()` 截屏四边边缘带(EDGE_BAND_PX=40)统计森林暗绿占比→0-3 级（阈值 ZOOM_RATIO_THRESHOLDS 待 T30 复标）；`detectMainVillageZoomLevel`/`detectBuilderBaseZoomLevel` 写 `CameraState.mainVillageZoomLevel/builderBaseZoomLevel`；`setMainVillageZoomLevel(target)` 单调 pinch 逼近（pinchIn=拉远/pinchOut=拉近，坐标同 ZoomSmallMainBase，待 T30 复标）。`CameraState` 增两等级字段并在 reset() 复位。`buildJar` 通过、0 lint |

### 夜世界（Builder Base）增强（对齐源 函数123a/323a）

> 当前项目 `BuilderBaseAttack.kt` 已实现打鱼/练兵/升级主流程（判为已覆盖），但对照源 `awcocx_main.lua` 的 `函数123a(下兵)` / `函数323a(双指滑屏放兵)` / 战斗监控(16606~16669)，存在以下可抄增强点。

| ID | 任务 | 目标文件(当前项目) | 工作量 | 优先级 | 源依据 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|
| T14 | 夜世界夜飞机(空中机器)英雄部署 | `jar/code/builderbase/attack/BuilderBaseAttack.kt` | 1天 | 中 | 源 夜飞机槽(15979) | 🔄 | 代码已实现，用户手动验证中 |
| T15 | 夜世界多兵种识别与批量下兵 | `jar/code/builderbase/attack/BuilderBaseAttack.kt` + `builderbase` colors | 2天 | 中 | 源 函数123a(15997~16071) | 🔄 | 12 兵种颜色 + `deployAllTroops`；**实机发现单点落点会压到基地建筑区(不可下兵)**，已改为四象限多点候选 `buildDeployPoints()` + `buildFallbackDeployPoints()` 边缘环带兜底；下兵状态机：白框 `isCardSelected()` 判选中（避免重复点卡=放技能）、**顶部紫色技能条位置 `cardSkillBarTop()`/`isCardDeployable()` 判兵是否已下放**（未下 顶部 y≈588 / 已下场 568 / 技能已放或阵亡则无条，跨 10 张截图验证一致）、单轮上限 `MAX_TROOPS_PER_ROUND=30`/连点 `DEPLOY_TAPS_PER_ROUND=8`、非网格卡（x<180：英雄/夜飞机）逐兵种记录 `offGridTapped`；**已下完的卡按卡位号 `exhaustedSlots` 去重**（同兵种占 6 个卡位，按兵种序号会把其余同款卡一起跳过）并支持逐卡位 rescope 续找；判据整体失效时（一个兵都没下却被判全部已下放）回退按特征下兵，待验证 |
| T16 | 夜世界英雄技能自动释放 | `jar/code/builderbase/attack/BuilderBaseAttack.kt` + `builderbase` colors | 1天 | 中 | 源 战斗监控(16606~16669) | 🔄 | 夜飞机按卡槽 rescope 检测粉光后点槽；战争机器改用**充能条第 1 格亮起**判据（新增 `HeroChargeReady`，位置 (95,560)-(112,565)、亮色 #C022FB，由用户实机截图经 `tools/make_feature.py` 标定），命中即点英雄卡槽释放；**颜色串是 BGR 且为上中下三层垂直叠色**，`HeroChargeReady` 修正为 (93,559)-(116,570) 三层 FB25C1/FE3AC7/FF98DF、`TroopSkills` 同理修正（此前按 RGB 解释被改坏）；技能轮询 `SKILL_LOOP_MAX_MS` 120s→**30s**（原值每次下兵后死守两分钟，把"进第二区域/继续下兵"全卡住；英雄充能约 15s/格，30s 够放 1~2 次），待验证 |
| T17 | 场景识别健壮性（载入页/战斗中/编辑模式/未识别重试） | `jar/code/universal/SceneState.kt` + `colorpackage/UIColors.kt` | 1天 | 高 | 15 张实机未识别 debug 截图 | 🔄 | 新增 `GameScene.LOADING` 与特征 `GameLoadingNotice`(合规黑屏)/`BuilderBaseEditMode`(夜世界编辑模式)；战斗中(`EndBattle`等)归 `BATTLE`；未识别先等 1.5s×2 重试、载入等待 15–90s；载入页不再按返回（修复游戏退出确认弹窗），待验证 |
| T18 | 日志分级与文件落盘 | `core/util/basic/ShowMessage.kt` + `core/util/fileactions/LogHelper.kt` | 1天 | 中 | — | 🔄 | `ShowMessage` 恢复旧 `invoke` 入口(保持旧 jar 二进制兼容)并新增 `run()/warn()/error()/log()`；`LogHelper` 按 DEBUG(全量 VERBOSE)/RELEASE(仅 INFO+) 分级落文件、缓冲 100→200→**2000**（夜世界下兵诊断每轮 3 行、一场约 200 行，200 行会滚掉上一场）；夜世界里程碑改 `ShowMessage.run()`，待验证 |

### 通用稳定性修复（实机问题驱动，不入阶段表）

| ID | 任务 | 目标文件(当前项目) | 工作量 | 优先级 | 源依据 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|
| T20 | 「橙色转圈卡死」检测误判修复 | `jar/code/universal/smalltools/CheckReconnections.kt` | 0.5天 | 高 | 实机：夜世界「开始进攻」确认弹窗被判网络卡死并重启游戏 | 🔄 | 采样区收窄到中央 (480,280)-(800,440) 避免橙色装饰误入；卡死计时改为**跨循环累计**（原实现单次调用阻塞 12s，导致弹窗白等十几秒后仍被重启）；命中 `AttackNow`/`CancelAttackSearch` 等静止等待界面直接放行并复位计时，待验证 |
| T21 | swipe 手势误触统一（长按拖建筑 / 被判点击弹「信息」面板） | `core/util/touchactions/TouchActions.kt` + 夜世界/主世界 zoom、收集资源、城墙批量建造调用点 | 0.5天 | 高 | 实机：swipe 起点常压在村庄建筑/城墙上 | 🔄 | `swipe` 是「按下→停顿(delayTime×0.7×倍率)→移动」：停顿过长被判长按拖建筑、过短被判点击选中建筑。默认 300~400ms→150~200ms；各调用点统一 `delayTime=180`（收集资源/夜世界 zoom/主世界 zoom 由 100、120、600、800 统一为 180，城墙批量 600→120）；收集资源盲点后补 `clickRightBottom()` 清掉选中状态，待验证 |

### 文档与梳理（源脚本盘点）

| ID | 任务 | 目标文件 | 工作量 | 优先级 | 依据 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|
| T24 | 源脚本全量功能对照文档（逐行归类，可检索） | `docs/源脚本全量功能对照.md` + `tools/scan_lua_skeleton.py` | 1天 | 高 | 用户 2026-09-27 要求 | ✅ | 覆盖 `doc/decrypt/` 全部 6 个 lua（主脚本 56062 行）：21 个分区 + 1067 条「行范围↔功能」条目；扫描器 `tools/scan_lua_skeleton.py` 自动切块，`tools/_lua_scan_out/verify_doc.py` 自检通过（仅 43 个源码空行无功能对应） |

---

## 三、决策记录（已明确不抄项，留存追溯）

| 项 | 决策 | 依据 | 日期 |
|---|---|---|---|
| 村庄改名 | ❌ 不抄 | 用户 2026-09-23 明确决定 | 2026-09-23 |
| 部落靓标签筛选 | ❌ 不抄 | 用户 2026-09-23 明确决定 | 2026-09-23 |

---

## 四、进度汇总

- 总任务数：37（T01–T37，T36/T37 为 2026-09-28 用户追加的训练/升级，原编号表无此 ID）
- ✅ 已完成：21（T03,T04,T05,T08,T09,T10,T12,T13,T22,T24,T25,T26,T27,T28,T29,T31,T32,T33,T35,T36,T37）
- 🔄 进行中：12（T02,T07,T11,T14,T15,T16,T17,T18,T19,T20,T21,T23）
- 🔄 精炼完成待真机验证：1（T06，部署顺序/9地图补点已修正，下兵颜色待 emulator-5556 复标）
- ⬜ 待办：3（T01,T30,T34）
- ❌ 不抄/取消：2（村庄改名、部落靓标签，见决策记录）

---

## 五、更新记录

| 日期 | 任务ID | 变更 | 说明 |
|---|---|---|---|
| 2026-09-23 | T01–T13 | 新建 | 由 `docs/源项目功能对比与移植计划.md` §5 导入全部开发任务，确立本文件为任务唯一基准 |
| 2026-09-23 | — | 决策 | 村庄改名、部落靓标签标记为不抄（不立对应任务，记入决策记录） |
| 2026-09-23 | T14–T16 | 新建 | 新增夜世界增强任务（夜飞机部署 / 多兵种识别批量下兵 / 英雄技能释放），对齐源 函数123a/323a |
| 2026-09-23 | T06 | 状态 ⟳ | 用户决定延后都城，先做夜世界；T06 → ⏸ |
| 2026-09-26 | T22 | 新建 | 都城突袭补齐：造兵方案按源重写(函数28a/29a两套打法)、下兵补第一轮三区域逐级回退+第二轮滑动视野重扫+9地图目标建筑定位、法术投放点按源区分、总都城币累计统计落盘、关键节点加日志与截图存档 |
| 2026-09-26 | T22 | 状态 ⟳ | 代码补齐完成（read_lints 0 错误），待真机验证（每号每周仅5次突袭机会，需在真机标定坐标） |
| 2026-09-27 | T22 | 状态 ⟳ | 真机验证通过：都城入口=热气球浮岛（源 4D6EFF 按 BGR 即热气球橙红，须先缩放归位镜头再找）、进入都城、领币/发起、造兵、按星选子城（正被进攻的子城进攻按钮置灰→正确跳过换下一个）均按预期；验证期不点「进攻」，未消耗突袭次数 |
| 2026-09-27 | T22 | 真打调试 | 新增 CAPITAL_RAID_REAL_ATTACK 开关（默认关），确认界面「进攻」按钮可用时真打；**修复 CapitalDistrictAttackGray 误判**（红色可用按钮上也会命中灰特征→所有子城被误判不可打，改为 CapitalDistrictAttackRed 正向判据，红/灰区分度完美）；runTestCode 临时改为只跑都城（调试完还原）。**第一场突袭全链路跑通**：选子城(红判据)→底部进攻→确认界面→真打→下兵+法术全下完→战斗结束→回营→统计，摧毁率 25%，消耗 1/5 次；日志+截图存档 temp/capital_debug/run_20260927_0208/。遗留问题：①都城入口定位对镜头初始位置敏感（遗留都城地图视图时反复失败，重启游戏恢复）；②prepareCapitalArmy 时序错位（在地图层点「编辑都城军队」无效，应在进攻确认层造兵）；③边缘 3 点下兵摧毁率仅 25%，待对齐源二轮扫描/9 地图补点优化 |
| 2026-09-27 | T22 | 真打调试2 | 第二轮真打（第2/3场）暴露并修复：①**造兵每轮重复**→改为进突袭地图后本轮只造一次（用户明确：造兵是进攻前准备）；②**战斗结束不回城**→漏抄源「回营主」，新增 CapitalResultMapButton（用户结算截图标定 RGB(141,209,64)@(579,597)），放弃按钮消失→等3.5s→点它→再回营；③**测试完不停脚本**→runTestCode 改为跑完一轮 setRunControl(STOPPED)，02:34 验证生效；④下兵点收集改回源语义（累计满10个才停，此前满2个就返回导致兵/法术全下一点）。**缩放手势重大踩坑（已回退）**：照抄源 L94829~94847 手搓「双指收拢+swipes+单指拖拽」，实测变成放大且把镜头拖到角落。与已验证的 zoomToCapitalShore 对比：正确写法 = pinchIn(两指→同一收拢点) 或「分步成对 touchMove + 每步 delay 60~80ms + releaseAllPointers」；错误写法 = 单步瞬移到 20px 间距（项目已知坑：20px+瞬移被判单击/选中建筑）+ 两指各自瞬移被拆成两次单指拖动 + 额外 swipes/拖拽。战斗内缩放已暂停使用，待专项标定 |
| 2026-09-27 | T22 | 真打调试3：删兵重造 | 修复「没有触发删除兵和法术重造」：①`prepareCapitalArmy` 入口从过期的"进攻确认层编辑按钮"(1053,497)改为**都城内左侧军队入口图标**（只在都城村庄视图可见），在 `openCapitalRaidMap` 之前调用；②`CapitalArmyDeleteButton` 特征重标：原 bbox 误匹配顶部军队卡片上的"移除"小红叉，现改为红色垃圾桶按钮左侧红色背景（RGB(222,18,23)，x≈472~488,y≈302~336）；③`CapitalTrainConfirm` 特征与点击点修正：原绿色特征区实际对应"突袭信息"按钮，现用保存按钮红色背景区（x≈625~700,y≈302~336）并直接点特征命中点，避免用过期标定中心；④训练点击加偏移：特征点落在兵种卡右上角"i"信息按钮上，原点击会弹出详情导致造兵无效，现相对特征点左移30、下移40点卡面主体。模拟器验证：进入都城→左图标开面板→删除（250→0）→造超级矿工10次（0→250）+骷髅法术2次+冰冻法术2次→点保存，面板关闭，军队配置生效。**遗留**：野蛮人攻城槌/超级野蛮人/雷电法术特征未匹配（当前屏可见但颜色schema过期），不过超级矿工已填满250军队、骷髅+冰冻填满7法术，不影响出征；下一步需继续修复突袭地图锚点识别与战斗内缩放。 |
| 2026-09-27 | T22 | 都城主界面识别 | 应用户要求增加都城主界面识别、区别于主世界/夜世界：①新增 `CapitalVillageLabel` 颜色特征（顶部"部落都城"标签：深色文字+浅蓝底+羊皮纸图标，x≈525~570,y≈10~25），在 zoom2_base（都城）自检命中，在 zoom3_base（主世界）与 capital_enter（突袭地图）不自检命中；②`isInClanCapital()` 改为 `isInCapitalVillage()`，同时检测左侧 `CapitalArmyEntryIcon` 与顶部 `CapitalVillageLabel`，任一命中即认为在都城主界面；③`SceneState.detectCurrentScene` 中原来的 `ClanCapitalEntry`（主世界海岸热气球/回营按钮，易造成都城误判）替换为新的 `isInCapitalVillage(screen)`。避免把主世界/夜世界误判为都城。 |
| 2026-09-27 | T23 | 新建+实现 | 聊天界面都城友谊战：标定红进攻(221EF6)/绿侦察(38CD86)/蓝详细信息(F48439)三按钮组合，新增 `CapitalFriendlyChatColors` 与 `CapitalFriendlyChat.enterCapitalFriendlyFromChat()`；实机聊天截图离线自检 689 命中全在红按钮区、0 假阳性，待真机验证点击进入 |
| 2026-09-27 | T23 | 实现 | 友谊战下兵测试链路：`playCapitalFriendlyChallenge()`(进卡片→等放弃按钮→`capitalDeployArmy`) 挂到 `runTestCode`；`capitalDeployArmy` 增 `zoomOut` 参数(友谊战首跑跳过战斗内缩放，待标定)；用于实机标定/验证 `CapitalDeployColors` 与下兵逻辑 |
| 2026-09-27 | T24 | 新建+完成 | 用户要求重新梳理源脚本全部代码并分类存文档：产出 `docs/源脚本全量功能对照.md`（21 分区 / 1067 条行号↔功能条目，覆盖主脚本 56062 行及 bootstrap/TURING/dkjson/hdtyynnn/layout）；配套 `tools/scan_lua_skeleton.py`（切块扫描）、`tools/_lua_scan_out/dump.py`（按 LF 行号打印源码）、`verify_doc.py`（覆盖自检） |
| 2026-09-23 | T14 | 状态 ⟳ | 开始夜世界任务；T14 → 🔄 进行中 |
| 2026-09-23 | T14 | 实现 | 新增 `BuilderBaseBattleCopter` 颜色(90°映射 (15,578,500,672))；`normalBattle()` 在战争机器后部署夜飞机落向 deployPos；无 lint 错误，待模拟器验证 |
| 2026-09-23 | T15 | 实现 | 新增 12 个兵种卡槽颜色(区域 (15,612,1279,677)，偏移 90°旋转)；`normalBattle()` 重构为 `deployAllTroops()`，遍历夜巫/野蛮/其余10兵种逐一点下放空；无 lint 错误，待模拟器验证 |
| 2026-09-23 | T16 | 实现 | 新增 `BattleCopterSkills`(+备选) 颜色(偏移 90°旋转)；`normalBattle()` 记录夜飞机卡槽位置，`realAttack()` 循环按卡槽 rescope 检测粉光并点槽放技能；无 lint 错误，待模拟器验证 |
| 2026-09-23 | T15 | 修复 | 实机验证发现 `deployAllTroops` 单点落点会压到基地建筑区(不可下兵，日志落点 (374,240)/(370,407) 在基地内)；改为 `buildDeployPoints()` 四象限多点候选 + `deployTroopUntilGone()` 选卡后试至兵卡消失 |
| 2026-09-23 | T16 | 修复 | 战争机器技能原点击 `(machineSkills.x, machineSkills.y + 100)` 落到英雄卡槽之外；改为记录 `machineSlot` 并在粉光就绪时点卡槽释放（与夜飞机一致） |
| 2026-09-23 | T18 | 新建 | 日志分级与落盘：`ShowMessage` 恢复旧 `invoke` 入口(保持旧 jar/预编译代码二进制兼容，修复改签名引起的 NoSuchMethodError 崩溃)并新增 `run/warn/error/log`；`LogHelper` 按 DEBUG(全量)/RELEASE(仅 INFO+) 分级落文件、缓冲 100→200；夜世界流程日志改 `ShowMessage.run()` |
| 2026-09-23 | T17 | 新建+实现 | 依据 15 张实机未识别 debug 截图（用户标注）：新增 `GameScene.LOADING` 与特征 `GameLoadingNotice`(合规黑屏)/`BuilderBaseEditMode`(夜世界编辑模式)；战斗中(`EndBattle`/`GiveUpButton`/`ExitBattleButton`/`CancelAttackSearch`)归 `BATTLE`；未识别先等 1.5s×2 重试、载入等待 15–90s；移除会与夜世界夜空互相误命中的"乌云"特征；修复载入页按返回触发游戏退出确认弹窗的问题 |
| 2026-09-23 | T17 | 参数 | 载入等待上限 90s（常规 15s 即完成，版本更新时长载入由上限兜底）；非载入类未识别重试 2×1.5s 后才记入未识别 |
| 2026-09-23 | T02 | 实现 | `NccMatcher` 接线：新增 `TemplateMatcher`（assets 模板加载+缓存、区域查询，阈值 0.75）；`SceneState` 新增模板兜底钩子 `TEMPLATE_RULES`（默认空，仅在未识别时兜底）。实测「整图搜索误命中 9/16 → 限区域 3/16 → 阈值 0.75 后 0/16」 |
| 2026-09-23 | T19 | 新建+实现 | 新增特征标定工具链：`tools/make_feature.py`（截图+矩形 → 自动生成 `ColorSchema`，含正/负样本自检）、`tools/make_template.py`（裁剪模板到 `assets/templates` + 同口径 NCC 自检，支持 `--verify-region`/`--threshold`）；示例模板 `btn_bb_edit.png` |
| 2026-09-23 | T15 | 修复 | 进入夜世界后"收集资源"误拖动建筑：`collectBuilderBaseResources()` 两次下滑起点 (587,420) 压在城墙上，且第二次未传 `delayTime`（`TouchActions.swipe` 按下后停顿 300~400ms×倍率）被游戏判定为"长按拖动建筑"→ 误拖城墙；已改为两次都传 `delayTime = 100` |
| 2026-09-23 | T16 | 修复 | 战争机器技能判据改为"英雄卡槽顶部**充能条第 1 格亮起**"（用户实机确认的机制）：新增 `HeroChargeReady` 特征（位置 (95,560)-(112,565)、亮色 RGB C022FB），由 `tools/make_feature.py` 从实机截图 `I:\coc\游戏截图\夜世界-英雄充能\进度0~3` 自动生成并自检（进度 1/2/3 命中、进度 0 不命中）；`realAttack` 命中即点英雄卡槽释放，不再依赖粉光与 `machineSlot`；`make_feature.py` 负样本自检支持 jpg/jpeg |
| 2026-09-24 | T15 | 实现 | 下兵状态机：`isCardSelected()` 用"选中卡左缘贯穿整卡的纯白竖线"判选中（差分法从用户标注图 `夜世界-已死亡-已放技能-未放技能-选中-未选中.jpg` 标定，此前凭缩放截图估算坐标取偏才识别不出）；`cardSaturation()` 用高饱和占比判卡已下完（已死亡 0.016 vs 有兵 0.32~0.43，阈值 0.15）；单轮上限 `MAX_TROOPS_PER_ROUND=30`、每轮连点 `DEPLOY_TAPS_PER_ROUND=8` 个落点（一张女巫卡 20 兵，一轮一点太慢）；`exhaustedCards` 跳过已下完的卡（放空的卡位色特征仍匹配得到）；英雄卡 x<180 不在 8 卡位网格内，白框判定无效→改为只点一次（`heroCardTapped`）并立刻标记耗尽（否则英雄排 TROOP_CARDS[0] 且永不变灰，会一直被挑中，兵种轮不到） |
| 2026-09-24 | T15 | 实现 | 落点兜底 `buildFallbackDeployPoints()`：对方基地铺满画面中央时部署线整段压在基地里，改为贴四边的密集环带（step 60），剔除左下「结束战斗」按钮 (x<160,y 470~525) 与底部卡槽区，按离中心距离降序尝试 |
| 2026-09-24 | T16 | 修复 | 颜色串是 **BGR 且为上中下三层垂直叠色**（用户指正）：`HeroChargeReady` 修正为 (93,559)-(116,570) 三层 FB25C1/FE3AC7/FF98DF（实机 RGB C125FB/C73AFE/DF98FF），`TroopSkills` 修正为 (189,566)-(1241,600) FF5AEE + 垂直偏移 FD34C5/FE3CC7/FF46C9/FF7AD7（原 FF44C9 本就是 BGR，此前按 RGB 解释被改坏）；未充能整条灰 252525 不命中 |
| 2026-09-24 | T20 | 新建+实现 | 断线重连卡死检测：采样区 (400,250)-(880,500) 收窄到 (480,280)-(800,440)；卡死计时改为跨循环累计（`spinnerStuckSince` + 上一帧缓存，不再单次阻塞 12s）；新增 `AttackNow`/`CancelAttackSearch` 静止等待界面白名单直接放行。修复实机"夜世界「开始进攻」确认弹窗被判网络卡死并重启游戏" |
| 2026-09-24 | T21 | 新建+实现 | swipe 手势统一：`TouchActions.swipe` 默认停顿 300~400ms→150~200ms（项目内十余处未传 delayTime 的调用一并受益）；`ZoomSmallBuilderBase`(120/800→180)、`ZoomSmallMainBase`(600→180)、`BuilderBaseCollectResources`(100→180)、`UpgradeBuildings` 城墙批量(600→120)；收集资源盲点未找到资源车时补 `clickRightBottom()` 清掉建筑选中弹窗。兼顾"停顿过长→长按拖建筑"与"过短→判成点击选中建筑"两侧 |
| 2026-09-24 | T18 | 参数 | `LogHelper.MAX_LOG_LINES` 200→2000（一场夜世界下兵诊断约 200 行，200 行缓冲会滚掉上一场，不利于排查） |
| 2026-09-24 | T15 | 判据重做 | 兵卡"是否还有兵可下"判据改为**顶部紫色技能条的位置**（用户实机确认的机制）：`cardSkillBarTop()` 自上而下找第一行紫色像素（B>200 且 R>150 且 G<150），返回条顶部 y —— 无条=已阵亡/技能已放、y<578=已下场、y≥578=还没下。实测未下 588（选中 582）、已下场 568，跨 10 张截图一致（「请选择其他兵种」全 588、「已经派出所有兵力」全 568）。旧的高饱和占比判据 `cardSaturation()` 删除 |
| 2026-09-24 | T15 | 修复 | 已下完的卡改为按**卡位号**去重 `exhaustedSlots`：`TROOP_CARDS` 下标是兵种序号，而同种兵占 6 个卡位，按兵种序号记录会让第一张女巫下完后其余 5 张一起被跳过；扫卡改为逐卡位 rescope 续找同兵种的下一张卡 |
| 2026-09-24 | T15 | 修复 | 非网格卡（x<180：英雄 idx0 / 夜飞机 idx1）原用同一个 `heroCardTapped` 布尔，导致第一区域点了英雄后第二区域扫到夜飞机也被判"已点过"、只点落点不点卡。改为按兵种序号记录 `offGridTapped` |
| 2026-09-24 | T15 | 修复 | 紫色条判据不能套在英雄卡上：英雄卡上方的粉色**充能条**（y559~567、D36AFF）同样满足紫色判据且位置偏上，会把战斗刚开始的英雄误判为"已下场"，整局不下英雄。x<180 的卡一律不套该判据 |
| 2026-09-24 | T15 | 兜底 | 新增判据整体失效兜底：一个兵都还没下（deployed==0）却被判"全部已下放"时，退回按特征匹配下兵，避免整局一个兵都不下 |
| 2026-09-24 | T15 | 回退 | **教训**：期间曾改用"人物背景色是否偏蓝"+"兵卡血条"判据（基于真机标注图标定，离线区分度 68），实机上完全对不上（战斗刚开始兵都没下却被全判已下场/已阵亡，兵和英雄都不下）→ 已 `git stash` 暂存（stash@{0}），待拿实机截图重新标定。**离线截图标定的判据必须在实机验证过再用**，且任何新判据都要配"失效兜底" |
| 2026-09-25 | T15 | 修复 | 夜世界进攻"战争机器(王)不下场"：源方案 `BuilderBaseAttackSource.dropHero` 只点英雄卡选中、漏掉"再点一次落点"，导致英雄始终留在手里不下场（日志 `检测到英雄[王]卡槽...点下放` 后无实际下兵）。对照源 `awcocx_main.lua` 函数123a(15961-15978)：选卡后需 `taps(左上/右上/左下/右下中间X/Y)` 中点下兵。已补 `heroDeployPoint()` 取当前方向象限中点（横屏真实坐标）并在选卡后点击，王与夜飞机均生效。改动只在 jar 包，需 `gradlew :app:deployAndReload` 热更后才生效 |
| 2026-09-28 | T07 | 拆分 | 用户要求"参考源脚本实现竞赛功能，先完善任务文档、按计划开发，真机调试留到最后"：T07 拆为 **T25–T30** 并新增「部落竞赛（T07 拆分）」章节；T07 → 🔄 |
| 2026-09-28 | T25 | 新建+完成 | 竞赛颜色特征库：新增 `ClanGamesColors.kt`（21 个命名特征：竞赛屋/已接任务/蓝徽章/可领奖励/接受按钮/任务失败/冷却/倒计时/第一张卡/放弃按钮/放弃确认/奖励行/绿色操作按钮/领取确认/任务切换/奖励卡/积分满/任务完成/未入部落）、`ClanGamesTaskIcons.kt`（60 组任务图标，分 SIMPLE 56 / HARD 4）、`ClanGamesRewardIcons.kt`（16 组奖励图标）；全部由 `temp/gen_cg_colors.py`+`temp/emit_cg_kt.py` 按 90° 映射从源 lua 自动生成并接入 `MyColors`。源 cmpColorEx 多点比色已换算成"主色 + 相对偏移" |
| 2026-09-28 | T26,T27,T28 | 新建+完成 | `jar/code/mainbase/clangames/ClanGames.kt`：竞赛屋定位(缩小镜头→找不到斜滑一次重试)→进面板(6s 等待)→15 次状态轮询(已接/蓝徽章≥3/积分满)→领奖(逐行绿勾+行内图标+确认)→未接任务时两轮(简单→简单+困难)图标匹配+绿色接受+复核→无任务放弃第一张卡刷新；退出/冷却/倒计时/未入部落/任务失败均有分支。`gradlew :app:buildJar` 编译通过 |
| 2026-09-28 | T29 | 新建+完成 | `StorageKeys` 新增 `CLAN_GAMES_CHECK`(3h) / `CLAN_GAMES_NOT_FOUND`(10min) 按账号记忆节流（源 L42649~42683）；`MainScript` 在 `playMainBase()` 之后调用 `playClanGames()`，竞赛内部出错自行跳过，不参与 stepBlock 中断判定 |
| 2026-09-28 | T30 | 新建 | 真机标定与验证（T07 收尾项，按用户要求留到最后）：优先标定 竞赛屋 / 已接任务蓝标 / 绿色接受按钮 |
| 2026-09-28 | T27 | 补充 | 接任务与夜世界切号联动：`StorageKeys` 增 `CLAN_GAMES_ACCEPTED`（按账号持久化"已接竞赛任务"）；`ClanGamesState` 的 已接/放弃/做满 均落记忆（源 L42634 已做满→关接竞赛、L42672 已接夜竞赛→夜打鱼）；`BuilderBaseAttack` 读到已接竞赛时对战局数改用 `SWITCH_ACCOUNT_AFTER_BATTLES_WITH_TASKS`；`BuilderBaseConfig` 恢复该输入行（此前被注释）。`gradlew :app:buildJar` 编译通过。**T30 阻塞：emulator-5556 当前不在线，真机标定待模拟器可用后进行** |
| 2026-09-28 | T11 | 拆分 | 用户要求实现中文字库 T11。经核实**源脚本并不存在中文字库**（`doc/decrypt/` 全库 grep `chinese/中文/hanzi/汉字` 均 0 命中；`函数21a` 实为数字字模），故不能从源照搬，改为"ML Kit 中文为主 + 像素字库兜底"路线，拆 T31–T34 |
| 2026-09-28 | T31,T32,T33 | 新建+完成 | T31 `PixelFontChinese.kt`（中文像素字库：解析/匹配/采集，格式与 `PixelFontDigits` 一致）+ `tools/harvest_chinese_font.py`（截图+标注采模，含回读自检）；App 侧 `harvestFromScreen()` 用 ML Kit 当"老师"自动标注、`saveHarvested()` 落盘 `/sdcard/zkqFiles/chinese_font.txt`。T32 `ChineseTextReader.kt`：封装 `TextRecognizer` 中文识别，ML Kit 无结果回落 `PixelFontChinese`，`containsAny()` 做关键字包含匹配。T33 `ClanGames.kt` 接入：`detectAcceptedTaskType()` 点开已接任务→OCR(661,167,799,242)→含"建筑大师"记 night 否则 main，补上 T27"无中文字库"缺口；记忆值改 `night/main/0`，`BuilderBaseAttack` 仅 `== "night"` 切用"接取竞赛后"局数。`gradlew :app:buildJar` 编译通过、相关文件 0 lint |
| 2026-09-28 | T34 | 新建 | 真机采集中文字模 + 中文识别验证（T11 收尾项）：需 emulator-5556（当前不在线），先采集部落名/村庄名高频字并验证 `detectAcceptedTaskType` 命中率 |
| 2026-09-28 | T31 | 修复 | 验证时发现中文字符（如"明"、"性"）二值化后多为多连通域，`PixelFontChinese` 原逻辑直接按连通域匹配会拆字；新增 `mergeComponentsIntoCells()`，把相邻笔画/偏旁合并成近似方形字格后再匹配；同步更新 PC 侧 `tools/validate_chinese_ocr.py`。`gradlew :app:buildJar` 编译通过 |
| 2026-09-28 | T34 | 验证 | 用 `cocfz-apk-test/游戏截图/` 现有截图离线跑 `tools/validate_chinese_ocr.py`：预处理/连通域/字格合并能较好地把 UI 中文切成单个字符区域（可视化见 `temp/cnocr_validation/`）；**用 APK 导出的 TTF 在 PC 截图上做模板匹配效果差**（字体/缩放/描边与模拟器截图差异大，匹配得分 0.45~0.50 且大量错认），说明像素字库法必须以**真机采集字模**为准。PC 端只能验证分割，不能验证最终识别准确率；最终识别率仍要靠 T34 真机 ML Kit + 采集字模后验证 |
