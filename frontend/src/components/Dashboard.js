import { getGreeting } from "../utils/greeting";

function Dashboard({ loggedInEmail, loggedInFirstName, loggedInLastName, onLogout }) {
  const fullName = `${loggedInFirstName} ${loggedInLastName}`.trim();

  return (
    <div className="dashboard-shell">
      <div className="dashboard-card">
        <div className="dashboard-header">
          <div>
            <p className="eyebrow">TUBInsight</p>
            <h1>{getGreeting()}, {fullName || "bem-vindo"}.</h1>
            <p className="subtitle">Sessao ativa para {loggedInEmail}</p>
          </div>
          <button type="button" className="logout-button" onClick={onLogout}>
            Terminar sessao
          </button>
        </div>

        <div className="dashboard-grid">
          <button type="button" className="dashboard-tile">
            <h2>Painel Geral</h2>
            <p>Consulta o resumo de bilhetes, utilizadores e atividade recente.</p>
          </button>

          <button type="button" className="dashboard-tile">
            <h2>Bilhetes</h2>
            <p>Cria e gere bilhetes digitais de forma rapida e centralizada.</p>
          </button>

          <button type="button" className="dashboard-tile">
            <h2>Notificacoes</h2>
            <p>Acompanha alertas importantes e mensagens do sistema.</p>
          </button>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
