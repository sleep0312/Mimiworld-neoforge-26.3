# 米米世界 (MimiWorld)

> 以「米」为主题的 Minecraft NeoForge 模组：完整的稻米产业链、9 级装备科技树、2 个独立维度与 8 个 Boss。

A rice-themed Minecraft NeoForge mod featuring a rice industry chain, a 9-tier gear progression, 2 dimensions and 8 bosses.

> **项目状态：测试版（Beta）**
>
> 本模组仍处于开发测试阶段：部分功能尚未完成或仍在调整，数值平衡、配方与掉落可能随时变动；纹理与模型大多为初版，尚未进行精细化打磨。
>
> 当前版本不建议用于长期存档或正式服务器。遇到问题或有改进建议，欢迎通过 GitHub Issues 反馈。

## 基本信息

| 项目      | 内容                                                                                                                                                              |
| --------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 模组 ID   | `mimiworld`                                                                                                                                                     |
| 版本      | v1.0.4-beta                                                                                                                                                       |
| Minecraft | 26.3                                                                                                                                                              |
| NeoForge  | 26.3.0.103+                                                                                                                                                       |
| Java      | 25                                                                                                                                                                |
| 必需前置  | [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) 5.5.7+、[Curios API](https://www.curseforge.com/minecraft/mc-mods/curios) 17.0+                   |
| 可选兼容  | [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) 31.17+（机器配方查看）、[Jade](https://www.curseforge.com/minecraft/mc-mods/jade) 26.3+（效果等级数字显示） |
| 侧别      | 客户端与服务端均需安装                                                                                                                                            |

## 内容概览

- **农业与材料**：米树、米草、米矿与米米平原；从「米 → 熟米 → 米团」的加工链，到饭晶 → 饭晶合金 → 超重结晶 → 复合米饭 → 清洁复合 → 超懒合金 → 最懒天堕 → 残堕的 9 级装备材料科技树。
- **机器与烹饪**：造米台、锤米台、沉淀台、搅碎器、搅拌器、大煮锅、巨无霸大大大煮锅、铁锅、烤箱、凝锭台，共 10 台工作方块、52 条加工配方；炉灶作为免燃料热源。
- **生物与 Boss**：44 种实体、38 枚生物蛋。8 个 Boss：米米猴王、饭晶虎王、超晶牛王、重晶兔王、复晶天青蟒、清洁长命夔、最懒终末龙、第八宗罪龙；另有残堕界精英与米米宝藏固定怪物。
- **维度与结构**：米米维度（米米平原）与残堕界（残堕荒原）两个维度；米米宝藏结构含 10 种固定怪物变体及其对应宝箱。
- **战斗与饰品**：自定义伤害类型、药水效果与箭矢；Curios 圣遗物槽位与神遗之书槽位；可放置的 Boss 头颅摆件。
- **模型与动画**：63 个 GeckoLib 模型、45 个动画文件，均由 Blockbench 制作。
- **配方查看**：安装 JEI 后可按 10 个自定义分类查看各机器的全部配方与催化剂。

## 移植状态

本仓库是 1.20.1 Forge 版本在 NeoForge 26.3 上的完整移植版：

- 物品、方块、实体、方块实体、菜单、流体、音效、药水效果与附魔注册项已对齐；
- 配方、战利品表、进度、标签、世界生成、维度与结构数据已迁移；
- 机器加工配方、JEI 集成、Jade 效果等级显示与剪刀采集判定已完成适配；
- 旧版中未注册的死代码与空动画文件不再保留。

## 安装

1. 安装 Minecraft 26.3 与 NeoForge 26.3.0.103+。
2. 将 **GeckoLib**、**Curios API** 与本模组 `mimiworld-<版本>.jar` 一同放入 `.minecraft/mods`。
3. 启动游戏；联机服务器的 `mods` 目录同样需要这三个 jar。
4. JEI 与 Jade 为可选兼容模组，不安装不影响本模组正常运行。

## 开发

- 环境：JDK 25 + Gradle（项目自带 Wrapper）。
- 构建：`gradlew build`；运行开发客户端：`gradlew runClient`；运行开发服务端：`gradlew runServer`。
- 数据生成：`gradlew runData`，输出到 `src/generated/resources`。
- 资源目录：`src/main/resources/assets/mimiworld/`；其中物品定义位于 `items/`，GeckoLib 模型与动画位于 `geckolib/`。
- 机器配方在 `ModMachineRecipes` 中统一注册，JEI 分类由 `jei/MimiJeiPlugin` 反射加载。

## 许可证

本项目采用分许可策略：

| 内容                                           | 许可                | 文件                            |
| ---------------------------------------------- | ------------------- | ------------------------------- |
| 源代码（Java、Gradle 脚本等）                  | **MIT**       | [LICENSE](LICENSE)               |
| 美术与音频资源（贴图、模型、动画、语言文件等） | **CC BY 4.0** | [LICENSE-ASSETS](LICENSE-ASSETS) |

使用本模组资源时，请按 CC BY 4.0 要求署名：**MimiWorld（米米世界）**。
