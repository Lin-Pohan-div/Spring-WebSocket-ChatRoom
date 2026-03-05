# 規格文件 - student.html

來源檔案: `src/main/resources/static/student.html`、
          `src/main/resources/static/js/student.js`

---

## API 互動邏輯 (fetch)

本頁面無 HTTP fetch/XHR 呼叫，所有通訊均透過原生 WebSocket 進行。

---

## WebSocket 互動邏輯（原生 WebSocket）

### 1. 建立 WebSocket 連線

- **連線端點**：`ws://10.0.101.187:8080/ws`
- **協定**：原生 WebSocket（`new WebSocket(...)`）
- **驗證**：無身份驗證，連線後直接成為接收端（Student）。
- **後端處理**：`WebSocketConfig` 將 `/ws` 路由至 `MyWebSockethandler`，所有 session 加入 `sessions` 集合。

### 2. 接收繪圖指令

- **觸發時機**：`webSocket.onmessage` 事件（收到 Teacher 廣播的訊息）。
- **訊息格式**：
```json
{
  "isClear": false,
  "isNewLine": true,
  "x": 120,
  "y": 80
}
```

| 欄位 | 型別 | 說明 |
|------|------|------|
| `isClear` | boolean | 是否清除畫布 |
| `isNewLine` | boolean | 是否開始新筆畫（mousedown） |
| `x` | number | Canvas 上的 X 座標 |
| `y` | number | Canvas 上的 Y 座標 |

- **資料解讀與處理邏輯**：
  1. 解析 JSON 字串為物件（`JSON.parse(event.data)`）。
  2. 若 `isClear === true`：清空畫布。
  3. 若 `isNewLine === true`：呼叫 `newLine(x, y)`，開始新路徑。
  4. 其他情況：呼叫 `drawLine(x, y)`，延伸路徑並繪製。

- **顯示邏輯**：
  - 畫布元素為 `<canvas id="myDrawer" width="800px" height="480px">`。
  - `newLine(x, y)`：`ctx.beginPath()` + `ctx.lineWidth = 4` + `ctx.moveTo(x, y)`。
  - `drawLine(x, y)`：`ctx.lineTo(x, y)` + `ctx.stroke()`。
  - `clear()`：`ctx.clearRect(0, 0, width, height)`。

- 其他重要細節：
  - 設有 `isConnect` 旗標，僅在連線成功（`onopen`）後才處理訊息，`onclose` 時重置為 `false`。
  - Student 為純接收端，不會向伺服器傳送任何訊息。

---

## 其他重要功能或邏輯

- 功能/邏輯名稱：被動接收角色（純 Subscriber）
- 描述：Student 頁面不進行任何輸入或操作，僅監聽 WebSocket 訊息並同步呈現 Teacher 在畫布上的繪圖動作（包含清除）。
- 相關程式碼片段：
```javascript
// student.js
webSocket.onmessage = function(event){
    if (isConnect){
        let mesgObj = JSON.parse(event.data);
        if (mesgObj.isClear){
            clear();
        } else {
            if (mesgObj.isNewLine){
                newLine(mesgObj.x, mesgObj.y);
            } else {
                drawLine(mesgObj.x, mesgObj.y);
            }
        }
    }
}
```

---

- 功能/邏輯名稱：後端廣播機制
- 描述：後端 `MyWebSockethandler.handleTextMessage` 收到 Teacher session 傳來的繪圖資料後，遍歷所有 `sessions`（含 Teacher 自身）並廣播相同訊息，Student 端因此收到同步的繪圖指令。
- 相關程式碼片段：
```java
// MyWebSockethandler.java
for (WebSocketSession s : sessions) {
    if (s.isOpen()) {
        s.sendMessage(message);
    }
}
```
