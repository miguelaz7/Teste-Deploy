import { getPasswordStrength } from "../utils/passwordStrength";

function AuthForm({
  mode,
  loading,
  mensagem,
  fieldErrors,
  firstName,
  lastName,
  email,
  password,
  confirmPassword,
  showPassword,
  showConfirmPassword,
  onSubmit,
  onSetFirstName,
  onSetLastName,
  onSetEmail,
  onSetPassword,
  onSetConfirmPassword,
  onTogglePassword,
  onToggleConfirmPassword,
  onToggleMode,
  clearFieldError,
  markFieldError,
}) {
  const passwordStrength = getPasswordStrength(password);

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

        {/* Right Side Form */}
        <div className="app-card">
          <h2 className="card-title">
            {mode === "login" ? (
              <>Bem-Vindo de volta ao <span className="text-ticketub">TickeTUB</span>!</>
            ) : (
              <>Cria a tua conta no <span className="text-ticketub">TickeTUB</span>!</>
            )}
          </h2>

          <form onSubmit={onSubmit}>
            {mode === "register" && (
              <>
                <div className="input-wrapper with-icon">
                  <span className="input-icon"><UserIcon /></span>
                  <input
                    type="text"
                    placeholder="Primeiro nome"
                    value={firstName}
                    onChange={(event) => {
                      onSetFirstName(event.target.value);
                      clearFieldError("firstName");
                    }}
                    className={`email-input ${fieldErrors.firstName ? "has-error" : ""}`}
                    required
                  />
                  {fieldErrors.firstName && <span className="input-error-icon">!</span>}
                </div>

                <div className="input-wrapper with-icon">
                  <span className="input-icon"><UserIcon /></span>
                  <input
                    type="text"
                    placeholder="Apelido"
                    value={lastName}
                    onChange={(event) => {
                      onSetLastName(event.target.value);
                      clearFieldError("lastName");
                    }}
                    className={`email-input ${fieldErrors.lastName ? "has-error" : ""}`}
                    required
                  />
                  {fieldErrors.lastName && <span className="input-error-icon">!</span>}
                </div>
              </>
            )}

            <div className="input-wrapper with-icon">
              <span className="input-icon"><UserIcon /></span>
              <input
                type="email"
                placeholder={mode === "login" ? "you@example.pt" : "Insere o teu email"}
                value={email}
                onChange={(event) => {
                  onSetEmail(event.target.value);
                  clearFieldError("email");
                }}
                onInvalid={(event) => {
                  event.target.setCustomValidity("Insira um email valido");
                  markFieldError("email");
                }}
                onInput={(event) => {
                  event.target.setCustomValidity("");
                  clearFieldError("email");
                }}
                className={`email-input ${fieldErrors.email ? "has-error" : ""}`}
                required
              />
              {fieldErrors.email && <span className="input-error-icon">!</span>}
            </div>

            <div className="password-field input-wrapper with-icon">
              <span className="input-icon"><LockIcon /></span>
              <input
                type={showPassword ? "text" : "password"}
                placeholder={mode === "login" ? "**********" : "Insere a tua password"}
                value={password}
                onChange={(event) => {
                  onSetPassword(event.target.value);
                  clearFieldError("password");
                }}
                onInvalid={(event) => {
                  event.target.setCustomValidity(
                    mode === "register"
                      ? "A password deve ter pelo menos 6 caracteres"
                      : "Insere a tua password"
                  );
                  markFieldError("password");
                }}
                onInput={(event) => {
                  event.target.setCustomValidity("");
                  clearFieldError("password");
                }}
                className={`email-input password-input ${fieldErrors.password ? "has-error" : ""}`}
                minLength={mode === "register" ? 6 : undefined}
                required
              />
              {fieldErrors.password && <span className="input-error-icon password-error-icon">!</span>}
              <button
                type="button"
                className="password-toggle"
                onClick={onTogglePassword}
                aria-label={showPassword ? "Ocultar password" : "Mostrar password"}
                title={showPassword ? "Ocultar password" : "Mostrar password"}
              >
                <EyeIcon open={showPassword} />
              </button>
            </div>

            {mode === "register" && password.length > 0 && (
              <div className="password-strength" aria-live="polite">
                <div
                  className="strength-track"
                  role="progressbar"
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-valuenow={passwordStrength.progress}
                >
                  <div
                    className="strength-fill"
                    style={{
                      width: `${passwordStrength.progress}%`,
                      backgroundColor: passwordStrength.color,
                    }}
                  />
                </div>
                <p className="strength-text" style={{ color: passwordStrength.color }}>
                  Segurança da password: {passwordStrength.label}
                </p>
              </div>
            )}

            {mode === "register" && (
              <div className="password-field input-wrapper with-icon">
                <span className="input-icon"><LockIcon /></span>
                <input
                  type={showConfirmPassword ? "text" : "password"}
                  placeholder="Confirma a tua password"
                  value={confirmPassword}
                  onChange={(event) => {
                    onSetConfirmPassword(event.target.value);
                    clearFieldError("confirmPassword");
                  }}
                  onInvalid={(event) => {
                    event.target.setCustomValidity("Confirma a tua password");
                    markFieldError("confirmPassword");
                  }}
                  onInput={(event) => {
                    event.target.setCustomValidity("");
                    clearFieldError("confirmPassword");
                  }}
                  className={`email-input password-input ${fieldErrors.confirmPassword ? "has-error" : ""}`}
                  minLength={6}
                  required
                />
                {fieldErrors.confirmPassword && (
                  <span className="input-error-icon password-error-icon">!</span>
                )}
                <button
                  type="button"
                  className="password-toggle"
                  onClick={onToggleConfirmPassword}
                  aria-label={showConfirmPassword ? "Ocultar password" : "Mostrar password"}
                  title={showConfirmPassword ? "Ocultar password" : "Mostrar password"}
                >
                  <EyeIcon open={showConfirmPassword} />
                </button>
              </div>
            )}

            <button type="submit" className="submit-button" disabled={loading}>
              {loading ? "A processar..." : mode === "register" ? "Registar" : "Login"}
            </button>
          </form>

          {mode === "login" && (
            <div className="auth-footer-links">
              <a href="#" className="forgot-password">Esqueceu-se da palavra-passe</a>
              <p className="auth-switch-text">
                Não tem conta? <button type="button" className="auth-switch-link" onClick={onToggleMode}>Registe-se</button>
              </p>
            </div>
          )}
          {mode === "register" && (
            <div className="auth-footer-links">
              <p className="auth-switch-text">
                Já tens conta? <button type="button" className="auth-switch-link" onClick={onToggleMode}>Faz login</button>
              </p>
            </div>
          )}

          {mensagem && <p className="message">{mensagem}</p>}
        </div>
      </div>
    </div>
  );
}

