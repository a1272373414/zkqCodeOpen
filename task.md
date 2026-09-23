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
| T03 | 特征带方向搜索 | `FindMultiColors.kt` / Rust | 1天 | 低 | 源 函数24a/26a | ⬜ | 评估是否给找色增加方向参数提升滑动场景鲁棒性 |
| T19 | 特征标定工具链（自动生成 + 模板裁剪） | `tools/make_feature.py` / `tools/make_template.py` | 1天 | 中 | 实机痛点：文字按钮需逐点量色 | 🔄 | `make_feature.py`：给截图+矩形自动生成 `ColorSchema`(主色众数 + 跨样本稳定偏移点) + 正/负样本自检；`make_template.py`：裁剪模板到 `assets/templates` 并做同口径 NCC 自检（可限区域/阈值）。已离线验证（编辑模式：15 张负样本 0 误命中），待实机使用验证 |

### P1 — 明确缺失，直接抄（高优先）

| ID | 任务 | 目标文件(当前项目) | 工作量 | 优先级 | 源依据 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|
| T04 | 换阵(战争基地阵型切换) | `jar/code/mainbase/warbase/ChangeWarBase.kt` | 1天 | 高 | 源 换阵(48) | ⬜ | 复用已有 CHANGE_BASE 设置，补逻辑 |
| T05 | 协议弹窗双确认强化 | `jar/code/universal/smalltools/AgreementPopups.kt` | 0.5天 | 高 | 源 函数10a/301a | ⬜ | 腾讯协议双确认 + 谷歌"全部接受" |
| T06 | 都城部署精炼 + 实机验证 | `jar/code/clancapital/attack/CapitalAttack.kt` | 2天 | 高 | 源 突袭打野 | ⏸ | 用户 2026-09-23 决定延后，先做夜世界 |
| T07 | 部落竞赛 ClanGames | `jar/code/mainbase/clangames/ClanGames.kt` + Settings + colorpackage | 2天 | 高 | 源 竞赛(135) | ⬜ | 需从 awcocx 反推挑战类型与兵种映射 |
| T08 | 每周精选/商人购买 | `jar/code/mainbase/trader/Trader.kt` + colors | 1.5天 | 高 | 源 商人(18)+精选(20) | ⬜ | 识别商人页商品→按资源/宝石买周精选 |
| T09 | 部落战进攻 ClanWar | `jar/code/mainbase/clanwar/ClanWar.kt` | 2天 | 高 | 源 部落战(209) | ⬜ | 复用 MainBaseDeployTroops 部署器 |

### P2 — 识别/标签增强（中优先）

| ID | 任务 | 目标文件(当前项目) | 工作量 | 优先级 | 源依据 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|
| T10 | 联赛 CWL 进攻 | `jar/code/mainbase/league/League.kt` | 1天 | 中 | 源 联赛(67) | ⬜ | 接 PLAY_LADDER 设置，复用进攻部署 |
| T11 | 中文玩家名 OCR | `jar/code/.../recognizer/PixelFontChinese.kt` + `tools/harvest_chinese_font.py` | 2天 | 中 | 源 font_chinese + 函数21a | ⬜ | 建中文像素字库，供部落名/村庄名读取 |

### P3 — 可选/低优

| ID | 任务 | 目标文件(当前项目) | 工作量 | 优先级 | 源依据 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|
| T12 | 大字号 OCR | `jar/code/.../recognizer/PixelFontLarge.kt` | 0.5天 | 低 | 源 函数315a(19×18) | ⬜ | 大数字字库 |
| T13 | 地图缩放检测移植 | `jar/code/universal/map/MapLocator.kt` | 1天 | 低 | 源 coc-assist MapLocator | ⬜ | 四边缘森林色占比→缩放0-3 |

### 夜世界（Builder Base）增强（对齐源 函数123a/323a）

> 当前项目 `BuilderBaseAttack.kt` 已实现打鱼/练兵/升级主流程（判为已覆盖），但对照源 `awcocx_main.lua` 的 `函数123a(下兵)` / `函数323a(双指滑屏放兵)` / 战斗监控(16606~16669)，存在以下可抄增强点。

