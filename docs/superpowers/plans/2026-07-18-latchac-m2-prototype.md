# LatchAC M1(精简)+M2 架构原型实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 Paper 26.1.2 插件骨架上完成多模块化改造，交付 M1 精简白皮书与 M2 四层架构原型：核心引擎（评分模型 → 锁存聚合 → 四维豁免 → 可审计判定）全 TDD，Paper 层打通"移动事件 → 引擎 → 决策日志"流水线，支持假人自动豁免路由与 `/latchac reload` 热重载，在 run-paper 启动的 Paper 26.1.2 上冒烟通过。

**Architecture:** 双模块分层——`latchac-core`（平台无关纯 Java：Suspicion 评分模型、LatchAccumulator 指数衰减锁存累计器、ExemptionMatrix 豁免矩阵、ConfigParser、LatchEngine 门面）与 `latchac-paper`（薄适配层：事件监听、假人识别 SPI、命令、配置文件 IO）。评分单向上行，豁免命中即清零，热重载为配置纪元(Epoch)整体原子替换。默认只记录、绝不回弹、绝不修改玩家状态。

**Tech Stack:** Java 25 (Zulu LTS，已安装) · Gradle 9.6.1 (wrapper 已就绪) · paper-api `26.1.2.build.+` · run-paper 3.0.2 · SnakeYAML 2.2 (Paper 运行时自带) · JUnit 5.11

---

## 背景与范围

### 总体路线图（本计划仅覆盖 M1精简 + M2）

| 里程碑 | 内容 | 状态 |
|--------|------|------|
| M1 | 精简白皮书：检测边界决策表 + Mod 行为特征库骨架 | **本计划 Task 2** |
| M2 | 四层架构原型 + 豁免热重载 + 假人豁免路由 | **本计划 Task 1, 3-10** |
| M3 | 移动/战斗/交互/资源四大检测器 | 后续独立计划 |
| M4 | Folia 区域调度验证与压测 | 后续独立计划 |
| M5-M7 | 真实服内测 / 文档社区 / 正式发布 | 后续独立计划 |

### 已确认的技术事实（2026-07 核实）

- Minecraft 自 2026 起使用 CalVer（`年.drop.patch`）：1.21.11 → 26.1 "Tiny Takeover" → 26.2 "Chaos Cubed"。26.1+ 强制 Java 25。
- Spigot 官方声明 26.1 相对 1.21.11 无重大 API 破坏，1.21 时代的 Bukkit/Paper API 写法安全。
- Carpet Mod 是 Fabric-only，Paper 生态的假人由 fakeplayer 类插件提供 → 假人授信采用**通用特征识别 + SPI 扩展**，不绑定任何具体插件。
- Paper 默认关闭 TNT 复制/基岩破除等机制（`unsupported-settings`），机制感知留待 M3。
- paper-api 26.x 自带 Folia 系调度接口（RegionScheduler 等）；M2 不用调度器（纯事件驱动），天然 Folia 友好，`plugin.yml` 先行声明 `folia-supported: true`。

### 现有骨架（改造起点）

- 根目录：`d:\desktop\project\Latch AntiCheat`（下文所有路径省略此前缀）
- 已有：`build.gradle.kts`（单模块）、`settings.gradle.kts`、`gradle/libs.versions.toml`、`src/main/java/org/coffeepop/latchAC/LatchAntiCheat.java`（空壳主类）、`src/main/resources/plugin.yml`
- git 已 init，**无任何 commit**，骨架文件已 staged
- 包名统一为全小写 `org.coffeepop.latchac`（现有 `latchAC` 大小写混用不合规范，趁空壳期归一）

### 设计红线（来自 Latch 哲学，实现时不可违背）

1. 任何代码路径都**不得修改玩家位置/速度**（默认无回弹）。
2. 豁免命中 = 评分清零丢弃，且**必须留下可读原因**（透明审计）。
3. 热重载失败必须**保留旧配置**继续运行，不得让插件进入无配置状态。
4. 单次毛刺不得触发判定——只有持续证据能推过阈值（锁存语义，由半衰期衰减保证）。

## 最终文件结构

```
Latch AntiCheat/
├── settings.gradle.kts                  (修改: include 两模块)
├── build.gradle.kts                     (修改: 仅保留公共配置)
├── gradle.properties                    (不动)
├── gradle/libs.versions.toml            (修改: 增加 snakeyaml/junit)
├── docs/
│   ├── whitepaper.md                    (新建: M1 精简白皮书)
│   └── superpowers/plans/2026-07-18-latchac-m2-prototype.md (本文档)
├── latchac-core/                        (新建模块: 平台无关, 零 Bukkit 依赖)
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/org/coffeepop/latchac/core/
│       │   ├── model/Suspicion.java     (评分模型)
│       │   ├── model/Verdict.java       (判定结果)
│       │   ├── model/Outcome.java       (提交结局: 豁免/评分/禁用)
│       │   ├── latch/LatchAccumulator.java (锁存累计器)
│       │   ├── exempt/ExemptionContext.java
│       │   ├── exempt/ExemptionRule.java
│       │   ├── exempt/RegionRule.java
│       │   ├── exempt/PlayerRule.java
│       │   ├── exempt/TagRule.java
│       │   ├── exempt/ExemptionMatrix.java
│       │   ├── config/LatchConfig.java
│       │   ├── config/ConfigParser.java
│       │   └── LatchEngine.java         (核心门面)
│       └── test/java/org/coffeepop/latchac/core/
│           ├── model/SuspicionTest.java
│           ├── latch/LatchAccumulatorTest.java
│           ├── exempt/ExemptionMatrixTest.java
│           ├── config/ConfigParserTest.java
│           └── LatchEngineTest.java
└── latchac-paper/                       (新建模块: Paper 适配层)
    ├── build.gradle.kts
    └── src/main/
        ├── java/org/coffeepop/latchac/paper/
        │   ├── LatchAntiCheat.java      (主类, 从旧 src/ 迁移重写)
        │   ├── command/LatchacCommand.java
        │   ├── fake/FakePlayerProvider.java   (SPI)
        │   ├── fake/HeuristicFakePlayerProvider.java
        │   ├── fake/FakePlayerRegistry.java
        │   └── sensor/MovementSensor.java
        └── resources/
            ├── plugin.yml               (从旧 src/ 迁移修改)
            └── config.yml               (新建: 默认配置)
```

> **测试策略说明：** core 模块全部 TDD（纯 Java 可单测）。paper 模块是薄适配层，Bukkit `Player` 接口 mock 成本高且 MockBukkit 对 26.x 的可用性未验证，M2 以 Task 10 的 run-paper 冒烟验证代替单测；M3 计划中再评估引入 MockBukkit。

> **commit 纪律（用户规则"分主题 commit"）：** 每个 Task 恰好一个主题 commit，Task 1 例外（骨架快照 + 多模块化，两个主题两个 commit）。

---

### Task 1: 提交骨架快照 + 多模块化改造

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `gradle/libs.versions.toml`
- Create: `latchac-core/build.gradle.kts`
- Create: `latchac-paper/build.gradle.kts`
- Create: `latchac-paper/src/main/java/org/coffeepop/latchac/paper/LatchAntiCheat.java`（迁移）
- Create: `latchac-paper/src/main/resources/plugin.yml`（迁移）
- Delete: `src/`（整个旧目录）

- [ ] **Step 1: 先把 IDE 生成的骨架作为第一个 commit 固化**

```powershell
git add -A
git commit -m "chore: IDE 生成的 Paper 26.1.2 插件初始骨架"
```

预期：`master (root-commit)`，12+ 文件入库。

