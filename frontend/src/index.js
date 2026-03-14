import React from 'react';
import ReactDOM from 'react-dom/client';
import { Auth0Provider } from '@auth0/auth0-react';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';

const AUTH_ENABLED = false; // true = Auth0 ativo, false = tudo aberto

const auth0Domain = process.env.REACT_APP_AUTH0_DOMAIN;
const auth0ClientId = process.env.REACT_APP_AUTH0_CLIENT_ID;
const auth0Audience = process.env.REACT_APP_AUTH0_AUDIENCE;

const root = ReactDOM.createRoot(document.getElementById('root'));

if (AUTH_ENABLED && (!auth0Domain || !auth0ClientId)) {
  throw new Error('Missing Auth0 configuration. Set REACT_APP_AUTH0_DOMAIN and REACT_APP_AUTH0_CLIENT_ID.');
}

root.render(
  <React.StrictMode>
    {AUTH_ENABLED ? (
      <Auth0Provider
        domain={auth0Domain}
        clientId={auth0ClientId}
        authorizationParams={{
          redirect_uri: window.location.origin,
          ...(auth0Audience ? { audience: auth0Audience } : {}),
        }}
        cacheLocation="localstorage"
      >
        <App />
      </Auth0Provider>
    ) : (
      <App />
    )}
  </React.StrictMode>
);

reportWebVitals();