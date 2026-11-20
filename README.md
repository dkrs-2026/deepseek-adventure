# DeepSeek 冒险（Android 个人自用）

直连 `api.deepseek.com` 的文字互动冒险客户端。Kotlin + Jetpack Compose，支持流式输出、超长上下文、消息编辑/重试/分支、余额与本地用量估算。

> 仅个人侧载使用，不上架应用商店。API Key 加密存储，不写源码、不进日志。

---

## 一、上传到 GitHub（最少人工操作，全程网页点击）

本机没有 gh 工具与 GitHub 凭证，无法代你推送。以下步骤只需在浏览器点选，**无需输入任何命令**：

1. 打开 GitHub → 右上角 `+` → **New repository**。
2. 仓库名填 `deepseek-adventure`，选 **Public**（Private 也可，但 Actions 分钟数受限），**不要**勾选 "Add a README"。
3. 点 **Create repository**。
4. 在空仓库页面点 **"uploading an existing file"**（或 `Add file` → `Upload files`）。
5. 把本目录 `DeepSeekAdventure` 里的**全部内容与子文件夹**拖进上传框，务必包含隐藏的 `.github` 文件夹与 `.gitignore`。
6. 点 **Commit changes**。
7. 点顶部 **Actions** 标签 → 左侧选 **Build Android APK** → 点最新一次运行 → 等待绿色对勾（首次约 5–10 分钟）。
   - 如果 Actions 页面没看到任何构建：点右侧 **Run workflow** → 绿色按钮 **Run workflow**，手动触发一次即可（默认分支叫 main 或 master 都能自动触发，已同时兼容）。
8. 在运行页面底部 **Artifacts** → 下载 **`deepseek-adventure-apk`** → 解压得到 `app-debug.apk`（下载需要登录 GitHub；artifact 有效期 90 天）。

> 备选（若你愿意用命令行）：本地工程已 `git init` 并 commit，只需在 `DeepSeekAdventure` 目录执行：
> `git remote add origin https://github.com/<你的用户名>/deepseek-adventure.git`
> `git push -u origin main`

---

## 二、手机安装（最简单清单）

1. 把 `app-debug.apk` 传到手机（微信/QQ/数据线均可）。
2. 手机 **设置 → 安全/隐私 → 安装未知应用**，允许你用来打开 APK 的那个应用安装。
3. 点开 APK → **安装**。
4. 打开 App → 底部/顶部 **设置** → 粘贴 `sk-` 开头的 API Key → **保存 Key**（只显示尾 4 位）。
5. 切到 **会话** → 新建会话 → 输入剧情 → **发送**，开始冒险。

---

## 三、功能对应

| 需求 | 实现位置 |
| --- | --- |
| 流式聊天、SSE 停滞看门狗、连续失败降级非流式 | 设置页“流式”开关；聊天页发送 |
| 消息编辑/重试/分支 | 聊天气泡下方按钮；编辑后点“重新生成” |
| 思考模式折叠 | 聊天页“思考”开关；思考内容点一下展开 |
| 世界书注入 | 设定集 Tab |
| 前情摘要 | 聊天页右上“摘要” |
| 余额 + 本地用量估算 | 用量 Tab（今日/7天/30天/全部） |
| 价格/峰谷可配置 | 设置页价格表、节假日字段 |

---

## 四、已知边界

- 官方**没有**账单/明细用量 API；本应用用量仅来自响应 `usage` 字段本地累计，并在界面标注“估算”，不臆造接口。
- Token 数为本地启发式估算，发送前显示，实际以官方返回为准。
- 模型名与价格表全部可配置；`deepseek-chat`/`deepseek-reasoner` 已弃用，默认用 `deepseek-flash`。
- minSdk 29（Android 10+）；深色模式跟随系统或手动切换。
- 首次云端构建需要下载 Gradle 与依赖，耗时较长属正常。

---

## 五、工程结构

```
app/src/main/java/com/uusse/deepseekadventure/
├── App.kt / data/AppContainer.kt   手动 DI
├── core/network/                   OkHttp + SSE + DTO + 错误类型
├── core/security/                  EncryptedSharedPreferences
├── core/tokenizer|util/            token 估算、定价、峰谷判定
├── domain/                         模型与 Repository 接口
├── data/                           Room / DataStore / Repository 实现
└── ui/                             Compose 主题、导航、各功能页
.github/workflows/android.yml       云端构建工作流
```
