# agents.md 使用教學（SAVE MONEY 專案版）

> 這份文件回答一件事：**你手上那份 `agents.md` 要放哪、怎麼讓 AI 真的讀到、怎麼確認它有生效。**

---

## Step 0. 先搞懂：agents.md 是什麼

它**不是程式碼**，是一份「規則書」。

AI 每開一個新對話就會失憶一次，完全不記得你上次講過什麼。
規則書的作用就是：**每次開場自動塞給 AI 看的備忘錄。**

你上傳的那份內容是「工程原則」，例如：

- 不留向後相容的舊路徑
- 選最簡單、能滿足現有需求的做法
- 一層一層長，永遠保持有一個能跑的版本
- 優先用專案已有的套件，不要自己重造輪子

這些是在管**「AI 寫程式的品味」**。

---

## Step 1. 認識兩個檔名：CLAUDE.md vs AGENTS.md

| | CLAUDE.md | AGENTS.md |
|---|---|---|
| 誰讀 | Claude Code 專用 | 業界共通格式（Codex、Cursor、Copilot、Gemini CLI…） |
| 用途 | 一模一樣：告訴 AI 怎麼做事 | 一模一樣 |

**最關鍵的一句話：**

> Claude Code 只會自動讀 `CLAUDE.md`，**不會自己去讀 `AGENTS.md`**。

所以光把 `AGENTS.md` 丟進專案，**它是死的、完全沒作用**。
必須做 Step 3 那個動作，它才會活過來。

那為什麼還要用 AGENTS.md 這個檔名？
因為它是共通標準。你以後換工具、或跟別人協作，那份檔案照樣被讀得到，不用重寫一次。

---

## Step 2. 你現在的狀況

```
D:\discount coupon\
├── .claude\
│   ├── CLAUDE.md          ← 已經有了：角色設定 + 專案說明 + 技術棧
│   ├── flow\index.md      ← 資料流文件
│   └── db\main-coupon.md  ← 資料庫文件
├── db\
└── project\               ← 原始碼
```

**還沒有 AGENTS.md。**

而且要注意：你上傳的 `agents.md` 跟現有的 `CLAUDE.md` **不衝突，是互補的**：

- `CLAUDE.md` 管的是「你是誰、專案是什麼、要用什麼語氣教我」
- `agents.md` 管的是「寫程式時的取捨原則」

兩份可以同時存在。

---

## Step 3. 動手：三個動作

### 3-1　把檔案放到專案根目錄

放這裡（注意檔名大寫，這是慣例）：

```
D:\discount coupon\AGENTS.md
```

不要放進 `.claude\` 資料夾。放根目錄的理由：業界標準位置，其他工具才找得到。

---

### 3-2　在 CLAUDE.md 開頭加一行，把它「接」進來

打開 `D:\discount coupon\.claude\CLAUDE.md`，在**最上面第一行**加：

```markdown
@../AGENTS.md

Agent Persona
角色定位
...（以下你原本的內容不動）
```

**這行為什麼要有 `..`？這是最多人踩的坑。**

`@` 匯入的路徑，是相對於「**寫這行字的那個檔案自己**」，不是相對於專案根目錄。

```
CLAUDE.md 住在  D:\discount coupon\.claude\   ← 從這裡出發
AGENTS.md 住在  D:\discount coupon\           ← 要往外走一層

