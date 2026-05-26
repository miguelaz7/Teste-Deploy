import React, { useState, useEffect, useCallback } from 'react';
import { getFluxos } from '../../logica_do_sistema/services/odService';
import './Od.css';

const API = 'http://localhost:8080';

function MatrizOD() {
  const [fluxos, setFluxos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [calculando, setCalculando] = useState(false);
  const [mensagem, setMensagem] = useState('');

  const fetchFluxos = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getFluxos();
      setFluxos(Array.isArray(data) ? data : []);
    } catch {
      setFluxos([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchFluxos(); }, [fetchFluxos]);

  const handleForcarCalculo = async () => {
    setCalculando(true);
    setMensagem('');
    try {
      const res = await fetch(`${API}/api/od/forcar-calculo`, { method: 'POST' });
      const data = await res.json();
      setMensagem(data.mensagem || 'Cálculo concluído.');
      await fetchFluxos();
    } catch {
      setMensagem('Erro ao forçar cálculo.');
    } finally {
      setCalculando(false);
    }
  };

  const handleExportar = () => {
    if (fluxos.length === 0) return;
    const headers = Object.keys(fluxos[0]);
    const csv = [
      headers.join(','),
      ...fluxos.map(row => headers.map(h => `"${String(row[h] ?? '').replace(/"/g, '""')}"`).join(','))
    ].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `matriz_od_${new Date().toISOString().split('T')[0]}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  };

  const LABELS = {
    origemStopId:    'Paragem Origem',
    destinoStopId:   'Paragem Destino',
    routeId:         'Linha',
    periodo:         'Período',
    volume:          'Volume',
    indiceConfianca: 'Confiança',
    dataCalculo:     'Data Cálculo',
  };

  const renderTable = () => {
    if (fluxos.length === 0) return null;
    const headers = Object.keys(fluxos[0]).filter(h => h !== 'id' && h !== 'calculadoEm');
    return (
      <div className="od-table-container">
        <table className="od-table">
          <thead>
            <tr>{headers.map(h => <th key={h}>{LABELS[h] || h}</th>)}</tr>
          </thead>
          <tbody>
            {fluxos.map((row, idx) => (
              <tr key={idx}>
                {headers.map(h => <td key={h}>{String(row[h] ?? '—')}</td>)}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  };

  return (
    <div className="od-card">
      <div className="od-card-header">
        <h2>Matriz Origem-Destino</h2>
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <button className="btn-resolver" onClick={handleForcarCalculo} disabled={calculando}>
            {calculando ? 'A calcular...' : 'Forçar Cálculo'}
          </button>
          <button className="btn-download" onClick={handleExportar} disabled={fluxos.length === 0}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="7 10 12 15 17 10"></polyline>
              <line x1="12" y1="15" x2="12" y2="3"></line>
            </svg>
            Exportar CSV
          </button>
        </div>
      </div>
      <div className="od-card-body">
        {mensagem && (
          <div style={{ marginBottom: '1rem', padding: '0.75rem', background: '#f0fdf4', borderRadius: 8, color: '#166534', fontSize: '0.875rem' }}>
            {mensagem}
          </div>
        )}
        {loading ? (
          <div className="od-empty-state">A carregar fluxos...</div>
        ) : fluxos.length === 0 ? (
          <div className="od-empty-state">
            Sem dados de fluxos. A Matriz O-D é calculada automaticamente às 02h00 com base em cartões repetidos.
            Podes forçar o cálculo agora com o botão acima.
          </div>
        ) : renderTable()}
      </div>
    </div>
  );
}

export default MatrizOD;