function EyeIcon({ open }) {
  return (
    <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false" style={{ fill: "none", stroke: "currentColor" }}>
      <path
        d="M12 5C6.5 5 2 8.6 1 12c1 3.4 5.5 7 11 7s10-3.6 11-7c-1-3.4-5.5-7-11-7zm0 11.2a4.2 4.2 0 1 1 0-8.4 4.2 4.2 0 0 1 0 8.4z"
        fill="currentColor" stroke="none"
      />
      <circle cx="12" cy="12" r="2.1" fill="currentColor" stroke="none" />
      {!open && <path d="M4 20L20 4" strokeWidth="2" strokeLinecap="round" />}
    </svg>
  );
}

function UserIcon() {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
      <circle cx="12" cy="7" r="4"></circle>
    </svg>
  );
}

function LockIcon() {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
      <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
    </svg>
  );
}

function BackgroundGrid() {
  return (
    <div className="background-grid">
      <svg width="100%" height="100%" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <pattern id="grid-pattern" x="0" y="0" width="160" height="160" patternUnits="userSpaceOnUse">
            {/* Ticket at top left */}
            <g transform="translate(20, 25) scale(1.6)" fill="#d5dde4">
              <path d="M 2 4 C 4 4 5 5.5 5 7.5 C 5 9.5 4 11 2 11 L 2 13 C 2 14.1 2.9 15 4 15 L 20 15 C 21.1 15 22 14.1 22 13 L 22 11 C 20 11 19 9.5 19 7.5 C 19 5.5 20 4 22 4 L 22 2 C 22 0.9 21.1 0 20 0 L 4 0 C 2.9 0 2 0.9 2 2 Z" />
            </g>

            {/* Bus at top right */}
            <g transform="translate(100, 20) scale(1.6)" fill="#d5dde4">
              <path d="M 4 15 C 4 16.65 5.35 18 7 18 C 8.65 18 10 16.65 10 15 L 14 15 C 14 16.65 15.35 18 17 18 C 18.65 18 20 16.65 20 15 L 21 15 C 22.1 15 23 14.1 23 13 L 23 8 C 23 4 20 2 16 2 L 7 2 C 3 2 1 4 1 8 L 1 13 C 1 14.1 1.9 15 3 15 Z" />
            </g>

            {/* Bus at bottom left */}
            <g transform="translate(20, 100) scale(1.6)" fill="#d5dde4">
              <path d="M 4 15 C 4 16.65 5.35 18 7 18 C 8.65 18 10 16.65 10 15 L 14 15 C 14 16.65 15.35 18 17 18 C 18.65 18 20 16.65 20 15 L 21 15 C 22.1 15 23 14.1 23 13 L 23 8 C 23 4 20 2 16 2 L 7 2 C 3 2 1 4 1 8 L 1 13 C 1 14.1 1.9 15 3 15 Z" />
            </g>

            {/* Ticket at bottom right */}
            <g transform="translate(100, 105) scale(1.6)" fill="#d5dde4">
              <path d="M 2 4 C 4 4 5 5.5 5 7.5 C 5 9.5 4 11 2 11 L 2 13 C 2 14.1 2.9 15 4 15 L 20 15 C 21.1 15 22 14.1 22 13 L 22 11 C 20 11 19 9.5 19 7.5 C 19 5.5 20 4 22 4 L 22 2 C 22 0.9 21.1 0 20 0 L 4 0 C 2.9 0 2 0.9 2 2 Z" />
            </g>
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill="url(#grid-pattern)" />
      </svg>
    </div>
  );
}

