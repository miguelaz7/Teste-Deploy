import { useState } from "react";

export default function useAuthFlow() {
  const [authenticated, setAuthenticated] = useState(() => {
    return localStorage.getItem("authenticated") === "true";
  });
  const [loggedInEmail, setLoggedInEmail] = useState(() => {
    return localStorage.getItem("loggedInEmail") || "visitante@ticketub.pt";
  });
  const [loggedInFirstName, setLoggedInFirstName] = useState(() => {
    return localStorage.getItem("loggedInFirstName") || "Visitante";
  });
  const [loggedInLastName, setLoggedInLastName] = useState(() => {
    return localStorage.getItem("loggedInLastName") || "";
  });

  const handleLogin = () => {
    const defaultEmail = "visitante@ticketub.pt";
    const defaultFirstName = "Visitante";
    const defaultLastName = "";

    setAuthenticated(true);
    setLoggedInEmail(defaultEmail);
    setLoggedInFirstName(defaultFirstName);
    setLoggedInLastName(defaultLastName);

    localStorage.setItem("authenticated", "true");
    localStorage.setItem("loggedInEmail", defaultEmail);
    localStorage.setItem("loggedInFirstName", defaultFirstName);
    localStorage.setItem("loggedInLastName", defaultLastName);
  };

  const handleLogout = () => {
    setAuthenticated(false);
    setLoggedInEmail("");
    setLoggedInFirstName("");
    setLoggedInLastName("");

    localStorage.removeItem("authenticated");
    localStorage.removeItem("loggedInEmail");
    localStorage.removeItem("loggedInFirstName");
    localStorage.removeItem("loggedInLastName");
    localStorage.removeItem("activeTab");
  };

  return {
    authenticated,
    loggedInEmail,
    loggedInFirstName,
    loggedInLastName,
    handleLogout,
    authFormProps: {
      onLogin: handleLogin,
    },
  };
}
