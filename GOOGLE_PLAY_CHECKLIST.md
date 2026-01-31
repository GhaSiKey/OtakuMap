# Google Play 上架准备清单

## 项目信息
- **应用名称**: 圣地巡礼 (OtakuMap)
- **包名**: com.gaoshiqi.otakumap
- **当前版本**: versionCode=3, versionName="1.2.0"
- **最低 SDK**: 24 (Android 7.0)
- **目标 SDK**: 35 (Android 15)

---

## P0 - 必须完成 (Google Play 强制要求)

### 1. 隐私政策
- [x] 创建隐私政策网页/页面
- [x] 在设置页面添加隐私政策入口
- [ ] 在 Play Console 填写隐私政策 URL
- **隐私政策 URL**: https://github.com/GhaSiKey/BangumiAPP/blob/main/docs/PRIVACY_POLICY_EN.md
- **相关文件**:
  - `docs/PRIVACY_POLICY.md` (中文)
  - `docs/PRIVACY_POLICY_EN.md` (英文)
  - `app/src/main/java/com/gaoshiqi/otakumap/homepage/SettingsFragment.kt`

### 2. 数据安全声明
- [ ] 在 Play Console 填写数据安全表单
- [ ] 声明收集的数据类型:
  - 位置信息 (地图功能)
  - 本地存储数据 (收藏)
- [ ] 声明第三方 SDK 数据收集 (Google Maps)

### 3. 签名配置
- [ ] 生成 Release 签名密钥 (keystore)
- [ ] 在 `app/build.gradle.kts` 配置 signingConfigs
- [ ] 启用 Google Play App Signing
- **相关文件**: `app/build.gradle.kts`

### 4. 敏感数据安全
- [ ] 将 Bearer Token 从代码中移除，改用 BuildConfig 或后端代理
- **问题位置**: `app/src/main/java/com/gaoshiqi/otakumap/data/api/NextClient.kt:16`
- **当前代码**: `private const val bearerToken = "YjkgiGyCUlS6XAXetMUrCulh6Q7yuW3rI8Y4SEOG"`

---

## P1 - 强烈建议 (提升审核通过率)

### 5. 网络安全配置
- [ ] 关闭明文流量: `android:usesCleartextTraffic="false"`
- [ ] 确认所有 API 使用 HTTPS (已确认)
- **相关文件**: `app/src/main/AndroidManifest.xml:17`

### 6. 代码混淆
- [ ] 启用 R8 混淆: `isMinifyEnabled = true`
- [ ] 配置 ProGuard 规则 (Retrofit, Glide, Room, Google Maps)
- **相关文件**:
  - `app/build.gradle.kts`
  - `app/proguard-rules.pro`

### 7. 版本号同步
- [ ] 更新 versionCode 和 versionName (Git 提交显示 1.3.0)
- **相关文件**: `app/build.gradle.kts:19-20`

---

## P2 - 建议完成 (提升应用质量)

### 8. 崩溃报告工具
- [ ] 集成 Firebase Crashlytics
- [ ] 配置 Firebase 项目
- [ ] 添加崩溃上报初始化代码

### 9. 应用内更新
- [ ] 集成 Google Play In-App Updates API
- [ ] 或自建版本检查接口

### 10. 数据备份规则
- [ ] 配置 `backup_rules.xml`
- [ ] 配置 `data_extraction_rules.xml`
- **相关文件**:
  - `app/src/main/res/xml/backup_rules.xml`
  - `app/src/main/res/xml/data_extraction_rules.xml`

---

## P3 - 可选优化

### 11. 多语言支持
- [ ] 添加英文资源 `values-en/strings.xml`
- [ ] 添加日文资源 `values-ja/strings.xml`

### 12. 大屏/平板适配
- [ ] 添加平板布局 `layout-sw600dp/`
- [ ] 测试横屏显示

### 13. 无障碍支持
- [ ] 为图片添加 contentDescription
- [ ] 确保触摸目标足够大 (48dp)

---

## 应用商店素材准备

### 必需素材
- [ ] 应用图标 512x512 PNG
- [ ] 功能图片 1024x500 PNG/JPG
- [ ] 手机截图 (至少 2 张, 建议 4-8 张)
- [ ] 应用简短描述 (80 字以内)
- [ ] 应用完整描述

### 可选素材
- [ ] 平板截图
- [ ] 宣传视频 (YouTube)

---

## 关键文件路径

| 文件 | 路径 |
|------|------|
| 主配置 | `app/build.gradle.kts` |
| AndroidManifest | `app/src/main/AndroidManifest.xml` |
| 设置页面 | `app/src/main/java/com/gaoshiqi/otakumap/homepage/SettingsFragment.kt` |
| ProGuard 规则 | `app/proguard-rules.pro` |
| 备份规则 | `app/src/main/res/xml/backup_rules.xml` |
| 数据提取规则 | `app/src/main/res/xml/data_extraction_rules.xml` |
| NextClient | `app/src/main/java/com/gaoshiqi/otakumap/data/api/NextClient.kt` |
| 字符串资源 | `app/src/main/res/values/strings.xml` |

---

## 当前发现的安全问题

| 问题 | 严重程度 | 位置 |
|------|---------|------|
| Bearer Token 硬编码 | 高 | `NextClient.kt:16` |
| 允许 HTTP 明文流量 | 高 | `AndroidManifest.xml:17` |
| 代码未混淆 | 中 | `build.gradle.kts` |
| 备份规则未配置 | 低 | `backup_rules.xml` |

---

## 使用的第三方 SDK (需在数据安全声明中披露)

| SDK | 版本 | 用途 |
|-----|------|------|
| Google Play Services Maps | 19.2.0 | 地图显示 |
| Retrofit | 2.11.0 | 网络请求 |
| Glide | 4.16.0 | 图片加载 |
| Room | 2.7.2 | 本地数据库 |

---

*文档生成日期: 2025-12-06*