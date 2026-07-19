# 生电机制与 Mod 合法行为白皮书（精简版 v0.1）

> LatchAC M1 交付物。原则：生电即公理——所有原版机制行为默认合法，检测器必须先穷尽"合法解释"再给可疑度评分。
> 本文档随 M3 各检测器开发逐项实测校准，"待实测"条目在落地前不得作为判定依据。

## 1. 版本基线

- 目标：Minecraft 26.1.2 / 26.2（CalVer `年.drop.patch`；26.1 起强制 Java 25）
- 0-tick 脉冲已于 1.16 被 Mojang 修复，现代版本以 1gt 脉冲为主——检测参数按 26.x 行为设计，不背历史包袱
- 更新抑制在 Paper 上默认受限（异常被捕获），依赖更新抑制的机器在 Paper 系服务器上本就少见

### Paper 机制开关感知表（M3 启动时读取，决定检测策略）

| paper-global.yml 键 | 默认 | 开启后 LatchAC 行为 |
|---|---|---|
| `unsupported-settings.allow-piston-duplication` | false | TNT/地毯/铁轨复制上下文豁免生效 |
| `unsupported-settings.allow-permanent-block-break-exploits` | false | 基岩消失需走红石上下文分析；关闭时基岩消失直接高可疑 |
| `unsupported-settings.allow-headless-pistons` | false | 无头活塞结构识别纳入合法结构库 |

## 2. 检测边界决策表

| 行为 | 判定 | 关键上下文信号 |
|---|---|---|
| 无载具持续上升/悬停 | 拦 | 无鞘翅、无烟花、无悬浮效果、无载具、无 15 格内弹射环境 |
| 鞘翅+烟花加速 | 放 | `isGliding` + 烟花使用事件时序 |
| 激流三叉戟飞行 | 放 | Riptide 附魔 + 雨中/水中状态 |
| 末影珍珠超传 | 放 | 珍珠实体投掷/着陆事件链 |
| 活塞/史莱姆弹射矢量突变 | 放 | 15 格内活塞激活事件或粘液块（机器友好原则） |
| 矿车/船内异常速度 | 放 | `isInsideVehicle` |
| 船飞（无水面持续升空） | 拦 | 船体脱离水面持续获得升力且无冰道/掉落上下文 |
| 高实体密度区多目标攻击 | 放宽 | 攻击半径内实体数超阈值（猪人塔/守卫者塔），并发攻击门槛自动上调 |
| 杀戮光环 | 拦 | 多目标瞬时切换 + 视角转速超人类 + 攻击可达性失败 |
| 秒挖 | 拦 | 无工具/无急迫/无信标/非创造下破坏耗时 < 理论下限 |
| 无摔落伤害 | 拦 | 高处坠落无伤且无水/藤蔓/缓降/鞘翅/珍珠上下文 |
| 穿墙 | 拦 | 穿越完整实心方块且无可移动方块实体（活塞推动中）上下文 |
| 红石 TNT 破基岩 | 放(记录) | 爆炸事件 + 红石信号 + 无头活塞结构三要素齐全 |
| 空手基岩变空气 | 拦(高危) | 无爆炸事件、无红石上下文、直接方块状态突变 |
| 自动钓鱼脚本 | 拦 | 收杆与上钩事件相关性 ≈ 1 且反应时间方差趋零 |
| 盲挂机钓鱼（连点器式） | 放 | 周期性右键与上钩事件无相关性 |
| X-Ray | 拦(慢累计) | 矿物命中率/挖掘路径直达率长期显著超基线，仅慢速累计从不即时判定 |

## 3. Mod 行为特征库（初版）

### Tweakeroo（客户端，连 Paper 服正常）
- fastRightClick / fastPlacement：固定 tick 间隔右键、视角保持不动或极小偏差
- 豁免策略：命中节奏模式 → 交互频率检测信用分上调
- 待实测：26.x 版本实际包间隔分布

### Litematica（客户端）
- printer 模式：极高频方块放置 + 放置目标与周边结构强一致 + 精准视角抖动
- 豁免策略：仅验证物品来源与方块可破坏性，速度/精度不作为扣分项
- 待实测：printer 放置速率上限、easy place 与 printer 的包特征差异

### 假人（Paper 生态：fakeplayer 类插件 / Citizens NPC）
- 通用特征：`Player#getAddress()` 为 null；metadata 含 `NPC` 或 `fakeplayer`
- 豁免策略：采集层打 `fake-player` 标签 → 豁免矩阵 TagRule 全量授信
- 待实测：主流假人插件（tanyaofei/minecraft-fakeplayer、FakePlayerPlugin）的实际特征值

### Item Scroller（客户端）
- 批量库存操作的密集窗口点击包
- 豁免策略：库存操作频率不纳入 M2/M3 检测范围，天然零冲突

## 4. 待验证清单（M3 前必须实测销项）

- [ ] fakeplayer 插件假人 `getAddress()` 实际返回值与 metadata 键
- [ ] Tweakeroo fastRightClick 实际间隔（4gt 假设需验证）
- [ ] Litematica printer 每 tick 放置数上限
- [x] Paper 26.x 是否仍内置 SnakeYAML —— **已验证（M2 冒烟，Paper 26.1.2 build 74）：内置。** 插件不打包 snakeyaml，`ConfigParser` 直接使用运行时提供的类，onEnable 配置解析正常。
- [x] `api-version: '26.1'` 在 Paper 26.1.2 的加载行为 —— **已验证（M2 冒烟）：接受。** `Loading server plugin LatchAC v0.0.1` 正常，且骨架模板默认的 `'26.2'` 高于服务器版本存在拒载风险，保持 `'26.1'`。
- [ ] 珍珠炮着陆瞬间的移动事件序列特征
- [ ] TNT 飞行器乘员的速度包络

## 5. M2 冒烟结论备忘（2026-07-18，Paper 26.1.2 build 74 / Java 25）

- 启动 → 启用 → `/latchac reload` → 优雅停服全链路通过；坏配置下"启动回退内置默认 + 热重载保留旧配置"两条韧性路径实机命中。
- **控制台中文乱码**：Windows GBK 终端下 onEnable 中文日志显示为乱码，但 `logs/latest.log` 文件内 UTF-8 完全正常（审计载体无碍）。M3 注意项：文档提示中文服主配置 `-Dstdout.encoding=UTF-8`，或考虑控制台日志采用 ASCII 安全格式。
- **Paper 26.x 控制台时序**：世界加载完成前注入的控制台命令会在原版命令层 NPE（`CommandSourceStack.getLevel()` 为 null），自动化测试需等待 `Done` 后再发命令。
- 红线审计：全代码库 `setVelocity` / `teleport(` 零命中（默认无回弹承诺成立）。
