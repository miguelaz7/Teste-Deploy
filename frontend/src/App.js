import "./App.css";
import AuthForm from "./components/AuthForm";
import Dashboard from "./components/Dashboard";
import useAuthFlow from "./hooks/useAuthFlow";

function App() {
  const { authenticated, loggedInEmail, loggedInFirstName, loggedInLastName, handleLogout, authFormProps } = useAuthFlow();

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
