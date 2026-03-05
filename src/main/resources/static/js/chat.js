let token = null;
		let client = null;
		document.getElementById("login").onclick = async () =>{
			let email = document.getElementById("email").value;
			let passwd = document.getElementById("passwd").value;

			let resp = await fetch("/auth/login", {
				method: "POST",
				headers: {"Content-Type":"application/json"},
				body: JSON.stringify({
					"email": email,
					"password": passwd      // matches Login.password
				})
			});
			console.log(resp.ok);
			if (resp.ok) {
				let data = await resp.json();
				token = data.token;
				// display email if name is not provided by backend
				document.getElementById("status").innerHTML =
					"已登入(" + (data.name || data.email) + ")";
				document.getElementById("chatDiv").style.display = "block";
				connectWebSocket();
			} else {
				console.warn('登入失敗', resp.status);
			}
		};

		document.getElementById("send").onclick = async () =>{
			let input = document.getElementById("mesg");
			let mesg = input.value.trim();

			// url: /app + ?
			client.send('/app/chat/send',{},mesg);

			input.value = '';
		};

		function connectWebSocket(){
			let socket = new SockJS('/ws-chat?token=' + encodeURIComponent('Bearer ' + token));
			client = Stomp.over(socket);
			client.connect({}, function(frame){
				console.log(frame);
				client.subscribe("/topic/public", function(message){
					console.log(message);
					let body = JSON.parse(message.body);
					console.log(body);
					console.log("----");
					receiveMessage(body);
				});
			});
		}

		function receiveMessage(mesg){
			let box = document.getElementById("chatBox");
			let line = document.createElement("div");
			line.innerHTML = `[(${mesg.time})${mesg.email}]:${mesg.content}`;
			box.appendChild(line);
		}