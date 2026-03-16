import { useState } from "react";
import { useAuth0 } from "@auth0/auth0-react";

const AUTH_ENABLED = false; // true = Auth0 ativo, false = tudo aberto

function useMockAuth() {
  const [authenticated, setAuthenticated] = useState(true);

  return {
    authenticated,
    authLoading: false,
    loggedInEmail: "dev@ticketub.local",
    loggedInFirstName: "Dev",
    loggedInLastName: "User",
    handleLogout: () => setAuthenticated(false),
    authFormProps: { onLogin: () => setAuthenticated(true) },
  };
}

function useAuth0Flow() {
  const { isLoading, isAuthenticated, user, loginWithRedirect, logout } = useAuth0();

  const handleLogin = () => loginWithRedirect();

  const handleLogout = () => {
    localStorage.removeItem("activeTab");
    logout({ logoutParams: { returnTo: window.location.origin } });
  };

  const fullName = user?.name || "";
  const nameParts = fullName.trim().split(" ").filter(Boolean);
  const loggedInFirstName = user?.given_name || nameParts[0] || "Utilizador";
  const loggedInLastName = user?.family_name || nameParts.slice(1).join(" ") || "";

  return {
    authenticated: isAuthenticated,
    authLoading: isLoading,
    loggedInEmail: user?.email || "sem-email@ticketub.local",
    loggedInFirstName,
    loggedInLastName,
    handleLogout,
    authFormProps: { onLogin: handleLogin },
  };
}

export default function useAuthFlow() {
  const mockAuth = useMockAuth();
  const auth0Flow = useAuth0Flow();
  return AUTH_ENABLED ? auth0Flow : mockAuth;
}