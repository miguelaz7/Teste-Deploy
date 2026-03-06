import { useState } from "react";
import "./App.css";

function App() {
  const [email, setEmail] = useState("");
  const [mensagem, setMensagem] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();
    await enviarEmail();
  };

  const enviarEmail = async () => {
    try {
      const response = await fetch("http://localhost:8080/api/emails", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
      });

      if (response.ok) {
        setMensagem("Email guardado com sucesso!");
        setEmail("");
      } else {
        setMensagem("Erro ao guardar o email.");
      }
    } catch (error) {
      setMensagem("Não foi possível ligar ao servidor.");
    }
  };

  return (
    <div className="app-shell">
      <div className="app-card">
        <p className="eyebrow">Bem-vindo ao TickeTUB</p>
        <h1>Gestão de Bilhética</h1>
        <p className="subtitle">Informação sempre à tua disposição.</p>
        <form onSubmit={handleSubmit}>
          <input
            type="email"
            placeholder="Insere o teu email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            onInvalid={(e) => e.target.setCustomValidity("Insira um email valido")}
            onInput={(e) => e.target.setCustomValidity("")}
            className="email-input"
          />
          <button type="submit" className="submit-button">
            Entrar
          </button>
        </form>
        {mensagem && <p className="message">{mensagem}</p>}
      </div>
    </div>
  );
}

export default App;