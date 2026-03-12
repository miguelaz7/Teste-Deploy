import { useMemo, useState } from "react";
import useBilhetesRealtime from "../hooks/useBilhetesRealtime";

function formatPrice(value) {
  const numeric = Number(value);

  if (Number.isNaN(numeric)) {
    return "-";
  }

  return new Intl.NumberFormat("pt-PT", {
    style: "currency",
    currency: "EUR",
  }).format(numeric);
}

function formatDateTime(isoValue) {
  if (!isoValue) {
    return "-";
  }

  const date = new Date(isoValue);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  return new Intl.DateTimeFormat("pt-PT", {
    dateStyle: "short",
    timeStyle: "medium",
  }).format(date);
}

export default function BilhetesPanel() {
  const [codigo, setCodigo] = useState("");
  const [nome, setNome] = useState("");
  const [minPreco, setMinPreco] = useState("");
  const [maxPreco, setMaxPreco] = useState("");
  const [soDisponiveis, setSoDisponiveis] = useState(false);

  const filters = useMemo(
    () => ({
      codigo: codigo.trim(),
      nome: nome.trim(),
      minPreco: minPreco.trim(),
      maxPreco: maxPreco.trim(),
      soDisponiveis,
    }),
    [codigo, nome, minPreco, maxPreco, soDisponiveis]
  );

  const { bilhetes, loading, error, isLive, lastUpdatedAt, refresh } = useBilhetesRealtime(filters);

  const parsedMinPreco = minPreco.trim() === "" ? null : Number(minPreco);
  const parsedMaxPreco = maxPreco.trim() === "" ? null : Number(maxPreco);
  const invalidPriceRange =
    parsedMinPreco !== null &&
    parsedMaxPreco !== null &&
    !Number.isNaN(parsedMinPreco) &&
    !Number.isNaN(parsedMaxPreco) &&
    parsedMinPreco > parsedMaxPreco;

  const bilhetesFiltrados = useMemo(() => {
    return bilhetes.filter((bilhete) => {
      const codigoValue = (bilhete.codigo || "").toLowerCase();
      const nomeValue = (bilhete.nome || "").toLowerCase();
      const precoValue = Number(bilhete.preco);
      const quantidadeValue = Number(bilhete.quantidade) || 0;

      if (filters.codigo && !codigoValue.includes(filters.codigo.toLowerCase())) {
        return false;
      }

      if (filters.nome && !nomeValue.includes(filters.nome.toLowerCase())) {
        return false;
      }

      if (filters.soDisponiveis && quantidadeValue <= 0) {
        return false;
      }

      if (parsedMinPreco !== null && !Number.isNaN(parsedMinPreco) && precoValue < parsedMinPreco) {
        return false;
      }

      if (parsedMaxPreco !== null && !Number.isNaN(parsedMaxPreco) && precoValue > parsedMaxPreco) {
        return false;
      }

      return true;
    });
  }, [bilhetes, filters, parsedMinPreco, parsedMaxPreco]);

  const totalStockFiltrado = useMemo(
    () => bilhetesFiltrados.reduce((sum, item) => sum + (Number(item.quantidade) || 0), 0),
    [bilhetesFiltrados]
  );

  const limparFiltros = () => {
    setCodigo("");
    setNome("");
    setMinPreco("");
    setMaxPreco("");
    setSoDisponiveis(false);
  };

  return (
    <section className="bilhetes-panel">
      <header className="bilhetes-header">
        <div>
          <h2>Gestao de Bilhetes</h2>
          <p>Atualizado automaticamente quando o CSV muda no backend.</p>
        </div>

        <div className="bilhetes-status-wrap">
          <span className={`live-badge ${isLive ? "online" : "offline"}`}>
            {isLive ? "Ligacao em tempo real ativa" : "A reconectar stream"}
          </span>
          <button type="button" className="manual-refresh" onClick={refresh}>
            Atualizar agora
          </button>
        </div>
      </header>

      <div className="bilhetes-metrics">
        <article className="metric-card">
          <span className="metric-label">Resultados</span>
          <strong className="metric-value">{bilhetesFiltrados.length}</strong>
        </article>
        <article className="metric-card">
          <span className="metric-label">Stock total</span>
          <strong className="metric-value">{totalStockFiltrado}</strong>
        </article>
        <article className="metric-card">
          <span className="metric-label">Ultima leitura</span>
          <strong className="metric-value metric-date">{formatDateTime(lastUpdatedAt)}</strong>
        </article>
      </div>

      {invalidPriceRange && (
        <p className="bilhetes-feedback bilhetes-error">O preco minimo nao pode ser maior do que o preco maximo.</p>
      )}

      <div className="bilhetes-filters">
        <div className="filter-field">
          <label htmlFor="filter-codigo">Codigo</label>
          <input
            id="filter-codigo"
            type="text"
            placeholder="Ex: B001"
            value={codigo}
            onChange={(event) => setCodigo(event.target.value)}
          />
        </div>

        <div className="filter-field">
          <label htmlFor="filter-nome">Nome</label>
          <input
            id="filter-nome"
            type="text"
            placeholder="Ex: Mensal"
            value={nome}
            onChange={(event) => setNome(event.target.value)}
          />
        </div>

        <div className="filter-field">
          <label htmlFor="filter-min-preco">Preco minimo</label>
          <input
            id="filter-min-preco"
            type="number"
            min="0"
            step="0.01"
            placeholder="0.00"
            value={minPreco}
            onChange={(event) => setMinPreco(event.target.value)}
          />
        </div>

        <div className="filter-field">
          <label htmlFor="filter-max-preco">Preco maximo</label>
          <input
            id="filter-max-preco"
            type="number"
            min="0"
            step="0.01"
            placeholder="999.99"
            value={maxPreco}
            onChange={(event) => setMaxPreco(event.target.value)}
          />
        </div>

        <label className="filter-checkbox">
          <input
            type="checkbox"
            checked={soDisponiveis}
            onChange={(event) => setSoDisponiveis(event.target.checked)}
          />
          So com stock disponivel
        </label>

        <button type="button" className="clear-filters-button" onClick={limparFiltros}>
          Limpar filtros
        </button>
      </div>

      {loading && <p className="bilhetes-feedback">A carregar bilhetes...</p>}
      {!loading && error && <p className="bilhetes-feedback bilhetes-error">{error}</p>}

      {!loading && !error && (
        <div className="bilhetes-table-wrap">
          <table className="bilhetes-table">
            <thead>
              <tr>
                <th>Codigo</th>
                <th>Nome</th>
                <th>Preco</th>
                <th>Quantidade</th>
                <th>Atualizado em</th>
              </tr>
            </thead>
            <tbody>
              {bilhetes.length === 0 && (
                <tr>
                  <td colSpan={5}>Sem bilhetes para mostrar.</td>
                </tr>
              )}

              {bilhetes.length > 0 && bilhetesFiltrados.length === 0 && (
                <tr>
                  <td colSpan={5}>Nenhum bilhete corresponde aos filtros.</td>
                </tr>
              )}

              {bilhetesFiltrados.map((bilhete) => (
                <tr key={bilhete.id || bilhete.codigo}>
                  <td>{bilhete.codigo || "-"}</td>
                  <td>{bilhete.nome || "-"}</td>
                  <td>{formatPrice(bilhete.preco)}</td>
                  <td>{bilhete.quantidade ?? "-"}</td>
                  <td>{formatDateTime(bilhete.atualizadoEm)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
