import { useState, useEffect } from "react";
import { getGreeting } from "../../logica_do_sistema/utils/greeting";
import Mapa from "../../funcionalidades/map/Mapa";
import PainelGeral from "../../funcionalidades/dashboard/PainelGeral";
import ModuloBilhetes from "../../funcionalidades/ticketing/ModuloBilhetes";

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
            <span className="nav-icon"><MailIcon /></span>
            <span className="nav-text">Tickets</span>
          </button>
          <button
            className={`nav-link ${activeTab === 'mapa' ? 'active' : ''}`}
            onClick={() => setActiveTab('mapa')}
          >
            <span className="nav-icon"><MapIcon /></span>
            <span className="nav-text">Mapa</span>
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
              <p className="welcome-subtitle">Aqui está o resumo da tua ingestão de dados.</p>
            </div>
          </div>
        )}

        {/* The content area where the tiles or tables will render based on the active tab */}
        <div className="content-area">
          {activeTab === 'geral' && (
            <PainelGeral />
          )}



          {activeTab === 'bilhetes' && (
            <ModuloBilhetes />
          )}

          {activeTab === 'notificacoes' && (
            <div className="welcome-card">
              <h2>Tickets</h2>
              <p>Não há novos Tickets.</p>
            </div>
          )}

          {activeTab === 'mapa' && (
            <Mapa />
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

function MapIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6"></polygon>
      <line x1="8" y1="2" x2="8" y2="18"></line>
      <line x1="16" y1="6" x2="16" y2="22"></line>
    </svg>
  );
}

function MailIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
      <polyline points="22,6 12,13 2,6"></polyline>
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

function SettingsIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="3"></circle>
      <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
    </svg>
  );
}

function ListIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <line x1="8" y1="6" x2="21" y2="6"></line>
      <line x1="8" y1="12" x2="21" y2="12"></line>
      <line x1="8" y1="18" x2="21" y2="18"></line>
      <line x1="3" y1="6" x2="3.01" y2="6"></line>
      <line x1="3" y1="12" x2="3.01" y2="12"></line>
      <line x1="3" y1="18" x2="3.01" y2="18"></line>
    </svg>
  );
}

export default Dashboard;


