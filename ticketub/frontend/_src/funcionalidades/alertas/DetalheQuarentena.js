import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { getQuarentena } from '../../logica_do_sistema/services/alertasService';
import './Alertas.css';

function DetalheQuarentena() {
  const [registos, setRegistos] = useState(null);
  const [loading, setLoading] = useState(true);
  const [filtroMotivo, setFiltroMotivo] = useState('Todos');

  const fetchQuarentena = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getQuarentena();
      setRegistos(Array.isArray(data) ? data : []);
    } catch (err) {
      setRegistos(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchQuarentena();
  }, [fetchQuarentena]);

  // Extrair motivos únicos para o dropdown
  const motivosDisponiveis = useMemo(() => {
    if (!registos) return ['Todos'];
    const motivos = registos
      .map(r => r.motivo || r.razao || 'Desconhecido')
      .filter((value, index, self) => self.indexOf(value) === index);
    return ['Todos', ...motivos];
  }, [registos]);

  // Filtrar registos baseados no motivo
  const registosFiltrados = useMemo(() => {
    if (!registos) return [];
    if (filtroMotivo === 'Todos') return registos;
    return registos.filter(r => (r.motivo || r.razao || 'Desconhecido') === filtroMotivo);
  }, [registos, filtroMotivo]);

  const handleExportar = () => {
    if (registosFiltrados.length === 0) return;

    const headers = Object.keys(registosFiltrados[0]);
    const csvContent = [
      headers.join(','),
      ...registosFiltrados.map(row => headers.map(h => `"${String(row[h] || '').replace(/"/g, '""')}"`).join(','))
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `quarentena_export_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const renderTable = (dataArray) => {
    if (dataArray.length === 0) {
      return (
        <div className="alertas-empty-state-container">
          <div className="empty-state-icon-wrapper">
            <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#10b981" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
              <path d="m9 11 2 2 4-4" strokeWidth="2"></path>
            </svg>
          </div>
          <h4 className="empty-state-title">Quarentena Limpa</h4>
          <p className="empty-state-description">Sem registos em quarentena para o filtro selecionado.</p>
        </div>
      );
    }

    const headers = Object.keys(dataArray[0]);

    return (
      <div className="quarentena-table-container">
        <table className="quarentena-table">
          <thead>
            <tr>
              {headers.map(h => <th key={h}>{h.charAt(0).toUpperCase() + h.slice(1)}</th>)}
            </tr>
          </thead>
          <tbody>
            {dataArray.map((row, idx) => (
              <tr key={idx}>
                {headers.map(h => <td key={`${idx}-${h}`}>{String(row[h] || '')}</td>)}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  };

  return (
    <div className="alertas-card">
      <div className="alertas-card-header">
        <h2>Detalhe de Quarentena</h2>
      </div>
      <div className="alertas-card-body">
        
        <div className="quarentena-controls">
          <div className="quarentena-filter">
            <label htmlFor="motivoFilter" style={{ fontWeight: 600, color: 'var(--text-main, #111827)', fontSize: '0.9rem' }}>
              Filtrar por Motivo:
            </label>
            <select 
              id="motivoFilter" 
              value={filtroMotivo} 
              onChange={(e) => setFiltroMotivo(e.target.value)}
              disabled={!registos || registos.length === 0}
            >
              {motivosDisponiveis.map(m => (
                <option key={m} value={m}>{m}</option>
              ))}
            </select>
          </div>
          
          <button 
            className="btn-exportar" 
            onClick={handleExportar}
            disabled={!registos || registosFiltrados.length === 0}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="7 10 12 15 17 10"></polyline>
              <line x1="12" y1="15" x2="12" y2="3"></line>
            </svg>
            Exportar CSV
          </button>
        </div>

        {loading ? (
          <div className="alertas-empty-state-container">
            <div className="empty-state-icon-wrapper loading">
              <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#3b82f6" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="spinning-icon">
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
            <h4 className="empty-state-title">A carregar registos...</h4>
            <p className="empty-state-description">Por favor aguarde enquanto sincronizamos os dados de quarentena.</p>
          </div>
        ) : !registos ? (
          <div className="alertas-empty-state-container">
            <div className="empty-state-icon-wrapper" style={{ backgroundColor: '#fee2e2' }}>
              <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ef4444" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
            </div>
            <h4 className="empty-state-title">Sem dados disponíveis</h4>
            <p className="empty-state-description">Não foi possível carregar os registos de quarentena neste momento.</p>
          </div>
        ) : (
          renderTable(registosFiltrados)
        )}
      </div>
    </div>
  );
}

export default DetalheQuarentena;