- [ ] **Step 2: 修改 `settings.gradle.kts` 声明两个子模块**

整文件替换为：

```kotlin
rootProject.name = "Latch AntiCheat"

include("latchac-core", "latchac-paper")
```

- [ ] **Step 3: 修改根 `build.gradle.kts` 为纯公共配置**

整文件替换为（root 自身不再产出构件，run-paper 插件移交 paper 模块）：

```kotlin
subprojects {
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion = JavaLanguageVersion.of(25)
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
```

- [ ] **Step 4: 扩充 `gradle/libs.versions.toml`**

整文件替换为：

```toml
[versions]
paper-api = "26.1.2.build.+"
minecraft = "26.1.2"

run-task = "3.0.2"
snakeyaml = "2.2"
junit = "5.11.4"

[libraries]
paper-api = { module = "io.papermc.paper:paper-api", version.ref = "paper-api" }
snakeyaml = { module = "org.yaml:snakeyaml", version.ref = "snakeyaml" }
junit-bom = { module = "org.junit:junit-bom", version.ref = "junit" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter" }
junit-platform-launcher = { module = "org.junit.platform:junit-platform-launcher" }

[plugins]
run-paper = { id = "xyz.jpenilla.run-paper", version.ref = "run-task" }
```

- [ ] **Step 5: 创建 `latchac-core/build.gradle.kts`**

```kotlin
// LatchAC 核心模块：平台无关，禁止出现任何 Bukkit/Paper 依赖
dependencies {
    implementation(libs.snakeyaml)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
```

- [ ] **Step 6: 创建 `latchac-paper/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.run.paper)
}

dependencies {
    implementation(project(":latchac-core"))
    compileOnly(libs.paper.api)
}

tasks {
    jar {
        archiveBaseName.set("LatchAC")
        // 将 core 模块类并入插件 jar；snakeyaml 不打包（Paper 运行时自带）
        from(project(":latchac-core").sourceSets["main"].output)
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true")
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
```

- [ ] **Step 7: 迁移主类到 paper 模块（新包名，暂保持空壳可编译）**

创建 `latchac-paper/src/main/java/org/coffeepop/latchac/paper/LatchAntiCheat.java`：

```java
package org.coffeepop.latchac.paper;

import org.bukkit.plugin.java.JavaPlugin;

public final class LatchAntiCheat extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("LatchAC 骨架已加载（M2 原型开发中）");
    }

    @Override
    public void onDisable() {
    }
}
```

- [ ] **Step 8: 迁移 `plugin.yml` 到 paper 模块**

创建 `latchac-paper/src/main/resources/plugin.yml`（main 指向新包；api-version 降为 `'26.1'`——原骨架的 `'26.2'` 高于目标服务器 26.1.2 会拒绝加载；声明 Folia 兼容）：

```yaml
name: LatchAC
version: '${version}'

main: org.coffeepop.latchac.paper.LatchAntiCheat
api-version: '26.1'
load: POSTWORLD
folia-supported: true

description: Latch AntiCheat —— 零误伤生电友好反作弊（如红石锁存器，精准记忆合法状态）
authors: [ Neamyoo ]

commands:
  latchac:
    description: LatchAC 管理命令
    permission: latchac.admin
    usage: /latchac reload

permissions:
  latchac.admin:
    description: LatchAC 管理权限
    default: op
```

- [ ] **Step 9: 删除旧 `src/` 目录**

删除 `src/main/java/org/coffeepop/latchAC/LatchAntiCheat.java` 与 `src/main/resources/plugin.yml`，随后删除空的 `src/` 树。

- [ ] **Step 10: 验证构建**

```powershell
.\gradlew.bat build
```

预期：`BUILD SUCCESSFUL`，产物 `latchac-paper/build/libs/LatchAC-0.0.1.jar`。

- [ ] **Step 11: Commit**

```powershell
git add -A
git commit -m "refactor: 多模块化——latchac-core(平台无关核心) + latchac-paper(适配层)"
```

---

### Task 2: M1 精简白皮书

**Files:**
- Create: `docs/whitepaper.md`

- [ ] **Step 1: 写入白皮书初版**

创建 `docs/whitepaper.md`，内容如下（这是活文档，M3 各检测器开发时逐项实测校准）：

```markdown
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
- [ ] Paper 26.x 是否仍内置 SnakeYAML（M2 Task 10 冒烟顺带验证）
- [ ] `api-version: '26.1'` 在 Paper 26.1.2 的加载行为（M2 Task 10 验证）
- [ ] 珍珠炮着陆瞬间的移动事件序列特征
- [ ] TNT 飞行器乘员的速度包络
```

- [ ] **Step 2: Commit**

```powershell
git add docs/whitepaper.md
git commit -m "docs: M1 精简白皮书——检测边界决策表与 Mod 行为特征库"
```

---

### Task 3: core 评分模型 Suspicion / Verdict

**Files:**
- Create: `latchac-core/src/main/java/org/coffeepop/latchac/core/model/Suspicion.java`
- Create: `latchac-core/src/main/java/org/coffeepop/latchac/core/model/Verdict.java`
- Test: `latchac-core/src/test/java/org/coffeepop/latchac/core/model/SuspicionTest.java`

- [ ] **Step 1: 写失败测试**

创建 `SuspicionTest.java`：

```java
package org.coffeepop.latchac.core.model;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuspicionTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void 合法评分可创建并携带证据() {
        Suspicion s = new Suspicion("movement.flight", PLAYER, 0.5, 1000L, Map.of("dy", "1.2"));
        assertEquals("movement.flight", s.checkId());
        assertEquals(0.5, s.score());
        assertEquals("1.2", s.evidence().get("dy"));
    }

    @Test
    void 评分超出01区间被拒绝() {
        assertThrows(IllegalArgumentException.class,
                () -> new Suspicion("movement.flight", PLAYER, 1.1, 1000L, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new Suspicion("movement.flight", PLAYER, -0.1, 1000L, Map.of()));
    }

    @Test
    void 空白checkId被拒绝() {
        assertThrows(IllegalArgumentException.class,
                () -> new Suspicion("  ", PLAYER, 0.5, 1000L, Map.of()));
    }

    @Test
    void 证据允许为null且结果不可变() {
        Suspicion s = new Suspicion("x", PLAYER, 0.1, 0L, null);
        assertTrue(s.evidence().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> s.evidence().put("k", "v"));
    }
}
```

- [ ] **Step 2: 运行确认编译失败**

```powershell
.\gradlew.bat :latchac-core:test
```

预期：FAILED，`Suspicion` 符号不存在。

- [ ] **Step 3: 实现 Suspicion 与 Verdict**

创建 `Suspicion.java`：

```java
package org.coffeepop.latchac.core.model;

import java.util.Map;
import java.util.UUID;

/**
 * 单次可疑行为评分（0.0 ~ 1.0）。
 * 检测器只提供证据，不做判决——判决权在 LatchEngine（Arbiter 层）。
 */
public record Suspicion(
        String checkId,
        UUID playerId,
        double score,
        long timestampMillis,
        Map<String, String> evidence
) {
    public Suspicion {
        if (checkId == null || checkId.isBlank()) {
            throw new IllegalArgumentException("checkId 不能为空");
        }
        if (playerId == null) {
            throw new IllegalArgumentException("playerId 不能为空");
        }
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("score 必须在 [0.0, 1.0] 区间，实际: " + score);
        }
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }
}
```

创建 `Verdict.java`：

