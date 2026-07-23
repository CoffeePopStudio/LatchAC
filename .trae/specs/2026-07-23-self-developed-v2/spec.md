# LatchAC 自研算法架构 V2 Spec

## 概述

基于第一性原理（Minecraft 原版物理 + 统计异常检测），完全重新设计全部 8 个检查。核心理念：不分析作弊器实现，只从运动方程和行为基线出发推导合法边界。

## 三大新组件

### 1. Prediction Engine（移动预测引擎）

所有移动类检查的统一底层。基于最新 tick 状态，用 wiki 验证的原版物理公式预测下帧合法运动范围。

**物理常量（来源：minecraft.wiki + 原版 Entity.move 源码）：**

| 常量 | 值 | 来源 |
|------|-----|------|
| movement_speed 默认值 | 0.7 | 属性/速度 |
| 地面速度公式 | `0.21168 × a × (1 − 0.91 × f) × f³` m/tick | 属性/速度 |
| 疾跑倍率 | 1.3 | Sprint 源码 |
| 潜行倍率 | 0.3 | Sneak 源码 |
| 跳跃初速度 | 0.42 blocks/tick | LivingEntity.jump |
| 重力加速度 | −0.08 blocks/tick² | Entity.move |
| 水平空气阻力 | ×0.91 / tick | Entity.move |
| 垂直空气阻力 | ×0.98 / tick | Entity.move |
| 地面摩擦 (草地) | 0.6 | 阻力系数 |

**输出 `MotionBounds`：**
```java
record MotionBounds(
    double maxDx, double maxDy, double maxDz,
    double expectedY,
    boolean groundedByPhysics
) {}
```

**计算流程：**
1. 获取脚下方块 slipperiness（玩家 Bukkit Location）
2. 查表获取 potion 倍率
3. 计算水平合法上限：`lastSpeed × friction + acceleration × strafeFactor`
4. 计算垂直预测：跳跃初速度 vs 重力
5. 推导 grounded：Y 是否在合法地面层级且 lastDy ≈ 0

### 2. Baseline Profiler（行为基线分析器）

**双层模型：**

```
Layer 1: Population Baseline（群体基线）
- 来源：硬编码种子 → 管理员训练 → 线上自适应（三层累积）
- 用于：新玩家冷启动防作弊、无条件备份判定

Layer 2: Individual Baseline（个体基线）
- 来源：EWMA 持续更新
- 用于：精准检测个体行为突变
```

**追踪指标：**

| 指标 | 用途 |
|------|------|
| rotationVariance | KillAura 旋转平滑度 |
| rotationSpeed | KillAura 角速度突变 |
| clickIntervalCV | AutoClicker 点击规律性 |
| swingAttackDelay | KillAura 挥臂同步 |
| packetIntervalMean | Timer 包间隔 |
| packetIntervalStd | Timer 包抖动 |
| moveRhythm | Speed 移动模式 |

**更新算法（EWMA α=0.05）：**
```
baseline = α × current + (1−α) × baseline
baselineStd = α × |current − baseline| + (1−α) × baselineStd
```

**判定：** zScore = |current − baseline| / baselineStd > 3.0 → 偏离自基线 3σ

### 3. Stage Pipeline（分阶段流水线）

```
Stage 0 [Pre-filter]: hasPosition / inVehicle / inLiquid / shouldExempt
Stage 1 [Physics]:  Prediction Engine → Fly checks + Speed
Stage 2 [Combat]:   Baseline Profiler → KillAura + Velocity
                    Packet timing → Timer
                    Container state → InvMove
Stage 3 [Post]:     VL 累加、跨检查关联打分
```

## 群体基线积累

**三层来源：**
- Layer 0: 硬编码种子（数学推导的合理范围），部署即生效
- Layer 1: 管理员种子 — `config.yml` 中 `training-mode: true` 时正常游玩数据入池
- Layer 2: 线上自适应 — 满足以下全部条件的玩家数据入池

**入池门控（防挂狗污染）：**

```
条件 1: 总 VL < 3
条件 2: 在线时长 > 2 小时
条件 3: 跨至少 2 次登录会话
条件 4: 30 分钟滑动窗口内各指标 CV < 0.3（行为自然方差）
  OR: 管理员白名单（config.yml 手动加 ID 直接入池）
```

