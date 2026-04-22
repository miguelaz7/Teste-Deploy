import React, { useState, useEffect } from 'react';
import { getUncategorizedEvents, reprocessEvents, deleteUncategorizedEvents } from '../services/categorizationService';
import './EcraRevisaoEventos.css';

const EcraRevisaoEventos = ({ onMappingChange }) => {
  const [eventos, setEventos] = useState([]);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let ativo = true;

    const fetchEventos = async (silencioso = false) => {
      if (!silencioso) setLoading(true);
      try {
        const data = await getUncategorizedEvents(startDate, endDate);
        if (ativo) setEventos(data);
      } catch (err) {
        if (!silencioso) console.error('Error fetching uncategorized events', err);
        if (ativo && !silencioso) setEventos([]);
      } finally {
        if (ativo && !silencioso) setLoading(false);
      }
    };

    fetchEventos(false);

    const intervalId = setInterval(() => {
      if (ativo) {
        fetchEventos(true);
      }
    }, 3000);

    return () => {
      ativo = false;
      clearInterval(intervalId);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [startDate, endDate]);

  const handleReprocessar = async (id) => {
    try {
      await reprocessEvents(id);
      if (onMappingChange) onMappingChange();
    } catch (err) {
      console.error('Error reprocessing event', err);
    }
  };

  const handleApagarTudo = async () => {
    if (window.confirm('Tem a certeza que deseja apagar TODOS os eventos não categorizados da base de dados?')) {
      try {
        await deleteUncategorizedEvents();
        if (onMappingChange) onMappingChange();
      } catch (err) {
        console.error('Error deleting events', err);
      }
    }
  };

  return (
    <div className="revisao-eventos-container">
      
      <div className="gestao-header">
        <h2>Revisão de Eventos Não Categorizados</h2>
        <div className="header-actions">
          <div className="date-filter-group">
            <label>De:</label>
            <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
            <label>Até:</label>
            <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
          </div>
          <button className="btn-secondary danger" onClick={handleApagarTudo}>
            Apagar Tudo
          </button>
        </div>
      </div>
      
      <div className="table-container">
        <table className="eventos-table">
          <thead>
            <tr>
              <th>Identificador</th>
              <th>Status</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan="3" style={{ textAlign: 'center' }}>
                  <span className="status-text">A carregar dados...</span>
                </td>
              </tr>
            ) : eventos.length > 0 ? (
              eventos.map((evento) => (
                <tr key={evento.id}>
                  <td>{evento.cardId || '---'}</td>
                  <td>
                    {evento.pii_detected ? (
                      <span className="badge badge-suspenso">Suspenso (PII)</span>
                    ) : (
                      <span className="status-text">Pendente</span>
                    )}
                  </td>
                  <td>
                    {!evento.pii_detected && (
                      <button 
                        className="btn-reprocess" 
                        onClick={() => handleReprocessar(evento.id)}
                      >
                        Reprocessar
                      </button>
                    )}
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="3" style={{ textAlign: 'center' }}>
                  <span className="status-text">Nenhum evento pendente de categorização para este período.</span>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default EcraRevisaoEventos;