| ID | 任务 | 目标文件(当前项目) | 工作量 | 优先级 | 源依据 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|
| T14 | 夜世界夜飞机(空中机器)英雄部署 | `jar/code/builderbase/attack/BuilderBaseAttack.kt` | 1天 | 中 | 源 夜飞机槽(15979) | 🔄 | 代码已实现，用户手动验证中 |
| T15 | 夜世界多兵种识别与批量下兵 | `jar/code/builderbase/attack/BuilderBaseAttack.kt` + `builderbase` colors | 2天 | 中 | 源 函数123a(15997~16071) | 🔄 | 12 兵种颜色 + `deployAllTroops`；**实机发现单点落点会压到基地建筑区(不可下兵)**，已改为四象限多点候选 `buildDeployPoints()` + `buildFallbackDeployPoints()` 边缘环带兜底；下兵状态机：白框 `isCardSelected()` 判选中（避免重复点卡=放技能）、灰卡饱和度 `cardSaturation()` 判已下完、单轮上限 `MAX_TROOPS_PER_ROUND=30`/连点 `DEPLOY_TAPS_PER_ROUND=8`、英雄卡只点一次并立刻标记耗尽，待验证 |
| T16 | 夜世界英雄技能自动释放 | `jar/code/builderbase/attack/BuilderBaseAttack.kt` + `builderbase` colors | 1天 | 中 | 源 战斗监控(16606~16669) | 🔄 | 夜飞机按卡槽 rescope 检测粉光后点槽；战争机器改用**充能条第 1 格亮起**判据（新增 `HeroChargeReady`，位置 (95,560)-(112,565)、亮色 #C022FB，由用户实机截图经 `tools/make_feature.py` 标定），命中即点英雄卡槽释放；**颜色串是 BGR 且为上中下三层垂直叠色**，`HeroChargeReady` 修正为 (93,559)-(116,570) 三层 FB25C1/FE3AC7/FF98DF、`TroopSkills` 同理修正（此前按 RGB 解释被改坏），待验证 |
| T17 | 场景识别健壮性（载入页/战斗中/编辑模式/未识别重试） | `jar/code/universal/SceneState.kt` + `colorpackage/UIColors.kt` | 1天 | 高 | 15 张实机未识别 debug 截图 | 🔄 | 新增 `GameScene.LOADING` 与特征 `GameLoadingNotice`(合规黑屏)/`BuilderBaseEditMode`(夜世界编辑模式)；战斗中(`EndBattle`等)归 `BATTLE`；未识别先等 1.5s×2 重试、载入等待 15–90s；载入页不再按返回（修复游戏退出确认弹窗），待验证 |
| T18 | 日志分级与文件落盘 | `core/util/basic/ShowMessage.kt` + `core/util/fileactions/LogHelper.kt` | 1天 | 中 | — | 🔄 | `ShowMessage` 恢复旧 `invoke` 入口(保持旧 jar 二进制兼容)并新增 `run()/warn()/error()/log()`；`LogHelper` 按 DEBUG(全量 VERBOSE)/RELEASE(仅 INFO+) 分级落文件、缓冲 100→200→**2000**（夜世界下兵诊断每轮 3 行、一场约 200 行，200 行会滚掉上一场）；夜世界里程碑改 `ShowMessage.run()`，待验证 |

### 通用稳定性修复（实机问题驱动，不入阶段表）

| ID | 任务 | 目标文件(当前项目) | 工作量 | 优先级 | 源依据 | 状态 | 备注 |
|---|---|---|---|---|---|---|---|
| T20 | 「橙色转圈卡死」检测误判修复 | `jar/code/universal/smalltools/CheckReconnections.kt` | 0.5天 | 高 | 实机：夜世界「开始进攻」确认弹窗被判网络卡死并重启游戏 | 🔄 | 采样区收窄到中央 (480,280)-(800,440) 避免橙色装饰误入；卡死计时改为**跨循环累计**（原实现单次调用阻塞 12s，导致弹窗白等十几秒后仍被重启）；命中 `AttackNow`/`CancelAttackSearch` 等静止等待界面直接放行并复位计时，待验证 |
| T21 | swipe 手势误触统一（长按拖建筑 / 被判点击弹「信息」面板） | `core/util/touchactions/TouchActions.kt` + 夜世界/主世界 zoom、收集资源、城墙批量建造调用点 | 0.5天 | 高 | 实机：swipe 起点常压在村庄建筑/城墙上 | 🔄 | `swipe` 是「按下→停顿(delayTime×0.7×倍率)→移动」：停顿过长被判长按拖建筑、过短被判点击选中建筑。默认 300~400ms→150~200ms；各调用点统一 `delayTime=180`（收集资源/夜世界 zoom/主世界 zoom 由 100、120、600、800 统一为 180，城墙批量 600→120）；收集资源盲点后补 `clickRightBottom()` 清掉选中状态，待验证 |

---

## 三、决策记录（已明确不抄项，留存追溯）

| 项 | 决策 | 依据 | 日期 |
|---|---|---|---|
| 村庄改名 | ❌ 不抄 | 用户 2026-09-23 明确决定 | 2026-09-23 |
| 部落靓标签筛选 | ❌ 不抄 | 用户 2026-09-23 明确决定 | 2026-09-23 |

---

## 四、进度汇总

- 总任务数：21（T01–T21）
- ✅ 已完成：0
- 🔄 进行中：9（T02,T14,T15,T16,T17,T18,T19,T20,T21）
- ⬜ 待办：11（T01,T03–T05,T07–T13）
- ⏸ 暂停：1（T06 延后）
- ❌ 不抄/取消：2（村庄改名、部落靓标签，见决策记录）

---

## 五、更新记录

| 日期 | 任务ID | 变更 | 说明 |
|---|---|---|---|
| 2026-09-23 | T01–T13 | 新建 | 由 `docs/源项目功能对比与移植计划.md` §5 导入全部开发任务，确立本文件为任务唯一基准 |
| 2026-09-23 | — | 决策 | 村庄改名、部落靓标签标记为不抄（不立对应任务，记入决策记录） |
| 2026-09-23 | T14–T16 | 新建 | 新增夜世界增强任务（夜飞机部署 / 多兵种识别批量下兵 / 英雄技能释放），对齐源 函数123a/323a |
| 2026-09-23 | T06 | 状态 ⟳ | 用户决定延后都城，先做夜世界；T06 → ⏸ |
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