```java
package org.coffeepop.latchac.core.model;

import java.util.UUID;

/** 锁存聚合后的判定结果。tripped=true 表示累计值已越过阈值（锁存翻转）。 */
public record Verdict(
        UUID playerId,
        String checkId,
        double aggregateScore,
        boolean tripped,
        String reason
) {}
```

- [ ] **Step 4: 运行测试通过**

```powershell
.\gradlew.bat :latchac-core:test
```

预期：`BUILD SUCCESSFUL`，SuspicionTest 4 例全绿。

- [ ] **Step 5: Commit**

```powershell
git add latchac-core/src
git commit -m "feat(core): Suspicion 评分模型与 Verdict 判定结果"
```

---

### Task 4: core 锁存累计器 LatchAccumulator

**Files:**
- Create: `latchac-core/src/main/java/org/coffeepop/latchac/core/latch/LatchAccumulator.java`
- Test: `latchac-core/src/test/java/org/coffeepop/latchac/core/latch/LatchAccumulatorTest.java`

- [ ] **Step 1: 写失败测试**

创建 `LatchAccumulatorTest.java`（时间全部显式注入，测试确定性）：

```java
package org.coffeepop.latchac.core.latch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatchAccumulatorTest {

    @Test
    void 单次毛刺不触发() {
        LatchAccumulator acc = new LatchAccumulator(3.0, 4000);
        assertEquals(1.0, acc.accumulate(1.0, 0), 1e-9);
        assertFalse(acc.isTripped(0));
    }

    @Test
    void 持续高分推过阈值后锁存翻转() {
        LatchAccumulator acc = new LatchAccumulator(3.0, 4000);
        acc.accumulate(1.0, 0);
        acc.accumulate(1.0, 100);
        acc.accumulate(1.0, 200);
        acc.accumulate(1.0, 300);
        assertTrue(acc.isTripped(300));
    }

    @Test
    void 经过一个半衰期评分减半() {
        LatchAccumulator acc = new LatchAccumulator(3.0, 4000);
        acc.accumulate(2.0, 0);
        assertEquals(1.0, acc.currentValue(4000), 1e-9);
    }

    @Test
    void 长时间静默后毛刺被完全稀释() {
        LatchAccumulator acc = new LatchAccumulator(3.0, 4000);
        acc.accumulate(2.9, 0);
        // 10 个半衰期 ≈ 千分之一
        assertTrue(acc.currentValue(40_000) < 0.01);
        assertFalse(acc.isTripped(40_000));
    }

    @Test
    void 非法构造参数被拒绝() {
        assertThrows(IllegalArgumentException.class, () -> new LatchAccumulator(0, 4000));
        assertThrows(IllegalArgumentException.class, () -> new LatchAccumulator(3.0, 0));
    }
}
```

- [ ] **Step 2: 运行确认失败**

```powershell
.\gradlew.bat :latchac-core:test --tests "org.coffeepop.latchac.core.latch.LatchAccumulatorTest"
```

预期：FAILED，`LatchAccumulator` 符号不存在。

- [ ] **Step 3: 实现 LatchAccumulator**

创建 `LatchAccumulator.java`：

```java
package org.coffeepop.latchac.core.latch;

/**
 * 锁存累计器——LatchAC 的核心语义。
 * 累计值随时间指数衰减（半衰期 halfLifeMillis），新评分线性叠加。
 * 瞬时毛刺（网络抖动、单次误报）被时间稀释，只有持续证据才能推动累计值
 * 越过 tripThreshold——如同锁存器只对持续信号翻转。
 * 非线程安全：调用方需保证同一实例串行访问（LatchEngine 对实例加锁）。
 */
public final class LatchAccumulator {

    private final double tripThreshold;
    private final long halfLifeMillis;

    private double value;
    private long lastTimestamp = Long.MIN_VALUE;

    public LatchAccumulator(double tripThreshold, long halfLifeMillis) {
        if (tripThreshold <= 0) {
            throw new IllegalArgumentException("tripThreshold 必须为正数");
        }
        if (halfLifeMillis <= 0) {
            throw new IllegalArgumentException("halfLifeMillis 必须为正数");
        }
        this.tripThreshold = tripThreshold;
        this.halfLifeMillis = halfLifeMillis;
    }

    /** 衰减到 nowMillis 后叠加评分，返回当前累计值。 */
    public double accumulate(double score, long nowMillis) {
        decayTo(nowMillis);
        value += score;
        return value;
    }

    /** 只衰减不叠加，读取当前累计值。 */
    public double currentValue(long nowMillis) {
        decayTo(nowMillis);
        return value;
    }

    public boolean isTripped(long nowMillis) {
        return currentValue(nowMillis) >= tripThreshold;
    }

    private void decayTo(long nowMillis) {
        if (lastTimestamp == Long.MIN_VALUE) {
            lastTimestamp = nowMillis;
            return;
        }
        long dt = nowMillis - lastTimestamp;
        if (dt > 0) {
            value *= Math.pow(0.5, (double) dt / halfLifeMillis);
            lastTimestamp = nowMillis;
        }
    }
}
```

- [ ] **Step 4: 运行测试通过**

```powershell
.\gradlew.bat :latchac-core:test
```

预期：全绿（Suspicion 4 例 + Accumulator 5 例）。

- [ ] **Step 5: Commit**

```powershell
git add latchac-core/src
git commit -m "feat(core): LatchAccumulator 锁存累计器——指数衰减+阈值翻转"
```

---

### Task 5: core 豁免矩阵

**Files:**
- Create: `latchac-core/src/main/java/org/coffeepop/latchac/core/exempt/ExemptionContext.java`
- Create: `latchac-core/src/main/java/org/coffeepop/latchac/core/exempt/ExemptionRule.java`
- Create: `latchac-core/src/main/java/org/coffeepop/latchac/core/exempt/RegionRule.java`
- Create: `latchac-core/src/main/java/org/coffeepop/latchac/core/exempt/PlayerRule.java`
- Create: `latchac-core/src/main/java/org/coffeepop/latchac/core/exempt/TagRule.java`
- Create: `latchac-core/src/main/java/org/coffeepop/latchac/core/exempt/ExemptionMatrix.java`
- Test: `latchac-core/src/test/java/org/coffeepop/latchac/core/exempt/ExemptionMatrixTest.java`

- [ ] **Step 1: 写失败测试**

创建 `ExemptionMatrixTest.java`：

```java
package org.coffeepop.latchac.core.exempt;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExemptionMatrixTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static ExemptionContext ctx(String world, double x, double y, double z, Set<String> tags) {
        return new ExemptionContext(PLAYER, "Steve", world, x, y, z, "movement.flight", tags);
    }

    @Test
    void 区域内命中豁免_区域外不命中() {
        RegionRule rule = new RegionRule("machine-hall", "world", -10, 0, -10, 10, 320, 10);
        assertTrue(rule.exempts(ctx("world", 0, 64, 0, Set.of())).isPresent());
        assertTrue(rule.exempts(ctx("world", 11, 64, 0, Set.of())).isEmpty());
        assertTrue(rule.exempts(ctx("world_nether", 0, 64, 0, Set.of())).isEmpty());
    }

    @Test
    void 玩家名单按名字或UUID命中() {
        PlayerRule byName = new PlayerRule(Set.of(), Set.of("Steve"));
        PlayerRule byUuid = new PlayerRule(Set.of(PLAYER), Set.of());
        assertTrue(byName.exempts(ctx("world", 0, 0, 0, Set.of())).isPresent());
        assertTrue(byUuid.exempts(ctx("world", 0, 0, 0, Set.of())).isPresent());
    }

    @Test
    void 标签规则命中假人授信() {
        TagRule rule = new TagRule("fake-player", "Carpet 类假人授信");
        assertEquals(Optional.of("Carpet 类假人授信"),
                rule.exempts(ctx("world", 0, 0, 0, Set.of("fake-player"))));
        assertTrue(rule.exempts(ctx("world", 0, 0, 0, Set.of())).isEmpty());
    }

    @Test
    void 矩阵返回第一条命中原因_全不命中返回空() {
        ExemptionMatrix matrix = new ExemptionMatrix(List.of(
                new RegionRule("hall", "world", -10, 0, -10, 10, 320, 10),
                new TagRule("fake-player", "假人授信")));
        assertEquals(Optional.of("区域豁免: hall"),
                matrix.firstExemption(ctx("world", 0, 64, 0, Set.of("fake-player"))));
        assertTrue(matrix.firstExemption(ctx("world", 999, 64, 999, Set.of())).isEmpty());
    }

    @Test
    void 空矩阵永不豁免() {
        ExemptionMatrix matrix = new ExemptionMatrix(List.of());
        assertTrue(matrix.firstExemption(ctx("world", 0, 0, 0, Set.of("fake-player"))).isEmpty());
    }
}
```

