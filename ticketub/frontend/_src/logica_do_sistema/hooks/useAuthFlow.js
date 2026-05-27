import { useState, useEffect } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import { setTokenGetter } from "../services/apiClient";

const AUTH_ENABLED = process.env.REACT_APP_AUTH_ENABLED !== "false"; // default true

// ── UC01.3 – Tabela de mapeamento: papel do IdP → perfil interno ────────────
const MAPEAMENTO_PERFIS = {
  "tub-gestor":   "GESTOR",
  "tub-operador": "OPERADOR",
  "tub-analista": "ANALISTA",
  "tub-dpo":      "DPO",
  "tub-admin":    "ADMIN",
  "gestor":       "GESTOR",
  "operador":     "OPERADOR",
  "analista":     "ANALISTA",
  "dpo":          "DPO",
  "admin":        "ADMIN",
};

function mapearPerfis(rawRoles) {
  const perfis = [];
  for (const role of rawRoles) {
    const normalizado = role.toLowerCase().replace("role_", "").trim();
    const perfilInterno = MAPEAMENTO_PERFIS[normalizado];
    if (perfilInterno && !perfis.includes(perfilInterno)) {
      perfis.push(perfilInterno);
    }
  }
  return perfis;
}

// UC01.2 – Decodifica o payload de um JWT (sem verificação de assinatura —
// já foi verificada pelo Auth0 antes de ser emitido)
function decodeJwtPayload(token) {
  try {
    const payload = token.split(".")[1];
    const decoded = atob(payload.replace(/-/g, "+").replace(/_/g, "/"));
    return JSON.parse(decoded);
  } catch {
    return {};
  }
}

// ── Mock Auth (apenas para ambiente de desenvolvimento local) ─────────────────
function useMockAuth() {
  const [authenticated, setAuthenticated] = useState(false);

  // Permite testar diferentes perfis via URL: ex. ?roles=ANALISTA ou ?roles=DPO
  const urlParams = new URLSearchParams(window.location.search);
  const rolesParam = urlParams.get("roles");
  const userRoles = rolesParam
    ? rolesParam.split(",").map(r => r.trim().toUpperCase())
    : ["GESTOR", "ANALISTA", "DPO", "ADMIN"];

  // Mock: sem token real — apiClient não precisa de token em modo dev
  useEffect(() => {
    setTokenGetter(null);
  }, []);

  return {
    authenticated,
    authLoading: false,
    semPerfil: false,
    loggedInEmail: "dev@ticketub.local",
    loggedInFirstName: "Dev",
    loggedInLastName: "User",
    userRoles,
    handleLogout: () => setAuthenticated(false),
    authFormProps: { onLogin: () => setAuthenticated(true) },
  };
}

// ── Auth0 Flow (UC01) ─────────────────────────────────────────────────────────
function useAuth0Flow() {
  const { isLoading, isAuthenticated, user, loginWithRedirect, logout, getAccessTokenSilently } = useAuth0();

  // UC01.2 – Roles lidas do access_token (onde o Auth0 Action as coloca)
  const [accessTokenRoles, setAccessTokenRoles] = useState(null); // null = ainda a carregar

  useEffect(() => {
    if (isAuthenticated) {
      // UC01 – Regista o getter do token no apiClient para todas as chamadas à API
      setTokenGetter(() => getAccessTokenSilently());

      getAccessTokenSilently()
        .then(token => {
          const payload = decodeJwtPayload(token);
          const roles = payload["https://ticketub.pt/roles"] || [];
          setAccessTokenRoles(Array.isArray(roles) ? roles : []);
        })
        .catch(() => {
          setAccessTokenRoles([]);
        });
    } else {
      setTokenGetter(null);
      setAccessTokenRoles(null);
    }
  }, [isAuthenticated, getAccessTokenSilently]);

  // UC01.1 – Redirecionar para o IdP (Auth0 gere PKCE internamente)
  const handleLogin = () => loginWithRedirect();

  const handleLogout = () => {
    localStorage.removeItem("activeTab");
    setTokenGetter(null);
    logout({ logoutParams: { returnTo: window.location.origin } });
  };

  // UC01.2 – Extrair atributos do id_token (nome, email)
  const fullName = user?.name || "";
  const nameParts = fullName.trim().split(" ").filter(Boolean);
  const loggedInFirstName = user?.given_name || nameParts[0] || "Utilizador";
  const loggedInLastName  = user?.family_name || nameParts.slice(1).join(" ") || "";

  // Ainda a aguardar roles do access_token → mostrar loading
  const authLoading = isLoading || (isAuthenticated && accessTokenRoles === null);

  // UC01.3 – Mapear papéis para perfis internos
  const userRoles = mapearPerfis(accessTokenRoles || []);

  // UC01.3 / FA4 – Utilizador sem perfil associado: bloquear acesso
  const semPerfil = isAuthenticated && accessTokenRoles !== null && userRoles.length === 0;

  return {
    authenticated: isAuthenticated && !semPerfil && accessTokenRoles !== null,
    authLoading,
    semPerfil,
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
