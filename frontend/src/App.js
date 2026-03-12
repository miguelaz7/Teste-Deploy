import "./App.css";
import { useAuth0 } from "@auth0/auth0-react";
import AuthForm from "./components/AuthForm";
import Dashboard from "./components/Dashboard";

function App() {
  const { isLoading, isAuthenticated, user, error, loginWithRedirect, logout } = useAuth0();

  const fullName = (user?.name || "").trim();
  const [fallbackFirstName, ...restNames] = fullName.split(" ").filter(Boolean);
  const fallbackLastName = restNames.join(" ").trim();
  const loggedInFirstName = user?.given_name || fallbackFirstName || "";
  const loggedInLastName = user?.family_name || fallbackLastName || "";
  const loggedInEmail = user?.email || "";

  if (isLoading) {
    return <div className="app-shell" style={{ display: "grid", placeItems: "center", minHeight: "100vh" }}>A carregar autenticação...</div>;
  }

  if (error) {
    return (
      <AuthForm
        auth0Enabled
        mensagem={`Erro de autenticação: ${error.message}`}
        onLogin={() => loginWithRedirect()}
        onSignup={() => loginWithRedirect({ authorizationParams: { screen_hint: "signup" } })}
      />
    );
  }

  if (isAuthenticated) {
    return (
      <Dashboard
        loggedInEmail={loggedInEmail}
        loggedInFirstName={loggedInFirstName}
        loggedInLastName={loggedInLastName}
        onLogout={() => logout({ logoutParams: { returnTo: window.location.origin } })}
      />
    );
  }

  return (
    <AuthForm
      auth0Enabled
      onLogin={() => loginWithRedirect()}
      onSignup={() => loginWithRedirect({ authorizationParams: { screen_hint: "signup" } })}
    />
  );
}

export default App;
