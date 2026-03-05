# 規格文件 - teacher.html

來源檔案: `src/main/resources/static/teacher.html`、
          `src/main/resources/static/js/teacher.js`

---

## API 互動邏輯 (fetch)

本頁面無 HTTP fetch/XHR 呼叫，所有通訊均透過原生 WebSocket 進行。

---

## WebSocket 互動邏輯（原生 WebSocket）

### 1. 建立 WebSocket 連線並註冊為 Teacher

- **連線端點**：`ws://10.0.101.187:8080/ws`
- **協定**：原生 WebSocket（`new WebSocket(...)`）
- **驗證**：無身份驗證。
- **連線建立後**（`onopen`）：立即傳送以下訊息向後端宣告自身為 Teacher：
```json
{
  "isTeacher": true
}
```
- **後端處理**：`MyWebSockethandler.handleTextMessage` 檢查訊息是否含 `"isTeacher"`，若是則將該 session 設為 `teacherSession`，後續只廣播來自此 session 的訊息。

---

### 2. 傳送繪圖資料

#### 2a. 開始新筆畫（mousedown）

- **觸發事件**：`myDrawer.onmousedown`
- **傳送訊息格式**：
```json
{
  "isClear": false,
  "isNewLine": true,
  "x": 150,
  "y": 200
}
```
- **本地行為**：`ctx.beginPath()` + `ctx.lineWidth = 4` + `ctx.moveTo(x, y)`（同步在本地畫布開始新路徑）。

#### 2b. 繪製線段（mousemove，拖曳中）

- **觸發事件**：`myDrawer.onmousemove`（僅 `isDrag === true` 時）
- **傳送訊息格式**：
```json
{
  "isClear": false,
  "isNewLine": false,
  "x": 155,
  "y": 205
}
```
- **本地行為**：`ctx.lineTo(x, y)` + `ctx.stroke()`（同步在本地畫布延伸並描繪路徑）。

#### 2c. 清除畫布（Clear 按鈕）

- **觸發事件**：`#clear` 按鈕 click 事件。
- **傳送訊息格式**：
```json
{
  "isClear": true
}
```
- **本地行為**：`ctx.clearRect(0, 0, width, height)`（同步清除本地畫布）。

| 欄位 | 型別 | 說明 |
|------|------|------|
| `isClear` | boolean | 是否清除畫布，`true` 時其餘欄位可省略 |
| `isNewLine` | boolean | `true` = 滑鼠按下（新筆畫），`false` = 滑鼠移動（延伸） |
| `x` | number | 滑鼠在 Canvas 上的 X 座標（`e.offsetX`） |
| `y` | number | 滑鼠在 Canvas 上的 Y 座標（`e.offsetY`） |

---

## 其他重要功能或邏輯

- 功能/邏輯名稱：拖曳狀態控制（isDrag）
- 描述：以布林變數 `isDrag` 管控繪圖狀態。`mousedown` 設為 `true`，`mouseup` 設為 `false`，`mousemove` 僅在 `isDrag === true` 時才傳送繪圖指令，避免滑鼠懸停時誤觸。
- 相關程式碼片段：
```javascript
// teacher.js
let isDrag = false;
myDrawer.onmousedown = function(e){ isDrag = true; /* ... */ }
myDrawer.onmouseup   = function(e){ isDrag = false; }
myDrawer.onmousemove = function(e){
    if (isDrag){ /* 傳送繪圖資料 */ }
}
```

---

- 功能/邏輯名稱：Teacher 唯一性機制（後端）
- 描述：後端 `MyWebSockethandler` 以靜態變數 `isExistTeacher` 與 `teacherSession` 確保系統中只有第一個宣告 `isTeacher` 的連線被視為 Teacher，後續的宣告會被忽略。Teacher 斷線後 `isExistTeacher` 不會自動重置（需伺服器重啟）。
- 相關程式碼片段：
```java
// MyWebSockethandler.java
private static boolean isExistTeacher = false;
private static WebSocketSession teacherSession;

if (!isExistTeacher && mesg.contains("isTeacher")) {
    isExistTeacher = true;
    teacherSession = session;
} else if (session == teacherSession) {
    // 廣播繪圖資料給所有 sessions
}
```

---

- 功能/邏輯名稱：本地即時繪圖 + 遠端同步
- 描述：Teacher 在傳送 WebSocket 訊息的同時，也會在本地 Canvas 執行相同的繪圖操作（`moveTo`/`lineTo`/`stroke`/`clearRect`），確保 Teacher 自身畫面即時反映，無需等待伺服器廣播回傳。
