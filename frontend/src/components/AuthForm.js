function AuthForm({ onLogin }) {
  return (
    <div className="app-shell">
      <BackgroundGrid />
      <DiagonalRoad />
      <div className="login-container">
        {/* Left Side Branding */}
        <div className="login-branding">
          <h1 className="brand-logo">Ticke<span>TUB</span></h1>
          <h2 className="brand-slogan">O seu software de <br /> Gestão de Bilhética</h2>
          <p className="brand-sub">Informação à sua disposição!</p>
        </div>

        {/* Right Side Card with Entry Button */}
        <div className="app-card" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', textAlign: 'center' }}>
          <h2 className="card-title">
            Bem-Vindo ao <span className="text-ticketub">TickeTUB</span>!
          </h2>

          <p style={{ color: 'var(--text-muted)', marginBottom: '2rem' }}>
            Clique no botão abaixo para entrar na plataforma.
          </p>

          <button
            type="button"
            className="submit-button"
            onClick={onLogin}
            style={{ width: '100%', maxWidth: '300px', fontSize: '1.2rem', padding: '1rem' }}
          >
            Entrar
          </button>

          <footer style={{ marginTop: '2rem', fontSize: '0.85rem', color: 'var(--text-muted)', opacity: 0.7 }}>

          </footer>
        </div>
      </div>
    </div>
  );
}

function BackgroundGrid() {
  return (
    <div className="background-grid">
      <svg width="100%" height="100%" viewBox="0 0 800 600" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="grid-grad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="var(--brand-blue)" stopOpacity="0.05" />
            <stop offset="100%" stopColor="var(--brand-green)" stopOpacity="0.05" />
          </linearGradient>
          <pattern id="tech-grid" x="0" y="0" width="100" height="100" patternUnits="userSpaceOnUse">
            <path d="M 100 0 L 0 0 0 100" fill="none" stroke="currentColor" strokeWidth="0.3" opacity="0.15" />
            <circle cx="0" cy="0" r="1.5" fill="currentColor" opacity="0.3" />
          </pattern>
        </defs>

        {/* Animated Grid Shell */}
        <rect width="100%" height="100%" fill="url(#grid-grad)" />
        <g className="animated-grid">
          <rect width="100%" height="100%" fill="url(#tech-grid)" color="#8da4b3" />
        </g>

        {/* Floating Particles/Data Bits */}
        <g className="data-particles">
          <circle cx="100" cy="150" r="1.5" fill="var(--brand-green)" opacity="0.4">
            <animate attributeName="opacity" values="0.2;0.6;0.2" dur="3s" repeatCount="indefinite" />
          </circle>
          <circle cx="650" cy="100" r="2" fill="var(--brand-blue)" opacity="0.3">
            <animate attributeName="opacity" values="0.1;0.5;0.1" dur="4s" repeatCount="indefinite" />
          </circle>
          <circle cx="400" cy="500" r="1.2" fill="var(--brand-green)" opacity="0.2">
            <animate attributeName="opacity" values="0.2;0.8;0.2" dur="2.5s" repeatCount="indefinite" />
          </circle>
        </g>
      </svg>
    </div>
  );
}

function DiagonalRoad() {
  return (
    <div className="diagonal-road-wrapper">
      <div className="diagonal-road">
        <div className="road-surface"></div>
        {/* Neon Side Lines */}
        <div className="road-border-top"></div>
        <div className="road-border-bottom"></div>
        <div className="diagonal-road-lines"></div>
        {/* Tech Decor / Lights on road */}
        <div className="road-tech-decor">
          <div className="tech-light tech-light-1"></div>
          <div className="tech-light tech-light-2"></div>
          <div className="tech-light tech-light-3"></div>
        </div>
      </div>
      {/* Forward Bus (Right lane) */}
      <div className="bus-lane-right">
        <IsometricBus />
      </div>
      {/* Reverse Bus (Left lane) */}
      <div className="bus-lane-left">
        <IsometricBus />
      </div>
    </div>
  );
}

