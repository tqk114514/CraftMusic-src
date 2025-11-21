# CraftMusic 版本管理

## 分支模型

**一个 Minecraft 版本一个长期分支**，mod 版本用 tag 标记。

| 分支 | Minecraft | NeoForge | Parchment 映射 |
| --- | --- | --- | --- |
| `mc-1.21.1` | 1.21.1 | 21.1.192 | 2024.11.17 |
| `mc-1.21.2` | 1.21.2 | 21.2.1-beta | 无（用 NeoForge 官方映射） |
| `mc-1.21.3` | 1.21.3 | 21.3.94 | 2024.12.07 |
| `mc-1.21.4` | 1.21.4 | 21.4.155 | 2025.03.23 |
| `mc-1.21.5` | 1.21.5 | 21.5.95 | 2025.06.15 |

之所以按 MC 版本切分支：同一个 mod 版本在不同 MC 版本之间只有几十行差异（NeoForge API
适配 + 少量功能降级），而 mod 版本升级动辄上万行。按 MC 版本建分支，升级 mod 时只需在
每个分支上各提交一次，port 时只需 cherry-pick。

## 标签命名

`v<mod版本>-mc<MC版本>`，例如：

- `v1.0.0-mc1.21.1` … `v1.0.0-mc1.21.5`
- `v1.1.0-mc1.21.1` … `v1.1.0-mc1.21.5`

每个分支上的提交历史就是：`v1.0.0` → `v1.1.0` → …

## 开发流程

### 1. 在主开发版本上开发

默认在 `mc-1.21.1` 上开发新功能：

```bash
git checkout mc-1.21.1
# ... 开发
git commit -m "feat: 新功能"
```

### 2. 移植到其他 MC 版本

```bash
git checkout mc-1.21.5
git cherry-pick <commit-hash>
# 解决冲突（通常是 NeoForge API 差异）
```

### 3. 发布

在**每个**分支上打同一个 mod 版本的 tag：

```bash
for mc in 1.21.1 1.21.2 1.21.3 1.21.4 1.21.5; do
  git checkout mc-$mc
  git tag -a v1.2.0-mc$mc -m "CraftMusic 1.2.0 for Minecraft $mc"
done
git push --tags
```

发布前记得同步 `gradle.properties` 里的 `mod_version`。

### 4. 新增 Minecraft 版本支持

```bash
git checkout -b mc-1.21.6 mc-1.21.5   # 从最接近的分支开
# 修改 gradle.properties：minecraft_version / minecraft_version_range / neo_version
./gradlew build
```

## 构建

```bash
./gradlew build        # 构建当前分支版本
./gradlew clean build
./gradlew runClient    # 启动客户端
```

产物命名：`craftmusic-<mod版本>-neoforge<MC版本>-<NeoForge版本>.jar`

## 已知差异

- **Minecraft 1.21.2**：Parchment 没有该版本的映射，该分支不启用 parchment；另外
  `RegisterKeyMappingsEvent` 的注册方式不同（走 mod bus 而非 `@SubscribeEvent`），
  且频谱可视化在该版本被禁用。
- **1.0.0 及更早**：`native/`（miniaudio C 源码）尚未包含频谱 FFT 部分，因此
  v1.0.0 的提交里没有 `native/` 目录。

## 历史说明

v1.0.0 与 v1.1.0 的历史原本只保存在开发者本地，未纳入版本控制。2026-09-16 从本地备份
恢复并补齐到本仓库，各提交的日期取自对应构建产物的生成时间。
