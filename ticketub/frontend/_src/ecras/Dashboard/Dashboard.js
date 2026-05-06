import { useState, useEffect } from "react";
import { getGreeting } from "../../logica_do_sistema/utils/greeting";
import Mapa from "../../funcionalidades/map/Mapa";
import PainelGeral from "../../funcionalidades/dashboard/PainelGeral";
import ModuloBilhetes from "../../funcionalidades/ticketing/ModuloBilhetes";
import ProcuraTempoReal from "../../funcionalidades/analise/ProcuraTempoReal";
import HistoricoConsolidado from "../../funcionalidades/analise/HistoricoConsolidado";
import PainelAlertas from "../../funcionalidades/alertas/PainelAlertas";
import DetalheQuarentena from "../../funcionalidades/alertas/DetalheQuarentena";
import MatrizOD from "../../funcionalidades/od/MatrizOD";
import ExportacaoDados from "../../funcionalidades/od/ExportacaoDados";
import SimulacaoCenarios from "../../funcionalidades/planeamento/SimulacaoCenarios";
import IntegracaoERP from "../../funcionalidades/planeamento/IntegracaoERP";
import GestaoRGPD from "../../funcionalidades/rgpd/GestaoRGPD";

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
            className={`nav-link ${activeTab === 'mapa' ? 'active' : ''}`}
            onClick={() => setActiveTab('mapa')}
          >
            <span className="nav-icon"><MapIcon /></span>
            <span className="nav-text">Mapa</span>
          </button>
          <button
            className={`nav-link ${activeTab === 'analise' ? 'active' : ''}`}
            onClick={() => setActiveTab('analise')}
          >
            <span className="nav-icon"><ChartIcon /></span>
            <span className="nav-text">Análise</span>
          </button>
          <button
            className={`nav-link ${activeTab === 'historico' ? 'active' : ''}`}
            onClick={() => setActiveTab('historico')}
          >
            <span className="nav-icon"><ClockIcon /></span>
            <span className="nav-text">Histórico</span>
          </button>
          <button
            className={`nav-link ${activeTab === 'alertas' ? 'active' : ''}`}
            onClick={() => setActiveTab('alertas')}
          >
            <span className="nav-icon"><BellIcon /></span>
            <span className="nav-text">Alertas</span>
          </button>
          <button
            className={`nav-link ${activeTab === 'matriz-od' ? 'active' : ''}`}
            onClick={() => setActiveTab('matriz-od')}
          >
            <span className="nav-icon"><NetworkIcon /></span>
            <span className="nav-text">Matriz O-D</span>
          </button>
          <button
            className={`nav-link ${activeTab === 'exportacao' ? 'active' : ''}`}
            onClick={() => setActiveTab('exportacao')}
          >
            <span className="nav-icon"><DownloadCloudIcon /></span>
            <span className="nav-text">Exportação</span>
          </button>
          <button
            className={`nav-link ${activeTab === 'planeamento' ? 'active' : ''}`}
            onClick={() => setActiveTab('planeamento')}
          >
            <span className="nav-icon"><TargetIcon /></span>
            <span className="nav-text">Planeamento</span>
          </button>
          <button
            className={`nav-link ${activeTab === 'erp' ? 'active' : ''}`}
            onClick={() => setActiveTab('erp')}
          >
            <span className="nav-icon"><ServerIcon /></span>
            <span className="nav-text">ERP</span>
          </button>
          <button
            className={`nav-link ${activeTab === 'rgpd' ? 'active' : ''}`}
            onClick={() => setActiveTab('rgpd')}
          >
            <span className="nav-icon"><ShieldIcon /></span>
            <span className="nav-text">RGPD</span>
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



          {activeTab === 'mapa' && (
            <Mapa />
          )}

          {activeTab === 'analise' && (
            <ProcuraTempoReal />
          )}

          {activeTab === 'historico' && (
            <HistoricoConsolidado />
          )}

          {activeTab === 'alertas' && (
            <div className="alertas-wrapper">
              <PainelAlertas />
              <DetalheQuarentena />
            </div>
          )}

          {activeTab === 'matriz-od' && (
            <MatrizOD />
          )}

          {activeTab === 'exportacao' && (
            <ExportacaoDados />
          )}

          {activeTab === 'planeamento' && (
            <SimulacaoCenarios />
          )}

          {activeTab === 'erp' && (
            <IntegracaoERP />
          )}

          {activeTab === 'rgpd' && (
            <GestaoRGPD />
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

function ChartIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <line x1="18" y1="20" x2="18" y2="10"></line>
      <line x1="12" y1="20" x2="12" y2="4"></line>
      <line x1="6" y1="20" x2="6" y2="14"></line>
    </svg>
  );
}

function ClockIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10"></circle>
      <polyline points="12 6 12 12 16 14"></polyline>
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

function NetworkIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="18" cy="5" r="3"></circle>
      <circle cx="6" cy="12" r="3"></circle>
      <circle cx="18" cy="19" r="3"></circle>
      <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line>
      <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line>
    </svg>
  );
}

function DownloadCloudIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="8 17 12 21 16 17"></polyline>
      <line x1="12" y1="12" x2="12" y2="21"></line>
      <path d="M20.88 18.09A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.29"></path>
    </svg>
  );
}

function TargetIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10"></circle>
      <circle cx="12" cy="12" r="6"></circle>
      <circle cx="12" cy="12" r="2"></circle>
    </svg>
  );
}

function ServerIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="2" width="20" height="8" rx="2" ry="2"></rect>
      <rect x="2" y="14" width="20" height="8" rx="2" ry="2"></rect>
      <line x1="6" y1="6" x2="6.01" y2="6"></line>
      <line x1="6" y1="18" x2="6.01" y2="18"></line>
    </svg>
  );
}

function ShieldIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
    </svg>
  );
}

export default Dashboard;


