# CraftMusic 版本管理指南

## 分支結構

- `main` - 主開發分支（跟隨最新 Minecraft 版本）
- `1.21.x` - Minecraft 1.21.x 系列維護分支
- `1.20.x` - Minecraft 1.20.x 系列維護分支

## 快速切換版本

### Windows
運行 `switch-version.bat` 腳本選擇版本

### 手動切換
```bash
# 切換到 1.21.x
git checkout 1.21.x
copy gradle.properties.1.21.1 gradle.properties

# 切換到 1.20.x  
git checkout 1.20.x
copy gradle.properties.1.20.4 gradle.properties
```

## 開發流程

### 1. 新功能開發
```bash
# 在主分支開發
git checkout main
# ... 開發新功能
git add .
git commit -m "feat: new feature"
```

### 2. 向後移植（Backport）
```bash
# 移植到 1.20.x
git checkout 1.20.x
git cherry-pick <commit-hash>
# 解決衝突（如果有）
# 測試
./gradlew runClient
```

### 3. 發布新版本
```bash
# 為每個分支打標籤
git checkout 1.21.x
git tag v1.1.0-mc1.21.1

git checkout 1.20.x
git tag v1.1.0-mc1.20.4

# 推送標籤
git push --tags
```

## 版本兼容性注意事項

### Minecraft 1.21.x → 1.20.x 主要差異：
- NeoForge API 版本不同
- 某些方法簽名可能變化
- 資源包格式版本不同

### 測試檢查清單
- [ ] 編譯通過
- [ ] 遊戲啟動正常
- [ ] UI 顯示正確
- [ ] 音頻播放正常
- [ ] 歌詞同步正確
- [ ] 所有語言文件加載

## 構建命令
```bash
# 構建當前分支版本
./gradlew build

# 清理並重新構建
./gradlew clean build

# 運行客戶端測試
./gradlew runClient
```

## 發布文件命名
- `CraftMusic-1.1.0-mc1.21.1.jar`
- `CraftMusic-1.1.0-mc1.20.4.jar`
