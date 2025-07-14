const WebSocket = require('ws');

// Connect to the debate service WebSocket
const ws = new WebSocket('ws://localhost:5013/ws?organization_id=default');

ws.on('open', () => {
  console.log('WebSocket connected');
});

ws.on('message', (data) => {
  console.log('Received:', JSON.parse(data.toString()));
});

ws.on('error', (error) => {
  console.error('WebSocket error:', error);
});

ws.on('close', () => {
  console.log('WebSocket disconnected');
});

// Keep the script running
console.log('Listening for WebSocket messages...');