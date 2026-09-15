# CraftMusic 版本管理

## 分支模型

**一个 Minecraft 版本一个长期分支**，mod 版本用 tag 标记。

| 分支 | Minecraft | NeoForge | 状态 |
| --- | --- | --- | --- |
| `mc-26.1.2` | 26.1.2 | 26.1.2.100+ | **Active** —— 主开发分支，GitHub 默认分支 |
| `mc-1.21.1` | 1.21.1 | 21.1.200+ | Maintenance —— 仅修崩溃，不加功能 |
| `mc-1.21.2` | 1.21.2 | 21.2.1-beta | Frozen（已 `lock_branch`，只读） |
| `mc-1.21.3` | 1.21.3 | 21.3.94 | Frozen |
| `mc-1.21.4` | 1.21.4 | 21.4.155 | Frozen |
| `mc-1.21.5` | 1.21.5 | 21.5.95 | Frozen |

状态含义：

- **Active**：持续开发新功能
- **Maintenance**：只修崩溃，版本号冻结在最后一个 bugfix，**不为旧版本 backport 新功能**
- **Frozen**：不再处理 issue；GitHub 上已设为只读锁，防止误推

之所以按 MC 版本切分支：同一个 mod 版本在不同 MC 版本之间只有几十行差异（NeoForge API
适配 + 少量功能降级），而 mod 版本升级动辄上万行。

## 标签命名

`v<mod版本>-mc<MC版本>` —— 注意 MC 版本**不带连字符**，与分支名的 `mc-<版本>` 相区分。

- `v1.0.0-mc1.21.1` … `v1.0.0-mc1.21.5`
- `v1.1.0-mc1.21.1` … `v1.1.0-mc1.21.5`
- `v1.2.0-mc1.21.1`、`v1.2.0-mc26.1.2`

**版本数字在不同 MC 版本间不要求一致。** 玩家按自己的 MC 版本找对应的最新版，
不会跨版本比较。真正要避免的是"旧版本没标注停更"，所以上面那张状态表是唯一事实源。

## 开发流程

### 1. 在主开发分支上开发

```bash
git checkout mc-26.1.2
# ... 开发
git commit -m "feat: 新功能"
```

### 2. 移植到旧版本

只对 Maintenance 分支做，Frozen 分支不再接受任何改动。

```bash
git checkout mc-1.21.1
git cherry-pick <commit-hash>
```

### 3. 发布

改 `gradle.properties` 的 `mod_version`，然后：

```bash
git tag -a v<版本>-mc<MC版本> -m "..."
git push origin <分支> && git push origin v<版本>-mc<MC版本>
```

## 构建

| 分支 | JDK | Gradle |
| --- | --- | --- |
| `mc-26.1.2` | **25** | 9.1.0（ModDevGradle 2.0.141） |
| `mc-1.21.x` | **21** | 8.14.3（ModDevGradle 2.0.107） |

本机默认 JDK 是 25，所以**构建 1.21.x 分支前必须先切 JDK**，否则 Gradle 8.14 会以
`Unsupported class file major version 69` 直接失败：

```bash
./gradlew --stop                                    # daemon 会沿用旧 JVM，不能省
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"
./gradlew build
```

构建 26.1.2 时用 JDK 25（默认即是）：

```bash
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-25.0.4.7-hotspot"
./gradlew build
```

## 版本号写法（26.x 起）

新版本体系是 `<年>.<release>.<hotfix>`，**三段**：

```
neo_version = 26.1.2.100
               └─┬┘ │   └── NeoForge 构建号
                 │  └────── hotfix（26.1 的第 2 个补丁）
                 └───────── 2026 年第 1 个 release
```

- NeoForge 版本号的前三位就是 MC 版本 → **MC 实际版本是 `26.1.2`，不是 `26.1`**
- 写错时**编译照样通过**（ModDevGradle 从 `neo_version` 推断 MC 版本），
  只在启动游戏时报错：`Mod craftmusic requires minecraft 26.1 / Currently, minecraft is 26.1.2`
- MC 26.1.0 / 26.1.1 的 NeoForge **从未 stable**（全是 `-beta`），26.1.2 是唯一有稳定
  加载器的版本，因此 `[26.1.2]` 精确匹配不会漏掉真实用户

## 已知差异

- **26.1.2**：官方已移除混淆，Parchment 不再需要（Mojang 参数名直接可用）。
  GUI 层改动较大，见下节。
- **1.21.2**：Parchment 无该版本映射；`RegisterKeyMappingsEvent` 注册方式不同
  （走 mod bus 而非 `@SubscribeEvent`）；频谱可视化在该版本被禁用。
- **1.0.0 及更早**：`native/` 尚无频谱 FFT 代码，因此 v1.0.0 的提交里没有 `native/`。

### 26.1 迁移要点

| 1.21.x | 26.1 |
| --- | --- |
| `GuiGraphics` | `GuiGraphicsExtractor` |
| `Screen#render(...)` | `extractRenderState(...)` |
| `renderBackground(...)` | `extractBackground(...)`（**框架已调用，子类不要重复调**） |
| `gfx.drawString(...)` | `gfx.text(...)` |
| `pose.pushPose / popPose` | `pushMatrix / popMatrix`（`pose()` 返回 `Matrix3x2fStack`） |
| `mouseClicked(x, y, button)` | `mouseClicked(MouseButtonEvent, boolean)` |
| `ObjectSelectionList.getEntry(i)` | `children().get(i)` |
| 条目 `render(gfx, index, y, x, w, h, …)` | `extractContent(gfx, mx, my, hovered, pt)`，位置取 `getX()/getY()` |
| `player.displayClientMessage(c, bool)` | `player.sendSystemMessage(c)` |
| `net.minecraft.Util` | `net.minecraft.util.Util` |
| `KeyMapping(...)` 第 4 参 `String` | `KeyMapping.Category` |

两个**只在运行期暴露**（编译查不出）的坑：

1. **颜色字面量必须写满 8 位 ARGB。** 1.21 的 `drawString` 会把 alpha 补成 `FF`，
   26.1 不再补 —— 写 `0xFFFFFF`（alpha = 00）的文字会完全透明。
2. **`extractRenderStateWithTooltipAndSubtitles` 是 final**，内部已经调用过
   `extractBackground`，子类再调一次会抛 `Can only blur once per frame`。

## 历史说明

v1.0.0 与 v1.1.0 的历史原本只保存在开发者本地，未纳入版本控制。2026-09-16 从本地备份
恢复并补齐到本仓库。这些历史提交由备份重建，**日期取自构建产物的时间戳，与真实发布时间
可能有偏差；真实发布日期以 `CHANGELOG.md` 为准**。
