import React, { useState, useEffect, useCallback } from 'react';
import { getFluxos, exportarMatrizOD } from '../../logica_do_sistema/services/odService';
import './Od.css';

const API = 'http://localhost:8080';

function MatrizOD() {
  const [fluxos, setFluxos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [calculando, setCalculando] = useState(false);
  const [exportando, setExportando] = useState(false);
  const [formato, setFormato] = useState('CSV');
  const [mensagem, setMensagem] = useState('');
  const [msgErro, setMsgErro] = useState('');

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
    setMsgErro('');
    try {
      const res = await fetch(`${API}/api/od/forcar-calculo`, { method: 'POST' });
      const data = await res.json();
      setMensagem(data.mensagem || 'Cálculo concluído.');
      await fetchFluxos();
    } catch {
      setMsgErro('Erro ao forçar cálculo.');
    } finally {
      setCalculando(false);
    }
  };

  const handleExportar = async () => {
    setExportando(true);
    setMensagem('');
    setMsgErro('');
    try {
      // Pedir exportação agregada ao backend (com privacidade + DPO validation + audit)
      const data = await exportarMatrizOD(null, null, formato);
      
      let blob;
      let filename = `matriz_od_export_${new Date().toISOString().split('T')[0]}`;
      
      if (formato === 'JSON') {
        const jsonStr = JSON.stringify(data, null, 2);
        blob = new Blob([jsonStr], { type: 'application/json;charset=utf-8;' });
        filename += '.json';
      } else {
        const rows = data.registos || [];
        if (rows.length === 0) {
          setMsgErro('Nenhum dado com volume suficiente (limiar mínimo de 5 ocorrências) para exportar.');
          return;
        }
        const headers = Object.keys(rows[0]);
        const csv = [
          headers.join(','),
          ...rows.map(row => headers.map(h => `"${String(row[h] ?? '').replace(/"/g, '""')}"`).join(','))
        ].join('\n');
        blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        filename += '.csv';
      }
      
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      setMensagem(`Matriz O-D exportada com sucesso em formato ${formato} (Registado em Auditoria).`);
    } catch (err) {
      setMsgErro(err.message || 'Erro ao exportar matriz. Certifique-se de que tem as permissões acordadas com o DPO.');
    } finally {
      setExportando(false);
    }
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

  const renderCellContent = (h, val, row) => {
    if (h === 'routeId') {
      return (
        <span style={{ 
          display: 'inline-flex', 
          alignItems: 'center', 
          justifyContent: 'center', 
          backgroundColor: '#eff6ff', 
          color: '#1e40af', 
          border: '1px solid #bfdbfe', 
          borderRadius: '6px', 
          padding: '2px 8px', 
          fontSize: '0.8rem', 
          fontWeight: 700 
        }}>
          L. {val}
        </span>
      );
    }
    if (h === 'indiceConfianca') {
      const numericVal = parseFloat(val);
      const pct = isNaN(numericVal) ? 0 : Math.round(numericVal * 100);
      let bg = '#fee2e2', color = '#991b1b', border = '#fecaca';
      if (pct >= 85) { bg = '#dcfce7'; color = '#15803d'; border = '#bbf7d0'; }
      else if (pct >= 60) { bg = '#fef9c3'; color = '#a16207'; border = '#fef08a'; }
      return (
        <span className="badge-estado" style={{ backgroundColor: bg, color: color, border: `1px solid ${border}`, padding: '4px 8px', fontSize: '0.8rem', fontWeight: '700' }}>
          {pct}% Confiança
        </span>
      );
    }
    if (h === 'volume') {
      return <strong style={{ color: '#0f172a', fontSize: '0.9rem' }}>{val} pax</strong>;
    }
    if (h === 'periodo') {
      const p = String(val).toUpperCase();
      let bg = '#f1f5f9', color = '#475569';
      if (p.includes('MANHÃ')) { bg = '#ecfeff'; color = '#0891b2'; }
      else if (p.includes('TARDE')) { bg = '#fff7ed'; color = '#ea580c'; }
      else if (p.includes('NOITE')) { bg = '#faf5ff'; color = '#7c3aed'; }
      return (
        <span style={{ backgroundColor: bg, color: color, padding: '2px 8px', borderRadius: '6px', fontSize: '0.8rem', fontWeight: 700, textTransform: 'uppercase' }}>
          {p}
        </span>
      );
    }
    if (h === 'origemStopId' || h === 'destinoStopId') {
      return (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', color: '#334155', fontWeight: 600 }}>
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" style={{ color: '#64748b' }}>
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
            <circle cx="12" cy="10" r="3" />
          </svg>
          Paragem {val}
        </span>
      );
    }
    if (h === 'dataCalculo') {
      return <span style={{ color: '#64748b', fontSize: '0.8rem', fontWeight: 500 }}>{val}</span>;
    }
    return String(val ?? '—');
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
                {headers.map(h => <td key={h}>{renderCellContent(h, row[h], row)}</td>)}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  };

  return (
    <div className="analise-container">
      <div className="analise-card">
        <div className="od-card-header">
          <h2>Matriz Origem-Destino</h2>
          <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
            <select 
              value={formato} 
              onChange={(e) => setFormato(e.target.value)} 
              style={{ 
                padding: '0.45rem 1rem', 
                borderRadius: '8px', 
                border: '1px solid #cbd5e1', 
                backgroundColor: '#fff', 
                color: '#0f172a', 
                fontSize: '0.875rem',
                fontWeight: '600',
                outline: 'none',
                cursor: 'pointer',
                height: '38px'
              }}
            >
              <option value="CSV">CSV</option>
              <option value="JSON">JSON (NGSI-LD)</option>
              <option value="Excel">Excel (CSV)</option>
            </select>

            <button className="btn-resolver" onClick={handleForcarCalculo} disabled={calculando}>
              <svg style={{ marginRight: '6px' }} className={calculando ? "spinning-icon" : ""} width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M23 4v6h-6"></path>
                <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
              </svg>
              {calculando ? 'A calcular...' : 'Forçar Cálculo'}
            </button>
            
            <button className="btn-download" onClick={handleExportar} disabled={exportando || fluxos.length === 0}>
              <svg style={{ marginRight: '6px' }} className={exportando ? "spinning-icon" : ""} width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                <polyline points="7 10 12 15 17 10"></polyline>
                <line x1="12" y1="15" x2="12" y2="3"></line>
              </svg>
              {exportando ? 'A exportar...' : `Exportar ${formato}`}
            </button>
          </div>
        </div>
        <div className="od-card-body">
          {mensagem && (
            <div style={{ marginBottom: '1.25rem', padding: '1rem', background: '#f0fdf4', borderRadius: 8, color: '#166534', fontSize: '0.875rem', border: '1px solid #bbf7d0', fontWeight: '500' }}>
              {mensagem}
            </div>
          )}
          {msgErro && (
            <div style={{ marginBottom: '1.25rem', padding: '1rem', background: '#fef2f2', borderRadius: 8, color: '#991b1b', fontSize: '0.875rem', border: '1px solid #fecaca', fontWeight: '500' }}>
              {msgErro}
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
              <h4 className="empty-state-title">A carregar fluxos...</h4>
              <p className="empty-state-description">Por favor aguarde enquanto sincronizamos os dados de tráfego.</p>
            </div>
          ) : fluxos.length === 0 ? (
            <div className="od-empty-state-container">
              <div className="empty-state-icon-wrapper" style={{ backgroundColor: '#eff6ff' }}>
                <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#3b82f6" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6"></polygon>
                  <circle cx="12" cy="12" r="3" fill="#3b82f6" fillOpacity="0.2"></circle>
                </svg>
              </div>
              <h4 className="empty-state-title">Sem dados de fluxos</h4>
              <p className="empty-state-description">
                A Matriz O-D é calculada automaticamente às 02h00 com base em cartões repetidos. Podes forçar o cálculo agora com o botão acima.
              </p>
            </div>
          ) : renderTable()}
        </div>
      </div>
    </div>
  );
}

export default MatrizOD;