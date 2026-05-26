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
    <div className="od-card" style={{ backgroundColor: 'transparent', border: 'none', boxShadow: 'none', padding: 0 }}>
      <div className="od-card-header" style={{ marginBottom: '1rem', borderBottom: 'none' }}>
        <h2>Exportação de Dados Abertos</h2>
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <button className="btn-resolver" onClick={handleSolicitarExportacao} disabled={solicitando}>
            {solicitando ? 'A solicitar...' : 'Nova Exportação'}
          </button>
          <button className="btn-download" onClick={handleDownload} disabled={!dados}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
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
          <div className="od-empty-state">A carregar datasets...</div>
        ) : !dados ? (
          <div className="od-empty-state">Sem dados disponíveis</div>
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
              <div className="od-empty-state">
                O Data Lake é populado automaticamente com cada validação ingerida.
                Usa "Nova Exportação" para solicitar ao DPO uma exportação aprovada.
                O botão Download CSV exporta os metadados do período actual.
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

export default ExportacaoDados;