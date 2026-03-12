import React from 'react';
import ReactDOM from 'react-dom/client';
import { Auth0Provider } from '@auth0/auth0-react';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';

const auth0Domain = process.env.REACT_APP_AUTH0_DOMAIN;
const auth0ClientId = process.env.REACT_APP_AUTH0_CLIENT_ID;
const auth0Audience = process.env.REACT_APP_AUTH0_AUDIENCE;

function MissingAuth0Config() {
  return (
    <div style={{ padding: '2rem', fontFamily: 'system-ui, -apple-system, Segoe UI, sans-serif' }}>
      <h1>Configuração Auth0 em falta</h1>
      <p>Define as variáveis REACT_APP_AUTH0_DOMAIN e REACT_APP_AUTH0_CLIENT_ID no ficheiro .env.</p>
    </div>
  );
}

const root = ReactDOM.createRoot(document.getElementById('root'));

if (!auth0Domain || !auth0ClientId) {
  root.render(
    <React.StrictMode>
      <MissingAuth0Config />
    </React.StrictMode>
  );
} else {
  root.render(
    <React.StrictMode>
      <Auth0Provider
        domain={auth0Domain}
        clientId={auth0ClientId}
        authorizationParams={{
          redirect_uri: window.location.origin,
          ...(auth0Audience ? { audience: auth0Audience } : {}),
        }}
      >
        <App />
      </Auth0Provider>
    </React.StrictMode>
  );
}

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