- [ ] **Step 2: 运行确认失败**

```powershell
.\gradlew.bat :latchac-core:test --tests "org.coffeepop.latchac.core.exempt.ExemptionMatrixTest"
```

预期：FAILED，符号不存在。

- [ ] **Step 3: 实现豁免体系**

创建 `ExemptionContext.java`：

```java
package org.coffeepop.latchac.core.exempt;

import java.util.Set;
import java.util.UUID;

/**
 * 豁免判定所需的最小上下文，平台无关。
 * tags 由采集层注入，例如 "fake-player"（假人授信）、未来的 "mod:litematica-printer"。
 */
public record ExemptionContext(
        UUID playerId,
        String playerName,
        String worldName,
        double x, double y, double z,
        String checkId,
        Set<String> tags
) {
    public ExemptionContext {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }
}
```

创建 `ExemptionRule.java`：

```java
package org.coffeepop.latchac.core.exempt;

import java.util.Optional;

/** 单条豁免规则。命中时必须返回人类可读的原因——透明审计是一等需求。 */
public interface ExemptionRule {

    /** @return 命中时为豁免原因，未命中为 empty */
    Optional<String> exempts(ExemptionContext ctx);
}
```

创建 `RegionRule.java`：

```java
package org.coffeepop.latchac.core.exempt;

import java.util.Optional;

/** 区域豁免：world + AABB 立方体，范围内行为完全免检（四维矩阵之"区域"维）。 */
public record RegionRule(String name, String worldName,
                         double minX, double minY, double minZ,
                         double maxX, double maxY, double maxZ) implements ExemptionRule {

    @Override
    public Optional<String> exempts(ExemptionContext ctx) {
        if (!worldName.equals(ctx.worldName())) {
            return Optional.empty();
        }
        boolean inside = ctx.x() >= minX && ctx.x() <= maxX
                && ctx.y() >= minY && ctx.y() <= maxY
                && ctx.z() >= minZ && ctx.z() <= maxZ;
        return inside ? Optional.of("区域豁免: " + name) : Optional.empty();
    }
}
```

创建 `PlayerRule.java`：

```java
package org.coffeepop.latchac.core.exempt;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家豁免：UUID 或名字名单（四维矩阵之"玩家"维）。
 * LuckPerms 权限节点豁免由 paper 层在采集时转换为 tag 注入，不在 core 耦合权限系统。
 */
public record PlayerRule(Set<UUID> uuids, Set<String> names) implements ExemptionRule {

    public PlayerRule {
        uuids = Set.copyOf(uuids);
        names = Set.copyOf(names);
    }

    @Override
    public Optional<String> exempts(ExemptionContext ctx) {
        if (uuids.contains(ctx.playerId()) || names.contains(ctx.playerName())) {
            return Optional.of("玩家豁免名单: " + ctx.playerName());
        }
        return Optional.empty();
    }
}
```

创建 `TagRule.java`：

```java
package org.coffeepop.latchac.core.exempt;

import java.util.Optional;

/** 标签豁免：采集层打上的授信标签（假人、Mod 行为模板等）直接放行。 */
public record TagRule(String tag, String description) implements ExemptionRule {

    @Override
    public Optional<String> exempts(ExemptionContext ctx) {
        return ctx.tags().contains(tag) ? Optional.of(description) : Optional.empty();
    }
}
```

创建 `ExemptionMatrix.java`：

```java
package org.coffeepop.latchac.core.exempt;

import java.util.List;
import java.util.Optional;

/**
 * 豁免矩阵：顺序询问所有规则，任一命中即豁免（评分直接清零）。
 * 不可变——热重载时由 LatchEngine 整体替换，天然线程安全。
 */
public final class ExemptionMatrix {

    private final List<ExemptionRule> rules;

    public ExemptionMatrix(List<ExemptionRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public Optional<String> firstExemption(ExemptionContext ctx) {
        for (ExemptionRule rule : rules) {
            Optional<String> hit = rule.exempts(ctx);
            if (hit.isPresent()) {
                return hit;
            }
        }
        return Optional.empty();
    }
}
```

- [ ] **Step 4: 运行测试通过**

```powershell
.\gradlew.bat :latchac-core:test
```

预期：全绿。

- [ ] **Step 5: Commit**

```powershell
git add latchac-core/src
git commit -m "feat(core): 豁免矩阵——区域/玩家/标签规则与可审计命中原因"
```

---

### Task 6: core 配置模型与 YAML 解析

**Files:**
- Create: `latchac-core/src/main/java/org/coffeepop/latchac/core/config/LatchConfig.java`
- Create: `latchac-core/src/main/java/org/coffeepop/latchac/core/config/ConfigParser.java`
- Test: `latchac-core/src/test/java/org/coffeepop/latchac/core/config/ConfigParserTest.java`

- [ ] **Step 1: 写失败测试**

创建 `ConfigParserTest.java`：

```java
package org.coffeepop.latchac.core.config;

import org.coffeepop.latchac.core.exempt.ExemptionContext;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigParserTest {

    private static final String FULL_YAML = """
            latch:
              half-life-ms: 5000
            checks:
              movement.flight:
                enabled: true
                trip-threshold: 3.5
                action: LOG
              combat.killaura:
                enabled: false
                trip-threshold: 2.0
                action: LOG
            exemptions:
              regions:
                - name: machine-hall
                  world: world
                  min: [-50, 0, -50]
                  max: [50, 320, 50]
              players:
                names: [Steve]
                uuids: ["00000000-0000-0000-0000-000000000009"]
              trusted-tags:
                - tag: fake-player
                  description: "假人授信"
            """;

    @Test
    void 完整配置解析() {
        LatchConfig config = new ConfigParser().parse(FULL_YAML);

        assertEquals(5000, config.halfLifeMillis());
        assertEquals(2, config.checks().size());
        LatchConfig.CheckConfig flight = config.checks().get("movement.flight");
        assertTrue(flight.enabled());
        assertEquals(3.5, flight.tripThreshold());
        assertEquals("LOG", flight.action());

        // 3 类规则：region + player + tag
        assertEquals(3, config.exemptionRules().size());
    }

    @Test
    void 解析出的规则真实可用() {
        LatchConfig config = new ConfigParser().parse(FULL_YAML);
        ExemptionContext inHall = new ExemptionContext(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "Alex", "world", 0, 64, 0, "movement.flight", Set.of());
        boolean anyHit = config.exemptionRules().stream()
                .anyMatch(r -> r.exempts(inHall).isPresent());
        assertTrue(anyHit, "机器大厅坐标应命中区域豁免");
    }

    @Test
    void 缺省节回退默认值() {
        LatchConfig config = new ConfigParser().parse("latch: {}\n");
        assertEquals(4000, config.halfLifeMillis());
        assertTrue(config.checks().isEmpty());
        assertTrue(config.exemptionRules().isEmpty());
    }

    @Test
    void 非法YAML抛出异常供调用方保留旧配置() {
        assertThrows(IllegalArgumentException.class, () -> new ConfigParser().parse("just a string"));
    }
}
```