function DiagonalRoad() {
  return (
    <div className="diagonal-road-wrapper">
      <div className="diagonal-road">
        <div className="diagonal-road-lines"></div>
      </div>
      <div className="bus-3d-animation">
        <IsometricBus />
      </div>
    </div>
  );
}

function IsometricBus() {
  return (
    <svg viewBox="0 0 240 160" width="240" height="160" xmlns="http://www.w3.org/2000/svg">
      {/* Bus Shadow */}
      <ellipse cx="120" cy="85" rx="55" ry="25" fill="rgba(0,0,0,0.15)" transform="rotate(-26.565, 120, 85)" />

      <g transform="translate(0, 0)">
        {/* Left Wheels (Background / Underneath) */}
        {/* Rear Left */}
        <ellipse cx="72" cy="91" rx="14" ry="18" fill="#111" />
        {/* Front Left */}
        <ellipse cx="132" cy="61" rx="14" ry="18" fill="#111" />

        {/* Right Face (Side Panel) */}
        <polygon points="90,120  190,70  190,30  90,80" fill="#1d6588" />

        {/* Rear Face (Back Panel) */}
        <polygon points="50,100  90,120  90,80  50,60" fill="#154d68" />

        {/* Top Face (Roof) */}
        <polygon points="90,80  190,30  150,10  50,60" fill="#2a82af" />

        {/* Green TicketTUB Stripe (Right Side) */}
        <polygon points="90,110  190,60  190,55  90,105" fill="#6bb14f" />

        {/* Green TicketTUB Stripe (Rear Side) */}
        <polygon points="50,90  90,110  90,105  50,85" fill="#4f8a37" />

        {/* Windows Right Side */}
        <polygon points="100,95  115,87.5  115,72.5  100,80" fill="#e8ebee" />
        <polygon points="120,85  135,77.5  135,62.5  120,70" fill="#e8ebee" />
        <polygon points="140,75  155,67.5  155,52.5  140,60" fill="#e8ebee" />
        <polygon points="160,65  180,55  180,40  160,50" fill="#e8ebee" />

        {/* Rear Window */}
        <polygon points="55,82.5  85,97.5  85,82.5  55,67.5" fill="#c8d4db" />

        {/* Rear Tail Lights */}
        <polygon points="54,92  58,94  58,89  54,87" fill="#e63946" />
        <polygon points="82,106  86,108  86,103  82,101" fill="#e63946" />

        {/* ======================================= */}
        {/* RIGHT WHEELS (Foreground)               */}
        {/* ======================================= */}
        {/* Rear Right Wheel */}
        <g>
          {/* Wheel depth (Backwards offset by dx=-4, dy=-2) */}
          <ellipse cx="108" cy="109" rx="11" ry="18" fill="#111" />
          {/* Connecting tread */}
          <polygon points="108,91 112,93 112,129 108,127" fill="#1a1a1a" />
          {/* Front face */}
          <ellipse cx="112" cy="111" rx="11" ry="18" fill="#222" />
          <ellipse cx="112" cy="111" rx="5" ry="10" fill="#777" />
          <ellipse cx="112" cy="111" rx="2.5" ry="5" fill="#111" />
        </g>

        {/* Front Right Wheel */}
        <g>
          {/* Wheel depth (Backwards offset by dx=-4, dy=-2) */}
          <ellipse cx="168" cy="79" rx="11" ry="18" fill="#111" />
          {/* Connecting tread */}
          <polygon points="168,61 172,63 172,99 168,97" fill="#1a1a1a" />
          {/* Front face */}
          <ellipse cx="172" cy="81" rx="11" ry="18" fill="#222" />
          <ellipse cx="172" cy="81" rx="5" ry="10" fill="#777" />
          <ellipse cx="172" cy="81" rx="2.5" ry="5" fill="#111" />
        </g>
      </g>
    </svg>
  );
}

export default AuthForm;