**持久化：** SQLite 存储个体基线。群体基线每 24h 从 SQLite 聚合重算。

---

## 8 个检查设计

### Type: MOVEMENT

#### FlyVertical
- Prediction Engine 预测下帧合法的 `dy` 范围
- 实际 `deltaY` 超出范围且非跳跃帧 → flag
- 液体中豁免（`isInLiquid()`）

#### FlyAirStuck
- Prediction Engine 推导 `groundedByPhysics = false`（物理上不应在地面）
- 连续 15 tick 预测垂直位移接近 0（`|predictedDy| < 0.005`）
- 实际 `onGround = false`
- → flag（空中悬停）

#### FlyGroundSpoof
- 实际 `onGround = true`
- Prediction Engine 推导 `groundedByPhysics = false`
- → flag（onGround 伪造）

#### Speed
- Prediction Engine 输出 `MotionBounds`
- 实际 `deltaXZ > bounds.maxDx × 1.05`（5% 浮点容差）→ flag
- 实际 `deltaY < bounds.minDy` 且非爆炸/活塞外力 → flag
- `strafe分量 / forward分量 > 1.2` → flag
- BHop 检测：连续 5 次跳跃间隔 ±1 tick → flag

### Type: COMBAT

#### KillAura
三项 Baseline Profiler 独立指标：

| 子检查 | Baseline 指标 | 异常条件 |
|--------|--------------|----------|
| Rotation Smoothing | rotationVariance | 当前方差 < 自基线 × 0.3 |
| Click Regularity | clickIntervalCV | 当前 CV < 自基线 × 0.3 |
| Swing-ATK Sync | swingAttackDelay | 延迟 < 自基线 × 0.2 或无 swing |

三项权重：单指示 → VL+1，双指示 → VL+3，三指示 → VL+5

#### Velocity
- 收到 velocity 包后 8 tick 追踪实际位移
- Prediction Engine 计算"无击退时预期位移"（纯摩擦衰减）
- 差值 < 0.1 → 击退被消除 → flag
- 差值方向与预期击退相反 → flag（Reversal）
- 10 次击退中 8 次在第 1-2 tick 精准跳跃 → flag（自动 JumpReset）

### Type: EXPLOIT

#### Timer
- Baseline Profiler `packetIntervalMean` 指标
- 当前窗口均值 / 个体基线 < 0.8（包加速 >20%）→ flag
- CUSUM 个人化阈值：`h = 3 × personalStd`

### Type: INVENTORY

#### InvMove
- `isInContainer() == true` 且任一移动包到达 → flag
- 5 秒 flag 冷却
- 容器追踪：Bukkit `InventoryOpenEvent` / `InventoryCloseEvent`，用 `InventoryType` 过滤

---

## 文件结构（目标）

```
latchac-core/src/main/java/org/coffeepop/latchac/core/
├── engine/
│   ├── PredictionEngine.java      # 移动预测引擎
│   ├── MotionBounds.java           # 预测输出 record
│   └── PhysicsConstants.java       # 物理常量
├── baseline/
│   ├── BaselineProfiler.java       # 双层基线模型
│   ├── BaselineMetric.java         # 指标枚举 + 序列化
│   ├── BaselineStorage.java        # SQLite 持久化
│   └── PopulationBaseline.java     # 群体基线管理
├── pipeline/
│   └── StagePipeline.java          # 三阶段流水线
├── check/impl/
│   ├── movement/ (4 files)
│   ├── combat/ (2 files)
│   ├── exploit/ (1 file)
│   └── inventory/ (1 file)
```

## 实施顺序

1. Prediction Engine（移动类依赖）
2. Baseline Profiler + SQLite 存储（战斗类依赖）
3. Stage Pipeline
4. 移动类 4 个 Check
5. 战斗/其他 4 个 Check

## 与 V1 的关键区别

| | V1 | V2 |
|---|-----|-----|
| 信息来源 | 分析作弊器代码推测绕过手段 | 纯数学推导 + wiki 物理公式 |
| 检测逻辑 | 固定阈值 + 手动调参 | 物理预测 + 个体基线偏离 |
| 阈值 | 魔法数字（0.04, 2.0, 0.85...） | 物理常量 + zScore > 3σ |
| 基线 | 无 | 双层（群体 + 个体）+ SQLite |
| 架构 | Check 独立计算 | Prediction Engine + Profiler 统一提供 |