- [ ] **Step 2: 运行确认失败**

```powershell
.\gradlew.bat :latchac-core:test --tests "org.coffeepop.latchac.core.config.ConfigParserTest"
```

预期：FAILED，符号不存在。

- [ ] **Step 3: 实现配置模型与解析器**

创建 `LatchConfig.java`：

```java
package org.coffeepop.latchac.core.config;

import org.coffeepop.latchac.core.exempt.ExemptionRule;

import java.util.List;
import java.util.Map;

/** 一次热重载产生的完整不可变配置。 */
public record LatchConfig(
        long halfLifeMillis,
        Map<String, CheckConfig> checks,
        List<ExemptionRule> exemptionRules
) {
    public LatchConfig {
        checks = Map.copyOf(checks);
        exemptionRules = List.copyOf(exemptionRules);
    }

    /** 单个检查项配置。action 在 M2 仅支持 LOG（静默日志），M3 扩展 NOTIFY/KICK/BAN。 */
    public record CheckConfig(boolean enabled, double tripThreshold, String action) {}
}
```

创建 `ConfigParser.java`：

```java
package org.coffeepop.latchac.core.config;

import org.coffeepop.latchac.core.exempt.ExemptionRule;
import org.coffeepop.latchac.core.exempt.PlayerRule;
import org.coffeepop.latchac.core.exempt.RegionRule;
import org.coffeepop.latchac.core.exempt.TagRule;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * YAML 文本 → 不可变 LatchConfig。
 * 任何解析失败抛 IllegalArgumentException，调用方保留旧配置继续运行（热重载安全法则）。
 */
public final class ConfigParser {

    private static final long DEFAULT_HALF_LIFE_MS = 4000;

    @SuppressWarnings("unchecked")
    public LatchConfig parse(String yamlText) {
        Object root = new Yaml().load(yamlText);
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("配置根节点必须是 YAML 映射");
        }
        Map<String, Object> map = (Map<String, Object>) root;
        try {
            return new LatchConfig(parseHalfLife(map), parseChecks(map), parseExemptions(map));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("配置结构错误: " + ex.getMessage(), ex);
        }
    }

    private long parseHalfLife(Map<String, Object> map) {
        Map<String, Object> latch = section(map, "latch");
        return ((Number) latch.getOrDefault("half-life-ms", DEFAULT_HALF_LIFE_MS)).longValue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, LatchConfig.CheckConfig> parseChecks(Map<String, Object> map) {
        Map<String, LatchConfig.CheckConfig> checks = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : section(map, "checks").entrySet()) {
            Map<String, Object> c = (Map<String, Object>) e.getValue();
            checks.put(e.getKey(), new LatchConfig.CheckConfig(
                    (Boolean) c.getOrDefault("enabled", Boolean.TRUE),
                    ((Number) c.getOrDefault("trip-threshold", 3.0)).doubleValue(),
                    String.valueOf(c.getOrDefault("action", "LOG"))));
        }
        return checks;
    }

    @SuppressWarnings("unchecked")
    private List<ExemptionRule> parseExemptions(Map<String, Object> map) {
        List<ExemptionRule> rules = new ArrayList<>();
        Map<String, Object> ex = section(map, "exemptions");

        for (Object o : (List<Object>) ex.getOrDefault("regions", List.of())) {
            Map<String, Object> r = (Map<String, Object>) o;
            List<Number> min = (List<Number>) r.get("min");
            List<Number> max = (List<Number>) r.get("max");
            rules.add(new RegionRule(
                    String.valueOf(r.getOrDefault("name", "unnamed")),
                    String.valueOf(r.get("world")),
                    min.get(0).doubleValue(), min.get(1).doubleValue(), min.get(2).doubleValue(),
                    max.get(0).doubleValue(), max.get(1).doubleValue(), max.get(2).doubleValue()));
        }

        Map<String, Object> players = section(ex, "players");
        Set<String> names = new HashSet<>((List<String>) players.getOrDefault("names", List.of()));
        Set<UUID> uuids = new HashSet<>();
        for (String u : (List<String>) players.getOrDefault("uuids", List.<String>of())) {
            uuids.add(UUID.fromString(u));
        }
        if (!names.isEmpty() || !uuids.isEmpty()) {
            rules.add(new PlayerRule(uuids, names));
        }

        for (Object o : (List<Object>) ex.getOrDefault("trusted-tags", List.of())) {
            Map<String, Object> t = (Map<String, Object>) o;
            String tag = String.valueOf(t.get("tag"));
            rules.add(new TagRule(tag, String.valueOf(t.getOrDefault("description", "标签授信: " + tag))));
        }
        return rules;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Map ? (Map<String, Object>) v : Map.of();
    }
}
```

- [ ] **Step 4: 运行测试通过**

```powershell
.\gradlew.bat :latchac-core:test
```

预期：全绿。

- [ ] **Step 5: Commit**

```powershell
git add latchac-core/src
git commit -m "feat(core): YAML 配置模型与解析器——失败即拒载保留旧配置"
```

---

### Task 7: core 引擎门面 LatchEngine + Outcome

**Files:**
- Create: `latchac-core/src/main/java/org/coffeepop/latchac/core/model/Outcome.java`
- Create: `latchac-core/src/main/java/org/coffeepop/latchac/core/LatchEngine.java`
- Test: `latchac-core/src/test/java/org/coffeepop/latchac/core/LatchEngineTest.java`

- [ ] **Step 1: 写失败测试**

创建 `LatchEngineTest.java`：