所以是  ..\AGENTS.md  →  寫成 @../AGENTS.md
```

如果你寫成 `@AGENTS.md`，它會去找 `.claude\AGENTS.md`——那裡沒有東西，就靜靜地失敗，你不會收到任何錯誤訊息。

> 小提醒：你原本的 `@flow/index.md` 和 `@db/main-coupon.md` 沒有 `..`，這是**對的**，因為那兩個資料夾就在 `.claude\` 裡面。

---

### 3-3　不要在 Windows 用 symlink

網路上會看到另一種做法：用 `ln -s AGENTS.md CLAUDE.md` 建捷徑。

**Windows 上不要用**——它需要系統管理員權限或開發者模式，很容易搞半天失敗。
用 Step 3-2 的 `@` 匯入就好，效果一樣。

---

## Step 4. 驗證它到底有沒有生效

**改完規則檔一定要開「全新的一次對話」。** 舊對話不會重讀，改了也沒用。

### 方法 A：Claude Code（終端機 / VS Code）

```
/context     ← 看「Memory files」清單裡有沒有列出你的檔案
/memory      ← 可以直接瀏覽、打開來編輯
```

沒列出來 = AI 根本看不到 = 路徑寫錯了，回去檢查 Step 3-2。

### 方法 B：任何介面都能用的土法煉鋼（推薦你先用這個）

開新對話，直接問：

> 你現在遵守哪些工程原則？用中文條列出來。

如果 AI 答得出「不留向後相容」「選最簡單的做法」這些，就是成功了。
如果它開始瞎掰或說不知道，就是沒讀到。

---

## ⚠️ Step 5. 一個實測發現：桌面版 Cowork 不會展開 `@` 匯入

這是實際驗證過的，不是猜的：

在 Cowork（Claude 桌面版）裡，`.claude\CLAUDE.md` **本身會被讀取**，
但裡面的 `@flow/index.md`、`@db/main-coupon.md` **只會被當成一般文字**，
被指到的那些檔案**內容不會被載入**。

**這代表什麼？**

| 你用的工具 | `@../AGENTS.md` 有效嗎 |
|---|---|
| Claude Code（終端機 / VS Code） | ✅ 有效 |
| Cowork（桌面版） | ❌ 不會自動展開 |

**在 Cowork 裡要讓規則生效，有兩條路：**

1. **直接把規則貼進 `.claude\CLAUDE.md` 本文**（最保險，但檔案會變長）
2. **每次開場跟 AI 說一句**：「先讀 AGENTS.md 再開始」——它會用工具去讀

如果你主要在 VS Code 用 Claude Code 寫程式，那就照 Step 3 做，沒問題。

---

## Step 6. 寫規則的三個地雷

### 地雷 1：太長

目標 **200 行以內**。檔案越長，AI 遵守率反而越低（規則被稀釋掉了）。

### 地雷 2：太模糊

| ❌ 模糊 | ✅ 具體 |
|---|---|
| 「程式碼要寫整齊」 | 「縮排用 4 個空格」 |
| 「要測試」 | 「改完 Service 層要跑 `mvn test`」 |
| 「檔案要分類好」 | 「Controller 一律放 `project/src/main/java/com/example/demo/controller/`」 |

判斷標準：**這條規則能不能用「有做/沒做」來檢查？** 不行就是太模糊。

### 地雷 3：互相矛盾

兩條規則打架時，AI 會**隨便挑一條**，而且不會告訴你。
所以每次加新規則，順手看一下有沒有跟舊的衝突。

---

## Step 7. 給你的一個真心提醒

你這份 `agents.md` 的**第一條**：

> Do not preserve backward compatibility. Remove obsolete paths instead of adding compatibility layers, fallbacks, or migrations.
> （不保留向後相容。直接刪掉過時的路徑，不要加相容層、fallback 或遷移程式碼。）

這條在成熟團隊很合理，但**對學習中的專案偏兇**。

風險是：AI 可能把你「還在學、還想留著參考」的舊寫法**直接刪掉**，而它認為自己完全照規則辦事。

**建議加一句限制**，例如在那條後面補上：

```markdown
- Do not preserve backward compatibility. Remove obsolete paths instead of adding
  compatibility layers, fallbacks, or migrations.
  刪除任何超過 5 行的既有程式碼前，先列出要刪什麼、為什麼，等我確認。
```

這樣既保留原則，又不會失控。

---

## Step 8. 進階：規則變多的時候用 `.claude/rules/`

等你規則寫到快 200 行，可以拆檔：

```
.claude/
├── CLAUDE.md          ← 主規則（保持精簡）
└── rules/
    ├── frontend.md    ← 前端規範
    ├── backend.md     ← Spring Boot / MyBatis 規範
    └── sql.md         ← 資料庫規範
```

厲害的地方是**可以綁定路徑**——只有在改到對應檔案時才載入，平常不佔空間。

以你的 SAVE MONEY 為例，`.claude/rules/backend.md` 可以這樣寫：

```markdown
---
paths:
  - "project/src/main/java/**/*.java"
---

# 後端規範
- Controller 只做參數驗證和回傳，商業邏輯一律寫在 Service
- 查資料庫一律走 MyBatis-Plus，不手寫 JDBC
- Redis 的 key 命名格式：`savemoney:token:{userId}`
```

最上面 `---` 包起來的部分叫 **frontmatter**，`paths` 就是「這條規則只在改 Java 檔時才出現」。

---

## 速查表

| 你想做的事 | 怎麼做 |
|---|---|
| 讓 AI 讀到 AGENTS.md | 在 `.claude/CLAUDE.md` 第一行加 `@../AGENTS.md` |
| 確認有沒有讀到 | 開新對話 → `/context` 看 Memory files；或直接問 AI |
| 改了規則沒反應 | **開新對話**，舊對話不會重讀 |
| 規則太長 | 拆到 `.claude/rules/`，用 `paths` 綁定路徑 |
| 想臨時加規則 | 直接在對話裡說「加到 CLAUDE.md」 |
| 桌面版 Cowork 要生效 | 規則直接寫進 `CLAUDE.md` 本文，或開場說「先讀 AGENTS.md」 |

---

## 參考來源

- [Claude Code 官方文件：How Claude remembers your project](https://code.claude.com/docs/en/memory)
