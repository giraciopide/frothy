var undercover = {
	socket: null
};

/**
 * When DOM is ready.
 */
document.onreadystatechange = () => {
	if (document.readyState == "complete") {
		// setup the login button
		undercover.socket = setupWebsocket();
		setupSendMessage();
	}
}

// TODO
let setupLoginButton = function() {
	$('#login-button').on('click', () => {
		let username = $('#username').val();
	});
}

let setupSendMessage = function() {
	// the click handler
	let sendMessageText = () => {
		let text = $('textarea#message-input').val();
		if (undercover.socket) {
			let message = {
				user: 'test', // TODO change
				type: 'say', // type of message say, whisper.
				payload: text
			};
			undercover.socket.send(JSON.stringify(message));
		}
	}

	$('button#send-message').on('click', sendMessageText);
}

/**
 * Sets up and returns a new socket connection
 */
let setupWebsocket = function() {
	let url = 'ws://' + window.location.host + '/ws';
	// url = 'ws://127.0.0.1:8080/ws'
	let socket = new WebSocket(url);

	// Connection opened
	socket.addEventListener('open', function (event) {
	    socket.send('Hello Server!');
	});

	// Listen for messages
	socket.addEventListener('message', function (event) {
		$('ul#conversation').append('<li>' + JSON.stringify(event.data, 2) + '</li>');
	    console.log('Message from server', event.data);
	});

	return socket;
}