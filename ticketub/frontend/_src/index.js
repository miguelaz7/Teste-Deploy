import React from 'react';
import ReactDOM from 'react-dom/client';
import { Auth0Provider } from '@auth0/auth0-react';
import './estilo_e_design/index.css';
import App from './configuracao/App';
import reportWebVitals from './configuracao/reportWebVitals';

const AUTH_ENABLED = process.env.REACT_APP_AUTH_ENABLED !== "false"; // default true

const auth0Domain = "dev-8mibe6xcqdkkxfaj.us.auth0.com";
const auth0ClientId = "1LuJnh3EWY2AVo956qkU4HtZTgy30EnY";
const auth0Audience = "https://dev-8mibe6xcqdkkxfaj.us.auth0.com/api/v2/";

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




