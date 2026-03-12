import { useAuth0 } from "@auth0/auth0-react";

export default function useAuthFlow() {
  const { isLoading, isAuthenticated, user, loginWithRedirect, logout } = useAuth0();

  const handleLogin = () => loginWithRedirect();

  const handleLogout = () => {
    localStorage.removeItem("activeTab");
    logout({
      logoutParams: {
        returnTo: window.location.origin,
      },
    });
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
    authFormProps: {
      onLogin: handleLogin,
    },
  };
}
