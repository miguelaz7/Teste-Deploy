import React, { useState, useEffect } from 'react';
import { getUncategorizedEvents, reprocessEvents } from '../services/categorizationService';
import CategorizationStats from './CategorizationStats';
import './EcraRevisaoEventos.css';

const EcraRevisaoEventos = () => {
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
      // Automatic UI state polling will pick this up
    } catch (err) {
      console.error('Error reprocessing event', err);
    }
  };

  return (
    <div className="revisao-eventos-container">
      <CategorizationStats />
      
      <div className="revisao-header">
        <h2 className="revisao-title">Revisão de Eventos Não Categorizados</h2>
        <div className="date-filter">
          <label>De: </label>
          <input 
            type="date" 
            value={startDate} 
            onChange={(e) => setStartDate(e.target.value)} 
          />
          <label>Até: </label>
          <input 
            type="date" 
            value={endDate} 
            onChange={(e) => setEndDate(e.target.value)} 
          />
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
                <td colSpan="3" style={{ textAlign: 'center', color: '#64748b' }}>A carregar...</td>
              </tr>
            ) : eventos.length > 0 ? (
              eventos.map((evento) => (
                <tr key={evento.id}>
                  <td>{evento.identifier || '---'}</td>
                  <td>
                    {evento.pii_detected ? (
                      <span className="badge badge-suspenso">Suspenso - PII</span>
                    ) : (
                      <span style={{ color: '#64748b', fontSize: '0.8rem', fontWeight: 600 }}>Pendente</span>
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
                <td colSpan="3" style={{ textAlign: 'center', color: '#64748b' }}>Nenhum evento encontrado.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default EcraRevisaoEventos;
