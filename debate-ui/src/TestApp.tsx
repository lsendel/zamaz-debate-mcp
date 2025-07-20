import React from 'react';

function TestApp() {
  return (
    <div style={{ padding: '20px', fontFamily: 'Arial, sans-serif' }}>
      <h1>Test App - React is Working!</h1>
      <p>If you can see this, React 18 is rendering correctly.</p>
      <p>Current time: {new Date().toLocaleTimeString()}</p>
      <button onClick={() => alert('Button clicked!')}>
        Test Button
      </button>
    </div>
  );
}

export default TestApp;