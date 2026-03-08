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
      <div className="app-card">
        <p className="eyebrow">Bem-vindo a TUBInsight</p>
        <h1>Gestao de Bilhetica</h1>
        <p className="subtitle">{mode === "login" ? "Entra na tua conta." : "Cria a tua conta."}</p>

        <form onSubmit={onSubmit}>
          {mode === "register" && (
            <>
              <div className="input-wrapper">
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

              <div className="input-wrapper">
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

          <div className="input-wrapper">
            <input
              type="email"
              placeholder="Insere o teu email"
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

          <div className="password-field">
            <input
              type={showPassword ? "text" : "password"}
              placeholder="Insere a tua password"
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
                Seguranca da password: {passwordStrength.label}
              </p>
            </div>
          )}

          {mode === "register" && (
            <div className="password-field">
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
            {loading ? "A processar..." : mode === "register" ? "Registar" : "Entrar"}
          </button>
        </form>

        <p className="auth-switch-text">
          {mode === "login" ? "Ainda nao tens conta?" : "Ja tens conta?"}{" "}
          <button type="button" className="auth-switch-link" onClick={onToggleMode}>
            {mode === "login" ? "Regista-te" : "Faz login"}
          </button>
        </p>

        {mensagem && <p className="message">{mensagem}</p>}
      </div>
    </div>
  );
}

function EyeIcon({ open }) {
  return (
    <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false">
      <path
        d="M12 5C6.5 5 2 8.6 1 12c1 3.4 5.5 7 11 7s10-3.6 11-7c-1-3.4-5.5-7-11-7zm0 11.2a4.2 4.2 0 1 1 0-8.4 4.2 4.2 0 0 1 0 8.4z"
        fill="currentColor"
      />
      <circle cx="12" cy="12" r="2.1" fill="currentColor" />
      {!open && <path d="M4 20L20 4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />}
    </svg>
  );
}

export default AuthForm;
