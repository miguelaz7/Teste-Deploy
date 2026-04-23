import "../estilo_e_design/App.css";
import AuthForm from "../funcionalidades/auth/AuthForm";
import Dashboard from "../ecras/Dashboard/Dashboard";
import useAuthFlow from "../logica_do_sistema/hooks/useAuthFlow";

function App() {
  const { authenticated, authLoading, loggedInEmail, loggedInFirstName, loggedInLastName, handleLogout, authFormProps } = useAuthFlow();

  if (authLoading) {
    return <div className="app-shell" style={{ display: "grid", placeItems: "center", minHeight: "100vh" }}>A carregar autenticacao...</div>;
  }

  if (authenticated) {
    return (
      <Dashboard
        loggedInEmail={loggedInEmail}
        loggedInFirstName={loggedInFirstName}
        loggedInLastName={loggedInLastName}
        onLogout={handleLogout}
      />
    );
  }

  return <AuthForm {...authFormProps} />;
}

export default App;



