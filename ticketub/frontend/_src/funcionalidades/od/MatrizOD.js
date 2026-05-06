import React, { useState, useEffect, useCallback } from 'react';
import { getFluxos, downloadExportacao } from '../../logica_do_sistema/services/odService';
import './Od.css';

function MatrizOD() {
  const [fluxos, setFluxos] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchFluxos = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getFluxos();
      setFluxos(Array.isArray(data) ? data : []);
    } catch (err) {
      setFluxos(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchFluxos();
  }, [fetchFluxos]);

  const handleExportar = () => {
    downloadExportacao('matriz_od_export.csv');
  };

  const renderTable = () => {
    if (fluxos.length === 0) {
      return <div className="od-empty-state">Sem dados de fluxos disponíveis.</div>;
    }

    // Adapt headers based on actual data
    const headers = Object.keys(fluxos[0]);

    return (
      <div className="od-table-container">
        <table className="od-table">
          <thead>
            <tr>
              {headers.map(h => <th key={h}>{h.charAt(0).toUpperCase() + h.slice(1).replace(/([A-Z])/g, ' $1')}</th>)}
            </tr>
          </thead>
          <tbody>
            {fluxos.map((row, idx) => (
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
    <div className="od-card">
      <div className="od-card-header">
        <h2>Matriz Origem-Destino</h2>
        <button 
          className="btn-download" 
          onClick={handleExportar}
          disabled={!fluxos || fluxos.length === 0}
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
          Exportar
        </button>
      </div>
      <div className="od-card-body">
        {loading ? (
          <div className="od-empty-state">A carregar fluxos...</div>
        ) : !fluxos ? (
          <div className="od-empty-state">Sem dados disponíveis</div>
        ) : (
          renderTable()
        )}
      </div>
    </div>
  );
}

export default MatrizOD;