```java
package org.coffeepop.latchac.core;

import org.coffeepop.latchac.core.config.LatchConfig;
import org.coffeepop.latchac.core.exempt.ExemptionContext;
import org.coffeepop.latchac.core.exempt.RegionRule;
import org.coffeepop.latchac.core.exempt.TagRule;
import org.coffeepop.latchac.core.model.Outcome;
import org.coffeepop.latchac.core.model.Suspicion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatchEngineTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String CHECK = "movement.flight";

    private static LatchConfig config(double threshold) {
        return new LatchConfig(4000,
                Map.of(CHECK, new LatchConfig.CheckConfig(true, threshold, "LOG")),
                List.of(new RegionRule("hall", "world", -10, 0, -10, 10, 320, 10),
                        new TagRule("fake-player", "假人授信")));
    }

    private static Suspicion sus(double score, long ts) {
        return new Suspicion(CHECK, PLAYER, score, ts, Map.of());
    }

    private static ExemptionContext ctx(double x, Set<String> tags) {
        return new ExemptionContext(PLAYER, "Steve", "world", x, 64, 0, CHECK, tags);
    }

    @Test
    void 豁免区域内评分清零并给出原因() {
        LatchEngine engine = new LatchEngine(config(3.0));
        Outcome out = engine.submit(sus(1.0, 0), ctx(0, Set.of()));
        Outcome.Exempted exempted = assertInstanceOf(Outcome.Exempted.class, out);
        assertEquals("区域豁免: hall", exempted.reason());
    }

    @Test
    void 假人标签直接授信() {
        LatchEngine engine = new LatchEngine(config(3.0));
        Outcome out = engine.submit(sus(1.0, 0), ctx(999, Set.of("fake-player")));
        assertInstanceOf(Outcome.Exempted.class, out);
    }

    @Test
    void 区域外单次低分不翻转_持续提交翻转() {
        LatchEngine engine = new LatchEngine(config(3.0));
        Outcome first = engine.submit(sus(1.0, 0), ctx(999, Set.of()));
        assertFalse(assertInstanceOf(Outcome.Scored.class, first).verdict().tripped());

        engine.submit(sus(1.0, 100), ctx(999, Set.of()));
        engine.submit(sus(1.0, 200), ctx(999, Set.of()));
        Outcome fourth = engine.submit(sus(1.0, 300), ctx(999, Set.of()));
        assertTrue(assertInstanceOf(Outcome.Scored.class, fourth).verdict().tripped());
    }

    @Test
    void 未配置或禁用的检查项返回Disabled() {
        LatchEngine engine = new LatchEngine(config(3.0));
        Suspicion unknown = new Suspicion("combat.killaura", PLAYER, 0.5, 0, Map.of());
        ExemptionContext c = new ExemptionContext(PLAYER, "Steve", "world", 999, 64, 0,
                "combat.killaura", Set.of());
        assertInstanceOf(Outcome.Disabled.class, engine.submit(unknown, c));
    }

    @Test
    void 热重载原子替换且旧累计清零() {
        LatchEngine engine = new LatchEngine(config(3.0));
        engine.submit(sus(1.0, 0), ctx(999, Set.of()));
        engine.submit(sus(1.0, 100), ctx(999, Set.of()));

        engine.reload(config(10.0)); // 新纪元：阈值 10，累计清零

        Outcome after = engine.submit(sus(1.0, 200), ctx(999, Set.of()));
        Outcome.Scored scored = assertInstanceOf(Outcome.Scored.class, after);
        assertEquals(1.0, scored.verdict().aggregateScore(), 1e-9);
        assertFalse(scored.verdict().tripped());
    }
}
```

- [ ] **Step 2: 运行确认失败**

```powershell
.\gradlew.bat :latchac-core:test --tests "org.coffeepop.latchac.core.LatchEngineTest"
```

预期：FAILED，符号不存在。

- [ ] **Step 3: 实现 Outcome 与 LatchEngine**

创建 `Outcome.java`：

```java
package org.coffeepop.latchac.core.model;

/** 一次评分提交的完整结局。三种情形全部可日志审计——决策要能回答"为什么"。 */
public sealed interface Outcome {

    /** 命中豁免，评分清零丢弃。 */
    record Exempted(String reason) implements Outcome {}

    /** 进入锁存聚合，产生判定。 */
    record Scored(Verdict verdict) implements Outcome {}

    /** 检查项未配置或已禁用。 */
    record Disabled(String checkId) implements Outcome {}
}
```

创建 `LatchEngine.java`：

```java
package org.coffeepop.latchac.core;

import org.coffeepop.latchac.core.config.LatchConfig;
import org.coffeepop.latchac.core.exempt.ExemptionContext;
import org.coffeepop.latchac.core.exempt.ExemptionMatrix;
import org.coffeepop.latchac.core.latch.LatchAccumulator;
import org.coffeepop.latchac.core.model.Outcome;
import org.coffeepop.latchac.core.model.Suspicion;
import org.coffeepop.latchac.core.model.Verdict;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LatchAC 核心引擎（Arbiter 判定部分，平台无关）。
 * 流程：检查项开关 → 豁免矩阵（命中即清零）→ 锁存聚合 → 阈值判定。
 * 热重载：reload() 以"配置纪元"整体原子替换（volatile 引用），旧累计随旧纪元丢弃。
 * 线程安全：纪元不可变 + 累计器 ConcurrentHashMap + 实例锁，Folia 多区域线程并发投递安全。
 */
public final class LatchEngine {

    /** 一个配置纪元：配置 + 矩阵 + 该纪元的累计器。整体替换实现热重载。 */
    private record Epoch(LatchConfig config, ExemptionMatrix matrix,
                         ConcurrentHashMap<String, LatchAccumulator> accumulators) {

        static Epoch of(LatchConfig config) {
            return new Epoch(config, new ExemptionMatrix(config.exemptionRules()),
                    new ConcurrentHashMap<>());
        }
    }

    private volatile Epoch epoch;

    public LatchEngine(LatchConfig config) {
        this.epoch = Epoch.of(config);
    }

    /** 热重载：原子替换配置纪元。失败的配置根本到不了这里（解析期已拒绝）。 */
    public void reload(LatchConfig config) {
        this.epoch = Epoch.of(config);
    }

    /** 提交一次可疑评分，返回可审计的结局。 */
    public Outcome submit(Suspicion suspicion, ExemptionContext ctx) {
        Epoch e = this.epoch;

        LatchConfig.CheckConfig check = e.config().checks().get(suspicion.checkId());
        if (check == null || !check.enabled()) {
            return new Outcome.Disabled(suspicion.checkId());
        }

        Optional<String> exemption = e.matrix().firstExemption(ctx);
        if (exemption.isPresent()) {
            return new Outcome.Exempted(exemption.get());
        }

        String key = suspicion.playerId() + "|" + suspicion.checkId();
        LatchAccumulator acc = e.accumulators().computeIfAbsent(key,
                k -> new LatchAccumulator(check.tripThreshold(), e.config().halfLifeMillis()));

        double aggregate;
        synchronized (acc) {
            aggregate = acc.accumulate(suspicion.score(), suspicion.timestampMillis());
        }

        boolean tripped = aggregate >= check.tripThreshold();
        String reason = tripped
                ? "累计可疑度 %.2f 越过阈值 %.2f（锁存翻转）".formatted(aggregate, check.tripThreshold())
                : "累计中 %.2f / %.2f".formatted(aggregate, check.tripThreshold());
        return new Outcome.Scored(
                new Verdict(suspicion.playerId(), suspicion.checkId(), aggregate, tripped, reason));
    }
}
```

- [ ] **Step 4: 运行 core 全部测试通过**

```powershell
.\gradlew.bat :latchac-core:test
```

预期：全绿（5 个测试类共 21 例）。

- [ ] **Step 5: Commit**

```powershell
git add latchac-core/src
git commit -m "feat(core): LatchEngine 引擎门面——豁免清零→锁存聚合→可审计判定"
```

---

### Task 8: paper 假人识别 SPI

**Files:**
- Create: `latchac-paper/src/main/java/org/coffeepop/latchac/paper/fake/FakePlayerProvider.java`
- Create: `latchac-paper/src/main/java/org/coffeepop/latchac/paper/fake/HeuristicFakePlayerProvider.java`
- Create: `latchac-paper/src/main/java/org/coffeepop/latchac/paper/fake/FakePlayerRegistry.java`

（薄适配层，无单测，Task 10 冒烟验证。）

- [ ] **Step 1: 创建 SPI 接口 `FakePlayerProvider.java`**

```java
package org.coffeepop.latchac.paper.fake;

import org.bukkit.entity.Player;

/**
 * 假人识别 SPI。
 * 第三方插件可通过 LatchAntiCheat#fakePlayers().register(...) 注册自定义识别器，
 * 为私有假人方案提供授信通道（高可扩展性需求）。
 */
public interface FakePlayerProvider {

    /** 识别器名称，进入审计日志。 */
    String name();

    boolean isFakePlayer(Player player);
}
```

