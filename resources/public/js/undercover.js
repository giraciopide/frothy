document.onreadystatechange = () => {
	if (document.readyState == "complete") {
		let socket = setupWsConnection();
	}
}

/**
 * Sets up and returns a new socket connection
 */
let setupWsConnection = function() {
	let url = 'ws://' + window.location.host + '/ws';
	// url = 'ws://127.0.0.1:8080/ws'
	let socket = new WebSocket(url);

	// Connection opened
	socket.addEventListener('open', function (event) {
	    socket.send('Hello Server!');
	});

	// Listen for messages
	socket.addEventListener('message', function (event) {
	    console.log('Message from server', event.data);
	});

	return socket;
}