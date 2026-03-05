# 規格文件 - chat.html
來源檔案:  `web/chat.html`、
          `web/student.html`、
          `web/teacher.html`
---
## API 互動邏輯 (fetch)
針對頁面中每一個 Web API 呼叫（fetch/XHR），填寫以下資訊。

### 1. 使用者登入
* **請求資訊（HTTP Request）**
- Method: `POST`
- URL: `/auth/login`
- Headers: `Content-Type: application/json`
- Payload (Request Body):
```json
{
  "email": "使用者輸入的 email 字串",
  "password": "使用者輸入的密碼字串"
}
```
* **回應內容 (Response)**
- HTTP Status: `200 OK`（成功）或 `401 Unauthorized`（帳密不符）
- Body
```json
{
  "email": "user@example.com",
  "token": "JWT 文字串",
  // 可選的欄位
  "name": "使用者名稱"
}
```
 失敗時會以純文本輸出錯誤訊息，例如 `帳號錯誤` 或 `密碼錯誤`。

* **資料解讀與處理邏輯：**
 取得 `token` 欄位後儲存於全域 `token` 變數。`email` 與 `name` 用於顯示登入狀態。
 失敗時會從 response body 讀出訊息並以 `alert` 彈出，也在控制台記錄。

* **顯示邏輯：**
  - 成功：將狀態元素 `#status` 設為 `已登入(名稱或 email)`，顯示聊天區塊 `#chatDiv`，並呼叫 `connectWebSocket()` 開啟 WebSocket。
  - 失敗：彈出提示並在 console 中顯示錯誤。

* **其他重要細節：**
  - 前端送出 JSON 字段必須與 `Login` DTO 的 `email`/`password` 對應。
  - 註冊功能存在於後端 (`/auth/register`)，但前端介面未直接呼叫。


## 其他重要功能或邏輯

- 功能/邏輯名稱：WebSocket 聊天連線與訊息接收
- 描述：
  使用 STOMP over SockJS 連線到後端 `/ws-chat`，連線參數包含 `token` 作為 JWT 授權。連線建立後，訂閱 `/topic/public`，處理伺服器推送的訊息物件。
  `receiveMessage()` 將 JSON 解析後加入聊天視窗。

- 相關程式碼片段：
```js
function connectWebSocket(){
    let socket = new SockJS('/ws-chat?token=' + encodeURIComponent('Bearer ' + token));
    client = Stomp.over(socket);
    client.connect({}, function(frame){
        console.log(frame);
        client.subscribe("/topic/public", function(message){
            let body = JSON.parse(message.body);
            receiveMessage(body);
        });
    });
}
```

---

# 規格文件 - student.html
來源檔案:  `web/student.html`
---

## 其他重要功能或邏輯

- 功能/邏輯名稱：教師繪圖即時同步
- 描述：
  於 `window.onload` 建立原生 WebSocket 連線至後端 `ws://10.0.101.187:8080/ws`。
  接收訊息後根據 JSON 內容繪製或清除畫布。訊息格式包含 `isClear`、`isNewLine`、`x`、`y` 欄位。

- 相關程式碼片段：
```js
webSocket.onmessage = function(event){
    if (isConnect){
        let mesgObj = JSON.parse(event.data);
        if (mesgObj.isClear){
            clear()
        }else {
            if (mesgObj.isNewLine){
                newLine(mesgObj.x, mesgObj.y);
            }else{
                drawLine(mesgObj.x, mesgObj.y);
            }
        }
    }
}
```

畫布繪圖函數：
```js
function clear(){
    ctx.clearRect(0,0,myDrawer.width, myDrawer.height);
}
function newLine(x,y){
    ctx.beginPath();
    ctx.lineWidth = 4;
    ctx.moveTo(x, y);
}
function drawLine(x,y){
    ctx.lineTo(x, y);
    ctx.stroke();
}
```

以上均在 `window.onload` 初始化階段設定。

---

# 規格文件 - teacher.html
來源檔案:  `web/teacher.html`
---

## 其他重要功能或邏輯

- 功能/邏輯名稱：教師端繪圖與廣播
- 描述：
  於 `window.onload` 打開與相同 URI 的 WebSocket，連線成功時發送 `{"isTeacher":true}` 表明身份。
  畫布事件 `mousedown`、`mousemove`、`mouseup` 用於繪製線條，並同步每一個座標或清除動作給伺服器，訊息架構與 student.html 相同。
  ``Clear`` 按鈕會清空畫布並發送 `{"isClear":true}`。

- 相關程式碼片段：
```js
webSocket.onopen = function(){
    isConnect = true;
    webSocket.send(JSON.stringify({isTeacher:true}));
}
...
myDrawer.onmousedown = function(e){
    isDrag = true;
    let x = e.offsetX, y = e.offsetY;
    ctx.beginPath();
    ctx.lineWidth = 4;
    ctx.moveTo(x, y);

    let data = {
        isClear : false,
        isNewLine : true,
        x : x,
        y : y
    };
    webSocket.send(JSON.stringify(data));
}
...
clear.addEventListener("click",function(){
    ctx.clearRect(0,0,myDrawer.width, myDrawer.height);
    let data = {
        isClear : true
    };
    webSocket.send(JSON.stringify(data));
});
```


```