- [ ] **Step 2: 创建通用特征识别器 `HeuristicFakePlayerProvider.java`**

```java
package org.coffeepop.latchac.paper.fake;

import org.bukkit.entity.Player;

/**
 * 通用假人特征识别：不绑定任何具体假人插件版本（模糊匹配原则——行为特征优先于插件签名）。
 * 特征 1：假人没有真实网络连接（fakeplayer 类插件、Carpet 移植方案普遍如此）。
 * 特征 2：主流 NPC/假人插件的通用 metadata 标记。
 */
public final class HeuristicFakePlayerProvider implements FakePlayerProvider {

    @Override
    public String name() {
        return "heuristic";
    }

    @Override
    public boolean isFakePlayer(Player player) {
        if (player.getAddress() == null) {
            return true;
        }
        return player.hasMetadata("NPC") || player.hasMetadata("fakeplayer");
    }
}
```

- [ ] **Step 3: 创建注册表 `FakePlayerRegistry.java`**

```java
package org.coffeepop.latchac.paper.fake;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/** 假人识别器注册表。CopyOnWriteArrayList：注册极少、读取高频，Folia 多线程读安全。 */
public final class FakePlayerRegistry {

    private final List<FakePlayerProvider> providers = new CopyOnWriteArrayList<>();

    public void register(FakePlayerProvider provider) {
        providers.add(provider);
    }

    /** @return 命中的识别器名；非假人返回 empty */
    public Optional<String> identify(Player player) {
        for (FakePlayerProvider p : providers) {
            if (p.isFakePlayer(player)) {
                return Optional.of(p.name());
            }
        }
        return Optional.empty();
    }
}
```

- [ ] **Step 4: 编译验证**

```powershell
.\gradlew.bat :latchac-paper:compileJava
```

预期：`BUILD SUCCESSFUL`。

- [ ] **Step 5: Commit**

```powershell
git add latchac-paper/src
git commit -m "feat(paper): 假人识别 SPI 与通用特征识别器"
```

---

### Task 9: paper 插件接线——主类、移动传感器、reload 命令、默认配置

**Files:**
- Modify: `latchac-paper/src/main/java/org/coffeepop/latchac/paper/LatchAntiCheat.java`
- Create: `latchac-paper/src/main/java/org/coffeepop/latchac/paper/sensor/MovementSensor.java`
- Create: `latchac-paper/src/main/java/org/coffeepop/latchac/paper/command/LatchacCommand.java`
- Create: `latchac-paper/src/main/resources/config.yml`

- [ ] **Step 1: 创建默认配置 `config.yml`**

```yaml
# ============================================================
# LatchAC —— 零误伤生电友好反作弊
# 全部配置支持 /latchac reload 热重载；重载失败自动保留旧配置。
# M2 原型仅支持 action: LOG（静默日志），绝不回弹、绝不干预。
# ============================================================

latch:
  # 累计评分半衰期（毫秒）。越短对瞬时毛刺越宽容（锁存器抗抖动）。
  half-life-ms: 4000

checks:
  movement.flight:
    enabled: true
    # 累计可疑度越过该阈值才判定（单次事件最高 1.0，即至少需要持续证据）
    trip-threshold: 3.0
    action: LOG

exemptions:
  # 区域豁免：范围内行为完全免检（世界名 + AABB 两角坐标）
  regions: []
  #  - name: machine-hall
  #    world: world
  #    min: [-50, 0, -50]
  #    max: [50, 320, 50]

  # 玩家豁免名单
  players:
    names: []
    uuids: []

  # 标签授信：采集层自动打标（假人 = fake-player）
  trusted-tags:
    - tag: fake-player
      description: "假人授信（Carpet 类假人插件/NPC）"
```

- [ ] **Step 2: 重写主类 `LatchAntiCheat.java`**

```java
package org.coffeepop.latchac.paper;

import org.bukkit.command.PluginCommand;
import org.coffeepop.latchac.core.LatchEngine;
import org.coffeepop.latchac.core.config.ConfigParser;
import org.coffeepop.latchac.core.config.LatchConfig;
import org.coffeepop.latchac.paper.command.LatchacCommand;
import org.coffeepop.latchac.paper.fake.FakePlayerRegistry;
import org.coffeepop.latchac.paper.fake.HeuristicFakePlayerProvider;
import org.coffeepop.latchac.paper.sensor.MovementSensor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LatchAntiCheat extends JavaPlugin {

    private LatchEngine engine;
    private FakePlayerRegistry fakePlayers;

    @Override
    public void onEnable() {
        saveDefaultConfig(); // 首次运行落盘 config.yml

        this.engine = new LatchEngine(loadConfigOrBuiltin());
        this.fakePlayers = new FakePlayerRegistry();
        this.fakePlayers.register(new HeuristicFakePlayerProvider());

        getServer().getPluginManager().registerEvents(
                new MovementSensor(this, engine, fakePlayers), this);

        PluginCommand command = getCommand("latchac");
        if (command != null) {
            command.setExecutor(new LatchacCommand(this));
        }

        getLogger().info("LatchAC M2 原型已启用——记录模式，绝不回弹（生电即公理）");
    }

    /** 磁盘配置解析失败时回退 jar 内置默认，保证插件永远有可用配置。 */
    private LatchConfig loadConfigOrBuiltin() {
        Path file = getDataFolder().toPath().resolve("config.yml");
        try {
            return new ConfigParser().parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException ex) {
            getLogger().severe("config.yml 解析失败，回退内置默认配置: " + ex.getMessage());
            try (InputStream in = getResource("config.yml")) {
                if (in == null) {
                    throw new IllegalStateException("jar 内缺少默认 config.yml");
                }
                return new ConfigParser().parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("内置默认配置不可读", e);
            }
        }
    }

    /**
     * 热重载：解析成功才替换纪元；失败保留旧配置继续运行。
     * @return 是否重载成功
     */
    public boolean reloadLatchConfig() {
        Path file = getDataFolder().toPath().resolve("config.yml");
        try {
            engine.reload(new ConfigParser().parse(Files.readString(file, StandardCharsets.UTF_8)));
            getLogger().info("配置已热重载");
            return true;
        } catch (IOException | RuntimeException ex) {
            getLogger().severe("热重载失败，继续使用旧配置: " + ex.getMessage());
            return false;
        }
    }

    /** 供第三方扩展注册假人识别器（公开 API）。 */
    public FakePlayerRegistry fakePlayers() {
        return fakePlayers;
    }
}
```

- [ ] **Step 3: 创建 `LatchacCommand.java`**

```java
package org.coffeepop.latchac.paper.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.coffeepop.latchac.paper.LatchAntiCheat;

public final class LatchacCommand implements CommandExecutor {

    private final LatchAntiCheat plugin;

    public LatchacCommand(LatchAntiCheat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            boolean ok = plugin.reloadLatchConfig();
            sender.sendMessage(ok
                    ? "[LatchAC] 配置已热重载"
                    : "[LatchAC] 重载失败，已保留旧配置（详见控制台）");
            return true;
        }
        sender.sendMessage("[LatchAC] 用法: /" + label + " reload");
        return true;
    }
}
```

- [ ] **Step 4: 创建 `MovementSensor.java`**

