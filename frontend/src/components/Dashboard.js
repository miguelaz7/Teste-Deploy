import { useState, useEffect } from "react";
import { getGreeting } from "../utils/greeting";

// Use direct string paths so Webpack doesn't crash if the files aren't in src/assets yet


function Dashboard({ loggedInEmail, loggedInFirstName, loggedInLastName, onLogout }) {
  const [activeTab, setActiveTab] = useState(() => {
    return localStorage.getItem("activeTab") || "geral";
  });

  const fullName = `${loggedInFirstName} ${loggedInLastName}`.trim();

  // Save activeTab to localStorage on change
  useEffect(() => {
    localStorage.setItem("activeTab", activeTab);
  }, [activeTab]);

  // Avatar initial
  const initial = loggedInFirstName ? loggedInFirstName.charAt(0).toUpperCase() : "U";



  return (
    <div className="dashboard-layout">
      {/* Sidebar Component */}
      <aside className="sidebar">
        <div className="sidebar-header" onClick={() => setActiveTab('geral')} style={{ cursor: 'pointer' }}>
          <strong className="sidebar-brand">
            Ticke
            <span>TUB</span>
          </strong>
          <strong className="sidebar-brand-full">
            Ticke<span>TUB</span>
          </strong>
        </div>

        <nav className="sidebar-nav">
          <button
            className={`nav-link ${activeTab === 'geral' ? 'active' : ''}`}
            onClick={() => setActiveTab('geral')}
          >
            <span className="nav-icon"><HomeIcon /></span>
            <span className="nav-text">Painel Geral</span>
          </button>
          <button
            className={`nav-link ${activeTab === 'bilhetes' ? 'active' : ''}`}
            onClick={() => setActiveTab('bilhetes')}
          >
            <span className="nav-icon"><TicketIcon /></span>
            <span className="nav-text">Bilhetes</span>
          </button>
          <button
            className={`nav-link ${activeTab === 'notificacoes' ? 'active' : ''}`}
            onClick={() => setActiveTab('notificacoes')}
          >
            <span className="nav-icon"><BellIcon /></span>
            <span className="nav-text">Notificações</span>
          </button>

        </nav>

        <div className="sidebar-footer">
          <div className="user-profile">
            <div className="avatar">{initial}</div>
            <div className="user-info">
              <span className="user-name">{fullName}</span>
            </div>
          </div>
          <button type="button" className="logout-button sidebar-logout" onClick={onLogout} title="Terminar sessão">
            <span className="nav-icon"><LogoutIcon /></span>
            <span className="nav-text">Sair</span>
          </button>
        </div>
      </aside>

      {/* Main Content Component */}
      <main className="dashboard-main">
        {activeTab === 'geral' && (
          <div className="dashboard-header">
            <div>
              <h1>{getGreeting()}, {fullName || "bem-vindo"}.</h1>
              <p className="subtitle">Sessão ativa para {loggedInEmail}</p>
            </div>
          </div>
        )}

        {/* The content area where the tiles or tables will render based on the active tab */}
        <div className="content-area">
          {activeTab === 'geral' && (
            <div className="welcome-card">
              <h2>Bem-vindo ao Novo Painel</h2>
              <p>Selecione uma opção no menu lateral para começar a gerir a sua bilhética.</p>
            </div>
          )}



          {activeTab === 'bilhetes' && (
            <div className="welcome-card">
              <h2>Módulo de Bilhetes</h2>
              <p>Funcionalidade em desenvolvimento.</p>
            </div>
          )}

          {activeTab === 'notificacoes' && (
            <div className="welcome-card">
              <h2>Notificações</h2>
              <p>Não há novas notificações.</p>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

// Simple inline SVG Icons for the Sidebar
function HomeIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
      <polyline points="9 22 9 12 15 12 15 22"></polyline>
    </svg>
  );
}

function TicketIcon() {
  return (
    <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ marginTop: '2px' }}>
      <path d="M4 7V5c0-1.1.9-2 2-2h12c1.1 0 2 .9 2 2v2c-1.1 0-2 .9-2 2s.9 2 2 2v2c0 1.1-.9 2-2 2H6c-1.1 0-2-.9-2-2v-2c1.1 0 2-.9 2-2s-.9-2-2-2z"></path>
      <line x1="12" y1="8" x2="12" y2="16" strokeDasharray="2 2"></line>
    </svg>
  );
}

function BellIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
      <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
    </svg>
  );
}



function LogoutIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
      <polyline points="16 17 21 12 16 7"></polyline>
      <line x1="21" y1="12" x2="9" y2="12"></line>
    </svg>
  );
}

export default Dashboard;
