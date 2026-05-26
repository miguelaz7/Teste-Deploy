import React, { useState, useEffect, useCallback } from 'react';
import { getDadosAbertos } from '../../logica_do_sistema/services/odService';
import './Od.css';

const API = 'http://localhost:8080';

function ExportacaoDados() {
  const [dados, setDados] = useState(null);
  const [loading, setLoading] = useState(true);
  const [solicitando, setSolicitando] = useState(false);
  const [mensagem, setMensagem] = useState('');

  const fetchDados = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getDadosAbertos();
      setDados(data && typeof data === 'object' ? data : null);
    } catch {
      setDados(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchDados(); }, [fetchDados]);

  const handleSolicitarExportacao = async () => {
    setSolicitando(true);
    setMensagem('');
    try {
      const hoje = new Date().toISOString().split('T')[0];
      const h7 = new Date(Date.now() - 7 * 86400000).toISOString().split('T')[0];
      const res = await fetch(`${API}/api/ngsi-ld/exportacoes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ formato: 'JSON', periodoInicio: h7, periodoFim: hoje })
      });
      const result = await res.json();
      setMensagem(result.mensagem || `Exportação submetida (estado: ${result.estado || 'PENDENTE_DPO'})`);
      await fetchDados();
    } catch {
      setMensagem('Erro ao solicitar exportação.');
    } finally {
      setSolicitando(false);
    }
  };

  const handleDownload = () => {
    if (!dados) return;
    const rows = dados.registos && dados.registos.length > 0
      ? dados.registos
      : [{ dataInicio: dados.dataInicio, dataFim: dados.dataFim, totalRegistos: dados.totalRegistos, anonimizado: dados.anonimizado }];
    const headers = Object.keys(rows[0]);
    const csv = [
      headers.join(','),
      ...rows.map(r => headers.map(h => `"${String(r[h] ?? '').replace(/"/g, '""')}"`).join(','))
    ].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `dados_abertos_${dados.dataInicio || 'export'}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  };

  return (
    <div className="od-card">
      <div className="od-card-header">
        <h2>Exportação de Dados Abertos</h2>
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <button className="btn-resolver" onClick={handleSolicitarExportacao} disabled={solicitando}>
            <svg style={{ marginRight: '6px' }} className={solicitando ? "spinning-icon" : ""} width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21.2 15a8.85 8.85 0 0 0-1.2-4.5 9 9 0 1 0-3.8 6.8m-1.7-1.3L12 12m0 0l-2.5 2.5M12 12v9"/>
            </svg>
            {solicitando ? 'A solicitar...' : 'Nova Exportação'}
          </button>
          <button className="btn-download" onClick={handleDownload} disabled={!dados}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="7 10 12 15 17 10"></polyline>
              <line x1="12" y1="15" x2="12" y2="3"></line>
            </svg>
            Download CSV
          </button>
        </div>
      </div>

      <div className="od-card-body">
        {mensagem && (
          <div style={{ marginBottom: '1rem', padding: '0.75rem', background: '#eff6ff', borderRadius: 8, color: '#1e40af', fontSize: '0.875rem' }}>
            {mensagem}
          </div>
        )}
        {loading ? (
          <div className="od-empty-state-container">
            <div className="empty-state-icon-wrapper loading">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#3b82f6" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="spinning-icon">
                <line x1="12" y1="2" x2="12" y2="6"></line>
                <line x1="12" y1="18" x2="12" y2="22"></line>
                <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line>
                <line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line>
                <line x1="2" y1="12" x2="6" y2="12"></line>
                <line x1="18" y1="12" x2="22" y2="12"></line>
                <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line>
                <line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>
              </svg>
            </div>
            <h4 className="empty-state-title">A carregar datasets...</h4>
            <p className="empty-state-description">Por favor aguarde enquanto sincronizamos os dados de exportação.</p>
          </div>
        ) : !dados ? (
          <div className="od-empty-state-container">
            <div className="empty-state-icon-wrapper" style={{ backgroundColor: '#eff6ff' }}>
              <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#3b82f6" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
              </svg>
            </div>
            <h4 className="empty-state-title">Sem dados disponíveis</h4>
            <p className="empty-state-description">Não foi possível localizar dados de exportação para este período.</p>
          </div>
        ) : (
          <>
            <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
              <div className="historico-metric-box">
                <span className="historico-metric-label">Período</span>
                <span className="historico-metric-value">{dados.dataInicio} → {dados.dataFim}</span>
              </div>
              <div className="historico-metric-box">
                <span className="historico-metric-label">Total Registos</span>
                <span className="historico-metric-value">{dados.totalRegistos ?? 0}</span>
              </div>
              <div className="historico-metric-box">
                <span className="historico-metric-label">Anonimizado</span>
                <span className="historico-metric-value">{dados.anonimizado ? '✓ Sim' : 'Não'}</span>
              </div>
            </div>
            {dados.registos && dados.registos.length > 0 ? (
              <div className="od-table-container">
                <table className="od-table">
                  <thead>
                    <tr>{Object.keys(dados.registos[0]).map(h => <th key={h}>{h}</th>)}</tr>
                  </thead>
                  <tbody>
                    {dados.registos.slice(0, 50).map((r, i) => (
                      <tr key={i}>
                        {Object.keys(dados.registos[0]).map(h => <td key={h}>{String(r[h] ?? '—')}</td>)}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="od-empty-state-container">
                <div className="empty-state-icon-wrapper" style={{ backgroundColor: '#f0fdf4' }}>
                  <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#10b981" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                    <polyline points="22 4 12 14.01 9 11.01"></polyline>
                  </svg>
                </div>
                <h4 className="empty-state-title">Data Lake Ingerido</h4>
                <p className="empty-state-description" style={{ maxWidth: '480px' }}>
                  O Data Lake é populado automaticamente com cada validação ingerida. Usa "Nova Exportação" para solicitar ao DPO uma exportação aprovada. O botão Download CSV exporta os metadados do período actual.
                </p>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

export default ExportacaoDados;