```java
package org.coffeepop.latchac.paper.sensor;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.coffeepop.latchac.core.LatchEngine;
import org.coffeepop.latchac.core.exempt.ExemptionContext;
import org.coffeepop.latchac.core.model.Outcome;
import org.coffeepop.latchac.core.model.Suspicion;
import org.coffeepop.latchac.paper.LatchAntiCheat;
import org.coffeepop.latchac.paper.fake.FakePlayerRegistry;

import java.util.Map;
import java.util.Set;

/**
 * 移动传感器（M2 流水线演示）。
 * 职责：演示 Sensor → Engine → 决策日志 全链路；真实移动检测器在 M3 实现。
 * 演示规则：排除所有已知合法上升上下文后，对超过跳跃初速的上升提交低分 Suspicion。
 * 设计红线：MONITOR 优先级只读事件，绝不修改玩家位置/速度（默认无回弹）。
 */
public final class MovementSensor implements Listener {

    private static final String CHECK_ID = "movement.flight";
    /** 原版跳跃初速约 0.42 格/tick，低于此值的上升直接早退。 */
    private static final double VANILLA_JUMP_VELOCITY = 0.42;

    private final LatchAntiCheat plugin;
    private final LatchEngine engine;
    private final FakePlayerRegistry fakePlayers;

    public MovementSensor(LatchAntiCheat plugin, LatchEngine engine, FakePlayerRegistry fakePlayers) {
        this.plugin = plugin;
        this.engine = engine;
        this.fakePlayers = fakePlayers;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        double dy = event.getTo().getY() - event.getFrom().getY();

        // 廉价前置过滤：明显合法状态直接早退（性能法则：事件驱动 + 最短路径）
        if (dy <= VANILLA_JUMP_VELOCITY) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isGliding() || player.isFlying() || player.getAllowFlight()) {
            return;
        }
        if (player.isInsideVehicle() || player.isClimbing() || player.isSwimming()) {
            return;
        }
        if (player.hasPotionEffect(PotionEffectType.LEVITATION)
                || player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            return;
        }

        Set<String> tags = fakePlayers.identify(player).isPresent()
                ? Set.of("fake-player")
                : Set.of();

        ExemptionContext ctx = new ExemptionContext(
                player.getUniqueId(), player.getName(), player.getWorld().getName(),
                event.getTo().getX(), event.getTo().getY(), event.getTo().getZ(),
                CHECK_ID, tags);

        Suspicion suspicion = new Suspicion(CHECK_ID, player.getUniqueId(), 0.2,
                System.currentTimeMillis(), Map.of("dy", "%.3f".formatted(dy)));

        switch (engine.submit(suspicion, ctx)) {
            case Outcome.Exempted e -> plugin.getLogger().fine(
                    () -> "[豁免] %s -> %s".formatted(player.getName(), e.reason()));
            case Outcome.Scored s -> {
                if (s.verdict().tripped()) {
                    plugin.getLogger().warning(
                            "[LATCH] %s 触发 %s | %s | 证据 dy=%s | 仅记录，不干预".formatted(
                                    player.getName(), CHECK_ID, s.verdict().reason(),
                                    suspicion.evidence().get("dy")));
                }
            }
            case Outcome.Disabled d -> { /* 检查项关闭，无动作 */ }
        }
    }
}
```

- [ ] **Step 5: 构建验证**

```powershell
.\gradlew.bat build
```

预期：`BUILD SUCCESSFUL`（core 测试全绿 + paper 编译通过 + jar 产出）。

- [ ] **Step 6: Commit**

```powershell
git add latchac-paper/src
git commit -m "feat(paper): 插件接线——移动传感器流水线、热重载命令与默认配置"
```

---

### Task 10: 冒烟验证（run-paper 起真实 Paper 26.1.2）

**Files:**
- 可能修改: `latchac-paper/src/main/resources/plugin.yml`（仅当 api-version 验证失败）

- [ ] **Step 1: 启动内置测试服**

```powershell
.\gradlew.bat :latchac-paper:runServer
```

首次运行会自动下载 Paper 26.1.2（run-paper 插件负责，EULA 已在 jvmArgs 同意）。

预期日志顺序：
1. `Done (…s)! For help, type "help"`
2. `[LatchAC] LatchAC M2 原型已启用——记录模式，绝不回弹（生电即公理）`

若出现 **api-version 拒载错误**（如 `Unsupported API version 26.1`）：将 `plugin.yml` 中 `api-version: '26.1'` 改为报错信息提示的受支持值（候选顺序：`'26.1.2'` → `'26.2'` → `'1.21'`），重新 `runServer` 直到加载成功。

若出现 **SnakeYAML NoClassDefFoundError**（白皮书待验证项）：在 `latchac-paper/build.gradle.kts` 的 `jar` 任务中追加一行 `from(configurations.runtimeClasspath.get().filter { it.name.contains("snakeyaml") }.map { zipTree(it) })`，重新构建。

- [ ] **Step 2: 控制台验证热重载**

在服务器控制台输入：

```
latchac reload
```

预期输出：`[LatchAC] 配置已热重载`。

再制造一次失败重载验证"保留旧配置"语义：把 `run/plugins/LatchAC/config.yml` 首行改成 `latch: [broken`（非法 YAML），再次 `latchac reload`，预期输出 `[LatchAC] 重载失败，已保留旧配置（详见控制台）` 且插件继续正常运行。改回正确内容再 reload 恢复。

- [ ] **Step 3: 停服**

控制台输入 `stop`，正常关闭。

- [ ] **Step 4: （可选，有本地客户端时）游戏内验证**

用 26.1.2 客户端进入 `localhost:25565`：正常走跳（含跳跃）不应出现任何 `[LATCH]` 日志；`/gamemode creative` 起飞也不应触发（`isFlying`/`getAllowFlight` 早退）。此步无客户端可跳过——引擎判定逻辑已由 core 单测覆盖。

- [ ] **Step 5: Commit（含冒烟期间的任何修正）**

```powershell
git add -A
git commit -m "test: Paper 26.1.2 冒烟通过——启用/热重载/失败保留旧配置验证"
```

若冒烟零修正、无文件变更，则跳过本 commit。

- [ ] **Step 6: 更新白皮书销项**

在 `docs/whitepaper.md` 的"待验证清单"中，把已验证项打勾（SnakeYAML 内置情况、api-version 实际值），并如实记录结论。

```powershell
git add docs/whitepaper.md
git commit -m "docs: 白皮书销项——SnakeYAML 与 api-version 冒烟结论"
```

---

## 验收清单（M2 完成定义）

- [ ] `.\gradlew.bat build` 全绿：core 21 例单测通过，`LatchAC-0.0.1.jar` 产出
- [ ] Paper 26.1.2 实机加载成功，`folia-supported: true` 声明就位
- [ ] `/latchac reload` 热重载生效；非法配置重载被拒且旧配置存活
- [ ] 假人标签（`fake-player`）在引擎层走 TagRule 全量授信（LatchEngineTest 覆盖）
- [ ] 全代码库无任何修改玩家位置/速度的调用（红线审计：全局搜索 `setVelocity` 与 `teleport` 应零命中）
- [ ] git 历史分主题：骨架 / 多模块化 / 白皮书 / 模型 / 累计器 / 豁免 / 配置 / 引擎 / 假人 SPI / 接线 / 冒烟

## 后续计划入口（不在本计划范围）

- **M2 接口规范文档**：原型期对外接口（`FakePlayerProvider` SPI、`LatchEngine` 公开 API）以 javadoc 为准；独立接口规范文档并入 M6 文档里程碑，避免接口未稳定就写死文档。
- **M3 计划**：四大检测器（移动检测器含 15 格红石弹射识别、战斗可达性、Tweakeroo/Litematica 模式匹配、X-Ray 慢累计）+ Paper 机制开关感知 + MockBukkit 评估 + 响应链扩展（NOTIFY/KICK/BAN + 证据快照）
- **M4 计划**：Folia 实机验证、区域快照缓存、压测报告
