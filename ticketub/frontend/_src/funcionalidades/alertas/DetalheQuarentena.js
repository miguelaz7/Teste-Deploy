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
      return <div className="alertas-empty-state">Sem registos em quarentena para o filtro selecionado.</div>;
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
            <label htmlFor="motivoFilter" style={{ fontWeight: 500, color: 'var(--text-main, #111827)' }}>
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
          <div className="alertas-empty-state">A carregar registos...</div>
        ) : !registos ? (
          <div className="alertas-empty-state">Sem dados disponíveis</div>
        ) : (
          renderTable(registosFiltrados)
        )}
      </div>
    </div>
  );
}

export default DetalheQuarentena;
