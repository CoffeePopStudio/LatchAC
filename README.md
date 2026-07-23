# Latch AntiCheat

轻量级 Minecraft 反作弊插件，基于自研算法检测作弊行为。

## 架构

```
latchac-core/     # 平台无关核心 — 检测引擎、基线分析、VL系统
latchac-paper/    # Paper 平台适配 — 数据包拦截、命令、配置
```

**核心组件：**
- **Prediction Engine** — 基于原版物理公式预测合法移动范围
- **Baseline Profiler** — 双层行为基线（群体 + 个体），SQLite 持久化
- **Stage Pipeline** — 四阶段检查流水线

## 检测列表

| 检查 | 类型 | 方法 |
|------|------|------|
| FlyVertical | Movement | 个体基线 |
| FlyAirStuck | Movement | 物理预测 |
| FlyGroundSpoof | Movement | 物理预测 |
| Speed | Movement | 个体基线 |
| KillAura | Combat | 个体基线偏离 |
| Velocity | Combat | 物理推导 |
| Timer | Exploit | 个体基线偏离 |
| InvMove | Inventory | 容器状态 |

## 构建

```bash
./gradlew build
# 输出: latchac-paper/build/libs/latchac-paper.jar
```

## 配置

```yaml
# latchac-paper/run/plugins/LatchAC/config.yml
debug: false
alert-threshold: 20
punish-threshold: 50

baseline:
  source: local
  training-mode: false
  admin-whitelist: []
```

## 命令

- `/latchac vl <player>` — 查看 VL
- `/latchac check list|enable|disable` — 管理检查
- `/latchac debug` — 切换 debug
- `/latchac baseline export|import|status` — 社区基线管理

## 训练基线

```yaml
baseline:
  training-mode: true   # 开启训练模式（不涨VL 不处罚 不回弹）
```

正常游玩一段时间后关掉。个体基线自动持久化到 SQLite，群体基线可导出供社区共享。

## 许可证

GNU General Public License v3.0
