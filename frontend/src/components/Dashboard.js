import { useState, useEffect } from "react";
import { getGreeting } from "../utils/greeting";
import BilhetesPanel from "./BilhetesPanel";

// Use direct string paths so Webpack doesn't crash if the files aren't in src/assets yet
const imgMateus = "/equipa/mateus.png";
const imgDiogo = "/equipa/diogo.png";
const imgJoao = "/equipa/joao.png";
const imgMiguel = "/equipa/miguel.png";
const imgAndreia = "/equipa/andreia.png";
const imgPedro = "/equipa/pedro.png";
const imgInes = "/equipa/ines.png";
const imgJDiogo = "/equipa/jdiogo.png";
const imgMariana = "/equipa/Mariana.png";
const imgRola = "/equipa/rola.png";
const imgMargarida = "/equipa/margarida.png";
const imgJoaoC = "/equipa/joaoc.png";

function Dashboard({ loggedInEmail, loggedInFirstName, loggedInLastName, onLogout }) {
  const [activeTab, setActiveTab] = useState(() => {
    return localStorage.getItem("activeTab") || "geral";
  });

  const [profileFirstName, setProfileFirstName] = useState(() => {
    return localStorage.getItem("profileFirstName") || loggedInFirstName || "";
  });
  const [profileLastName, setProfileLastName] = useState(() => {
    return localStorage.getItem("profileLastName") || loggedInLastName || "";
  });
  const [profileEmail, setProfileEmail] = useState(() => {
    return localStorage.getItem("profileEmail") || loggedInEmail || "";
  });
  const [profileMessage, setProfileMessage] = useState("");

  useEffect(() => {
    if (!localStorage.getItem("profileFirstName") && loggedInFirstName) {
      setProfileFirstName(loggedInFirstName);
    }
    if (!localStorage.getItem("profileLastName") && loggedInLastName) {
      setProfileLastName(loggedInLastName);
    }
    if (!localStorage.getItem("profileEmail") && loggedInEmail) {
      setProfileEmail(loggedInEmail);
    }
  }, [loggedInEmail, loggedInFirstName, loggedInLastName]);

  const fullName = `${profileFirstName} ${profileLastName}`.trim();

  // Save activeTab to localStorage on change
  useEffect(() => {
    localStorage.setItem("activeTab", activeTab);
  }, [activeTab]);

  // Avatar initial
  const initial = profileFirstName ? profileFirstName.charAt(0).toUpperCase() : "U";

  const handleSaveProfile = (event) => {
    event.preventDefault();

    const trimmedFirst = profileFirstName.trim();
    const trimmedLast = profileLastName.trim();
    const trimmedEmail = profileEmail.trim();

    if (!trimmedFirst || !trimmedLast || !trimmedEmail) {
      setProfileMessage("Preenche nome, apelido e email para guardar.");
      return;
    }

    localStorage.setItem("profileFirstName", trimmedFirst);
    localStorage.setItem("profileLastName", trimmedLast);
    localStorage.setItem("profileEmail", trimmedEmail);
    setProfileFirstName(trimmedFirst);
    setProfileLastName(trimmedLast);
    setProfileEmail(trimmedEmail);
    setProfileMessage("Informacoes pessoais atualizadas com sucesso.");
  };

  const teamData = [
    { name: "João Carmo", role: "Developer", img: imgJoao },
    { name: "Miguel Ângelo", role: "Developer", img: imgMiguel },
    { name: "Diogo Vieira", role: "Developer", img: imgDiogo },
    { name: "Mateus Pereira", role: "Developer", img: imgMateus },
    { name: "Andreia Lameira", role: "Analyst", img: imgAndreia },
    { name: "Pedro Cardoso", role: "Analyst", img: imgPedro },
    { name: "Inês Martins", role: "Analyst", img: imgInes },
    { name: "Margarida Rodrigues", role: "Analyst", img: imgMargarida },
    { name: "Mariana Esteves", role: "Project Manager", img: imgMariana },
    { name: "João Pinto", role: "Architect", img: imgRola },
    { name: "João Santos", role: "Tester", img: imgJDiogo },
    { name: "João Carvalho", role: "Tester", img: imgJoaoC },
  ];

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
          <button
            className={`nav-link ${activeTab === 'equipa' ? 'active' : ''}`}
            onClick={() => setActiveTab('equipa')}
          >
            <span className="nav-icon"><GroupIcon /></span>
            <span className="nav-text" style={{ lineHeight: '1.2' }}>Equipa de<br />Desenvolvimento</span>
          </button>
        </nav>

        <div className="sidebar-footer">
          <button
            type="button"
            className={`user-profile user-profile-button ${activeTab === 'perfil' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('perfil');
              setProfileMessage("");
            }}
            title="Ver e editar perfil"
          >
            <div className="avatar">{initial}</div>
            <div className="user-info">
              <span className="user-name">{fullName}</span>
              <span className="user-role">Editar perfil</span>
            </div>
          </button>
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

          {activeTab === 'equipa' && (
            <div className="team-section">
              <h2>A Nossa Equipa de Desenvolvimento</h2>
              <p className="team-subtitle">Conheça os criadores do TickeTUB.</p>

              <div className="team-grid">
                {teamData.map((member, idx) => (
                  <div className="team-card" key={idx}>
                    <div className="team-photo-wrapper">
                      <img src={member.img} alt={`Foto de ${member.name}`} className="team-photo" onError={(e) => { e.target.onerror = null; e.target.src = "https://via.placeholder.com/150"; }} />
                    </div>
                    <h3>{member.name}</h3>
                    <p>{member.role}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {activeTab === 'bilhetes' && (
            <BilhetesPanel />
          )}

          {activeTab === 'perfil' && (
            <section className="profile-panel">
              <h2>Informacoes Pessoais</h2>
              <p>Atualiza os teus dados de utilizador.</p>

              <form className="profile-form" onSubmit={handleSaveProfile}>
                <label>
                  Nome
                  <input
                    type="text"
                    value={profileFirstName}
                    onChange={(event) => setProfileFirstName(event.target.value)}
                    placeholder="O teu nome"
                  />
                </label>

                <label>
                  Apelido
                  <input
                    type="text"
                    value={profileLastName}
                    onChange={(event) => setProfileLastName(event.target.value)}
                    placeholder="O teu apelido"
                  />
                </label>

                <label>
                  Email
                  <input
                    type="email"
                    value={profileEmail}
                    onChange={(event) => setProfileEmail(event.target.value)}
                    placeholder="teu@email.com"
                  />
                </label>

                <button type="submit" className="profile-save-button">Guardar alteracoes</button>
              </form>

              {profileMessage && <p className="profile-message">{profileMessage}</p>}
            </section>
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

function GroupIcon() {
  return (
    <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
      <circle cx="9" cy="7" r="4"></circle>
      <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
      <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
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
