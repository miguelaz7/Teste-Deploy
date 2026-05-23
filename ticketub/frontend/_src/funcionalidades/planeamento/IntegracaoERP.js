import React, { useState, useEffect, useCallback } from 'react';
import { getDadosFinanceiros, gerarParaERP } from '../../logica_do_sistema/services/planeamentoService';
import './Planeamento.css';

function IntegracaoERP() {
  const [financas, setFinancas] = useState(null);
  const [historico, setHistorico] = useState([]);
  const [loadingList, setLoadingList] = useState(true);
  const [loadingGerar, setLoadingGerar] = useState(false);

  const fetchFinancas = useCallback(async () => {
    setLoadingList(true);
    try {
      const data = await getDadosFinanceiros();
      // Backend devolve { perioDias, geradoEm, porLinha, porTipoTitulo }
      setFinancas(data && typeof data === 'object' && !Array.isArray(data) ? data : null);
    } catch {
      setFinancas(null);
    } finally {
      setLoadingList(false);
    }
  }, []);

  useEffect(() => { fetchFinancas(); }, [fetchFinancas]);

  const handleGerarERP = async () => {
    setLoadingGerar(true);
    try {
      const payload = {
        routeId: '12',
        periodoInicio: new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0],
        periodoFim:    new Date().toISOString().split('T')[0],
        geradoPor:     'utilizador'
      };
      const resultado = await gerarParaERP(payload);
      // Adicionar ao histórico local
      setHistorico(prev => [resultado, ...prev]);
      await fetchFinancas();
    } catch (err) {
      console.error('Erro ao gerar dados para ERP', err);
    } finally {
      setLoadingGerar(false);
    }
  };

  const renderBadge = (estado) => {
    const s = String(estado || '').toUpperCase();
    if (s === 'ENVIADO' || s === 'SUCESSO')
      return <span className="badge-estado estado-sucesso">{s}</span>;
    if (s === 'FALHA' || s === 'ERRO')
      return <span className="badge-estado estado-falha">{s}</span>;
    return <span className="badge-estado estado-pendente">{s || 'PENDENTE'}</span>;
  };

  return (
    <div className="planeamento-container">
      <div className="planeamento-card">
        <div className="planeamento-card-header">
          <h2>Integração Financeira ERP</h2>
          <button className="btn-primary" onClick={handleGerarERP} disabled={loadingGerar}>
            {loadingGerar ? 'A gerar...' : 'Gerar para ERP'}
          </button>
        </div>

        <div className="planeamento-card-body">
          {loadingList ? (
            <div className="planeamento-empty-state">A carregar dados financeiros...</div>
          ) : !financas ? (
            <div className="planeamento-empty-state">Sem dados disponíveis.</div>
          ) : (
            <>
              {/* Resumo geral */}
              <div style={{ marginBottom: '1.5rem' }}>
                <h3 style={{ marginBottom: '1rem', fontSize: '1.1rem' }}>Resumo — Últimos {financas.perioDias} dias</h3>
                <div className="historico-metrics-grid">
                  <div className="historico-metric-box">
                    <span className="historico-metric-label">Gerado em</span>
                    <span className="historico-metric-value">
                      {financas.geradoEm ? new Date(financas.geradoEm).toLocaleString('pt-PT') : '—'}
                    </span>
                  </div>
                </div>
              </div>

              {/* Por Linha */}
              {financas.porLinha && financas.porLinha.length > 0 && (
                <div style={{ marginBottom: '1.5rem' }}>
                  <h3 style={{ marginBottom: '1rem', fontSize: '1.1rem' }}>Validações por Linha</h3>
                  <div className="planeamento-table-container">
                    <table className="planeamento-table">
                      <thead>
                        <tr><th>Linha</th><th>Total Validações</th></tr>
                      </thead>
                      <tbody>
                        {financas.porLinha.map((r, i) => (
                          <tr key={i}>
                            <td>{r.routeId}</td>
                            <td>{r.total}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* Por Tipo de Título */}
              {financas.porTipoTitulo && financas.porTipoTitulo.length > 0 && (
                <div style={{ marginBottom: '1.5rem' }}>
                  <h3 style={{ marginBottom: '1rem', fontSize: '1.1rem' }}>Validações por Tipo de Título</h3>
                  <div className="planeamento-table-container">
                    <table className="planeamento-table">
                      <thead>
                        <tr><th>Tipo de Título</th><th>Total</th></tr>
                      </thead>
                      <tbody>
                        {financas.porTipoTitulo.map((r, i) => (
                          <tr key={i}>
                            <td>{r.tipoTitulo}</td>
                            <td>{r.total}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </>
          )}

          {/* Histórico de exportações geradas nesta sessão */}
          {historico.length > 0 && (
            <div>
              <h3 style={{ marginBottom: '1rem', fontSize: '1.1rem' }}>Exportações Geradas</h3>
              <div className="planeamento-table-container">
                <table className="planeamento-table">
                  <thead>
                    <tr><th>Versão</th><th>Validações</th><th>Receita</th><th>Estado</th></tr>
                  </thead>
                  <tbody>
                    {historico.map((r, i) => (
                      <tr key={i}>
                        <td style={{ fontSize: '0.75rem' }}>{r.versaoExportacao}</td>
                        <td>{r.totalValidacoes}</td>
                        <td>{r.receitaEstimada ? `${r.receitaEstimada}€` : '—'}</td>
                        <td>{renderBadge(r.estadoERP)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default IntegracaoERP;