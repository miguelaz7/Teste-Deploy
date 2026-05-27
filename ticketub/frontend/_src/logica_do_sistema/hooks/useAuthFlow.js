import { useState } from "react";
import { useAuth0 } from "@auth0/auth0-react";

const AUTH_ENABLED = process.env.REACT_APP_AUTH_ENABLED !== "false"; // default true

function useMockAuth() {
  const [authenticated, setAuthenticated] = useState(false);

  // Permite testar diferentes perfis via URL: ex. ?roles=ANALISTA ou ?roles=DPO
  const urlParams = new URLSearchParams(window.location.search);
  const rolesParam = urlParams.get("roles");
  const userRoles = rolesParam 
    ? rolesParam.split(",").map(r => r.trim().toUpperCase())
    : ["GESTOR", "ANALISTA", "DPO", "ADMIN"];

  return {
    authenticated,
    authLoading: false,
    loggedInEmail: "dev@ticketub.local",
    loggedInFirstName: "Dev",
    loggedInLastName: "User",
    userRoles,
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

  // Extract roles and permissions from token claims
  const rawRoles = user?.["https://ticketub.pt/roles"] || user?.roles || user?.permissions || [];
  let userRoles = Array.isArray(rawRoles)
    ? rawRoles.map(r => r.toUpperCase().replace("ROLE_", ""))
    : [];

  // Fallback: If no roles are found in Auth0 claims (e.g., local test accounts),
  // grant all roles to avoid locking the developer/user out of their tabs.
  if (userRoles.length === 0) {
    userRoles = ["GESTOR", "ANALISTA", "DPO", "ADMIN"];
  }

  return {
    authenticated: isAuthenticated,
    authLoading: isLoading,
    loggedInEmail: user?.email || "sem-email@ticketub.local",
    loggedInFirstName,
    loggedInLastName,
    userRoles,
    handleLogout,
    authFormProps: { onLogin: handleLogin },
  };
}

const useAuthFlow = AUTH_ENABLED ? useAuth0Flow : useMockAuth;
export default useAuthFlow;

