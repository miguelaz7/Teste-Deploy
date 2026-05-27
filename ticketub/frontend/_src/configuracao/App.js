import "../estilo_e_design/App.css";
import AuthForm from "../funcionalidades/auth/AuthForm";
import Dashboard from "../ecras/Dashboard/Dashboard";
import useAuthFlow from "../logica_do_sistema/hooks/useAuthFlow";

function App() {
  const {
    authenticated,
    authLoading,
    semPerfil,
    loggedInEmail,
    loggedInFirstName,
    loggedInLastName,
    userRoles,
    handleLogout,
    authFormProps,
  } = useAuthFlow();

  // UC01 – A carregar estado de autenticação
  if (authLoading) {
    return (
      <div className="app-shell" style={{ display: "grid", placeItems: "center", minHeight: "100vh" }}>
        <div style={{ textAlign: "center", color: "var(--text-muted)" }}>
          <div style={{ fontSize: "2rem", marginBottom: "1rem" }}>🔐</div>
          <p>A verificar autenticação...</p>
        </div>
      </div>
    );
  }

  // UC01 / FA4 – Utilizador autenticado mas sem perfil associado
  if (semPerfil) {
    return (
      <div className="app-shell" style={{ display: "grid", placeItems: "center", minHeight: "100vh" }}>
        <div style={{
          background: "var(--card-bg, #1e2a35)",
          border: "1px solid rgba(239, 68, 68, 0.4)",
          borderRadius: "1rem",
          padding: "2.5rem",
          maxWidth: "480px",
          textAlign: "center",
          boxShadow: "0 8px 32px rgba(0,0,0,0.3)"
        }}>
          <div style={{ fontSize: "3rem", marginBottom: "1rem" }}>⛔</div>
          <h2 style={{ color: "#ef4444", marginBottom: "0.75rem" }}>Acesso Negado</h2>
          <p style={{ color: "var(--text-muted)", marginBottom: "0.5rem" }}>
            A sua conta foi autenticada, mas não possui um perfil autorizado no TickeTUB.
          </p>
          <p style={{ color: "var(--text-muted)", fontSize: "0.85rem", marginBottom: "2rem" }}>
            Contacte o administrador de sistema para que lhe seja atribuído o perfil correto.
            A ocorrência foi registada para auditoria.
          </p>
          <button
            onClick={handleLogout}
            style={{
              background: "rgba(239,68,68,0.15)",
              border: "1px solid rgba(239,68,68,0.5)",
              color: "#ef4444",
              padding: "0.75rem 2rem",
              borderRadius: "0.5rem",
              cursor: "pointer",
              fontSize: "0.9rem"
            }}
          >
            Terminar Sessão
          </button>
        </div>
      </div>
    );
  }

  // UC01 – Utilizador autenticado com perfil válido → Dashboard
  if (authenticated) {
    return (
      <Dashboard
        loggedInEmail={loggedInEmail}
        loggedInFirstName={loggedInFirstName}
        loggedInLastName={loggedInLastName}
        userRoles={userRoles}
        onLogout={handleLogout}
      />
    );
  }

  // UC01 – Utilizador não autenticado → Ecrã de login (redirect para Auth0)
  return <AuthForm {...authFormProps} />;
}

export default App;