function IsometricBus() {
  return (
    <svg viewBox="0 0 240 160" width="240" height="160" xmlns="http://www.w3.org/2000/svg">
      <defs>
        {/* Gradients for more volume */}
        <linearGradient id="side-grad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#25769e" />
          <stop offset="100%" stopColor="#154d68" />
        </linearGradient>
        <linearGradient id="roof-grad" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="#3d97c6" />
          <stop offset="100%" stopColor="#2a82af" />
        </linearGradient>
        <linearGradient id="glass-grad" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="#eef2f3" />
          <stop offset="100%" stopColor="#8da4b3" opacity="0.8" />
        </linearGradient>
        <linearGradient id="stripe-grad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#82cc63" />
          <stop offset="100%" stopColor="#4f8a37" />
        </linearGradient>
        <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="1.5" result="blur" />
          <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>
      </defs>

      {/* Shadow remains similar but adjusted */}
      <ellipse cx="120" cy="88" rx="65" ry="28" fill="rgba(0,0,0,0.12)" transform="rotate(-26.565, 120, 88)" />

      <g>
        {/* Rear Wheels (Depth) */}
        <ellipse cx="72" cy="91" rx="14" ry="18" fill="#0c0c0c" />
        <ellipse cx="132" cy="61" rx="14" ry="18" fill="#0c0c0c" />

        {/* --- MAIN BODY --- */}
        {/* Interior/Back Wall (Visible through windows) */}
        <polygon points="95,85  185,40  185,45  95,90" fill="#0b2c3d" />

        {/* Right Side Panel */}
        <polygon points="90,120  195,67.5  195,27.5  90,80" fill="url(#side-grad)" />

        {/* Rear Panel */}
        <polygon points="45,97.5  90,120  90,80  45,57.5" fill="#133e54" />

        {/* Roof Panel */}
        <polygon points="90,80  195,27.5  150,5  45,57.5" fill="url(#roof-grad)" />

        {/* Brand Stripe TicketTUB (Green) */}
        <polygon points="90,108  195,55.5  195,50  90,102.5" fill="url(#stripe-grad)" />
        <polygon points="45,86  90,108  90,102.5  45,80.5" fill="#3d6c29" />


        {/* --- WINDOWS --- */}
        {/* Right Side Windows with Reflexes */}
        <polygon points="100,95  118,86  118,71  100,80" fill="url(#glass-grad)" />
        <polygon points="123,83.5  141,74.5  141,59.5  123,68.5" fill="url(#glass-grad)" />
        <polygon points="146,72  164,63  164,48  146,57" fill="url(#glass-grad)" />
        <polygon points="169,60.5  190,50  190,35  169,45.5" fill="url(#glass-grad)" />

        {/* Rear Window */}
        <polygon points="52,80  84,96  84,81  52,65" fill="#c8d4db" />

        {/* --- LIGHTS & DETAILS --- */}
        {/* Tail Lights */}
        <polygon points="48,90  53,92.5  53,86  48,83.5" fill="#ff4d4d" filter="url(#glow)" />
        <polygon points="82,107  87,109.5  87,103  82,100.5" fill="#ff4d4d" filter="url(#glow)" />

        {/* FRONT WHEELS (Detailed Jantes) */}
        {/* Rear Right Wheel */}
        <g transform="translate(108, 109)">
          <ellipse cx="0" cy="0" rx="12" ry="19" fill="#0a0a0a" />
          <ellipse cx="4" cy="2" rx="12" ry="19" fill="#1a1a1a" />
          <ellipse cx="4" cy="2" rx="6" ry="10" fill="#444" />
          <circle cx="4" cy="2" r="2" fill="#0c0c0c" />
        </g>

        {/* Front Right Wheel */}
        <g transform="translate(170, 78)">
          <ellipse cx="0" cy="0" rx="12" ry="19" fill="#0a0a0a" />
          <ellipse cx="4" cy="2" rx="12" ry="19" fill="#1a1a1a" />
          <ellipse cx="4" cy="2" rx="6" ry="10" fill="#444" />
          <circle cx="4" cy="2" r="2" fill="#0c0c0c" />
        </g>
      </g>
    </svg>
  );
}

export default AuthForm;
